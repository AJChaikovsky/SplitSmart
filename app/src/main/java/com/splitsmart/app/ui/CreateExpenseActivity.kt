package com.splitsmart.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.splitsmart.app.R
import com.splitsmart.app.business.ExpenseManager
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ActivityCreateExpenseBinding
import com.splitsmart.app.model.ExpenseCategory
import com.splitsmart.app.model.SplitMethod
import com.splitsmart.app.model.User

class CreateExpenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateExpenseBinding
    private val expenseManager = ExpenseManager.getInstance()
    private val groupManager = GroupManager.getInstance()
    private val userRepository = UserRepository.getInstance()

    private lateinit var groupId: String
    private var members: List<User> = emptyList()
    private val participantChecks = mutableMapOf<String, CheckBox>()
    private val valueInputs = mutableMapOf<String, EditText>()
    private var editingExpenseId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        groupId = intent.getStringExtra(HomeActivity.EXTRA_GROUP_ID).orEmpty()
        editingExpenseId = intent.getStringExtra(EXTRA_EXPENSE_ID)
        supportActionBar?.title = if (editingExpenseId == null) getString(R.string.add_expense) else "Edit Expense"

        binding.etDate.setText(todayIso())
        binding.spCategory.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, ExpenseCategory.entries.map { it.name }
        )
        binding.spSplitMethod.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, SplitMethod.entries.map { it.name }
        )

        binding.spSplitMethod.setOnItemSelectedListenerSimple { rebuildSplitValueInputs() }

        binding.btnSave.setOnClickListener { save() }
        loadMembers()
    }

    private fun loadMembers() {
        binding.progress.visibility = View.VISIBLE
        launchIo {
            val group = groupManager.getGroup(groupId)
            members = group.memberIds.mapNotNull { userRepository.getById(it) }
            val existing = editingExpenseId?.let { expenseManager.getExpense(it) }
            runOnUiThread {
                binding.progress.visibility = View.GONE
                binding.spPayer.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    members.map { "${it.name} (${it.email})" }
                )
                binding.llParticipants.removeAllViews()
                participantChecks.clear()
                members.forEach { user ->
                    val cb = CheckBox(this).apply {
                        text = "${user.name} (${user.email})"
                        isChecked = existing?.participantIds?.contains(user.uid) != false
                        setOnCheckedChangeListener { _, _ -> rebuildSplitValueInputs() }
                    }
                    participantChecks[user.uid] = cb
                    binding.llParticipants.addView(cb)
                }
                existing?.let { exp ->
                    binding.etTitle.setText(exp.title)
                    binding.etAmount.setText(exp.amount.toString())
                    binding.etDate.setText(exp.date)
                    binding.etNote.setText(exp.note)
                    binding.spCategory.setSelection(ExpenseCategory.entries.indexOf(exp.categoryEnum()).coerceAtLeast(0))
                    binding.spSplitMethod.setSelection(SplitMethod.entries.indexOf(exp.splitMethodEnum()).coerceAtLeast(0))
                    val payerIndex = members.indexOfFirst { it.uid == exp.payerId }
                    if (payerIndex >= 0) binding.spPayer.setSelection(payerIndex)
                }
                rebuildSplitValueInputs()
            }
        }
    }

    private fun selectedParticipants(): List<String> =
        participantChecks.filter { it.value.isChecked }.keys.toList()

    private fun rebuildSplitValueInputs() {
        binding.llSplitValues.removeAllViews()
        valueInputs.clear()
        val method = SplitMethod.entries[binding.spSplitMethod.selectedItemPosition]
        if (method == SplitMethod.EQUAL) return

        val label = TextView(this).apply {
            text = if (method == SplitMethod.EXACT) "Exact amounts" else "Percentages"
            setPadding(0, 16, 0, 8)
        }
        binding.llSplitValues.addView(label)

        selectedParticipants().forEach { uid ->
            val user = members.find { it.uid == uid } ?: return@forEach
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val tv = TextView(this).apply {
                text = user.name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val et = EditText(this).apply {
                hint = if (method == SplitMethod.EXACT) "0.00" else "%"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            valueInputs[uid] = et
            row.addView(tv)
            row.addView(et)
            binding.llSplitValues.addView(row)
        }
    }

    private fun save() {
        val title = binding.etTitle.text?.toString().orEmpty()
        val amount = binding.etAmount.text?.toString()?.toDoubleOrNull()
        if (amount == null) {
            toast("Enter a valid amount")
            return
        }
        val date = binding.etDate.text?.toString().orEmpty()
        val note = binding.etNote.text?.toString().orEmpty()
        val category = ExpenseCategory.entries[binding.spCategory.selectedItemPosition]
        val method = SplitMethod.entries[binding.spSplitMethod.selectedItemPosition]
        val participants = selectedParticipants()
        if (members.isEmpty() || binding.spPayer.selectedItemPosition < 0) {
            toast("No members loaded")
            return
        }
        val payerId = members[binding.spPayer.selectedItemPosition].uid

        val exact = mutableMapOf<String, Double>()
        val pct = mutableMapOf<String, Double>()
        if (method == SplitMethod.EXACT) {
            valueInputs.forEach { (uid, et) ->
                exact[uid] = et.text.toString().toDoubleOrNull() ?: 0.0
            }
        } else if (method == SplitMethod.PERCENTAGE) {
            valueInputs.forEach { (uid, et) ->
                pct[uid] = et.text.toString().toDoubleOrNull() ?: 0.0
            }
        }

        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            val id = editingExpenseId
            if (id == null) {
                expenseManager.createExpense(
                    groupId = groupId,
                    title = title,
                    amount = amount,
                    date = date,
                    category = category,
                    payerId = payerId,
                    participantIds = participants,
                    splitMethod = method,
                    note = note,
                    exactAmounts = exact,
                    percentages = pct
                )
            } else {
                expenseManager.editExpense(
                    expenseId = id,
                    title = title,
                    amount = amount,
                    date = date,
                    category = category,
                    payerId = payerId,
                    participantIds = participants,
                    splitMethod = method,
                    note = note,
                    exactAmounts = exact,
                    percentages = pct
                )
            }
            runOnUiThread {
                binding.progress.visibility = View.GONE
                toast("Saved")
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_EXPENSE_ID = "expenseId"
    }
}

private fun android.widget.Spinner.setOnItemSelectedListenerSimple(onSelected: () -> Unit) {
    onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: android.widget.AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            onSelected()
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
    }
}
