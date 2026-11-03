package com.splitsmart.app.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.splitsmart.app.R
import com.splitsmart.app.business.ExpenseManager
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ActivityCreateExpenseBinding
import com.splitsmart.app.model.ExpenseCategory
import com.splitsmart.app.model.ExpenseShare
import com.splitsmart.app.model.SplitMethod
import com.splitsmart.app.model.User
import java.util.Calendar

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
        supportActionBar?.title =
            if (editingExpenseId == null) getString(R.string.add_expense) else "Edit Expense"

        binding.etDate.setText(todayIso())
        binding.etDate.isFocusable = false
        binding.etDate.isClickable = true
        binding.etDate.setOnClickListener { showDatePicker() }

        binding.spCategory.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, ExpenseCategory.entries.map { it.name }
        )
        binding.spCategory.setSelection(ExpenseCategory.OTHER.ordinal)

        binding.spSplitMethod.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, SplitMethod.entries.map { it.name }
        )

        binding.spSplitMethod.setOnItemSelectedListenerSimple {
            rebuildSplitValueInputs()
            updateSharePreview()
        }
        binding.spPayer.setOnItemSelectedListenerSimple {
            lockPayerAsParticipant()
            rebuildSplitValueInputs()
            updateSharePreview()
        }

        binding.btnSave.setOnClickListener { save() }
        loadMembers()
    }

    private fun loadMembers() {
        if (groupId.isBlank()) {
            toast("Missing group")
            finish()
            return
        }
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            val group = groupManager.getGroup(groupId)
            members = group.memberIds.mapNotNull { userRepository.getById(it) }
            val existing = editingExpenseId?.let { expenseManager.getExpense(it) }
            runOnUiThread {
                binding.progress.visibility = View.GONE
                binding.spPayer.adapter = ArrayAdapter(
                    this@CreateExpenseActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    members.map { "${it.name} (${it.email})" }
                )
                binding.llParticipants.removeAllViews()
                participantChecks.clear()
                members.forEach { user ->
                    val cb = CheckBox(this@CreateExpenseActivity).apply {
                        text = "${user.name} (${user.email})"
                        isChecked = existing?.participantIds?.contains(user.uid) != false
                        setOnCheckedChangeListener { button, checked ->
                            val payerId = currentPayerId()
                            if (payerId != null && user.uid == payerId && !checked) {
                                // Payer cannot be deselected.
                                button.isChecked = true
                                toast("Payer must remain a participant")
                                return@setOnCheckedChangeListener
                            }
                            rebuildSplitValueInputs()
                            updateSharePreview()
                        }
                    }
                    participantChecks[user.uid] = cb
                    binding.llParticipants.addView(cb)
                }
                existing?.let { exp ->
                    binding.etTitle.setText(exp.title)
                    binding.etAmount.setText(exp.amount.toString())
                    binding.etDate.setText(exp.date)
                    binding.etNote.setText(exp.note)
                    binding.spCategory.setSelection(
                        ExpenseCategory.entries.indexOf(exp.categoryEnum()).coerceAtLeast(0)
                    )
                    binding.spSplitMethod.setSelection(
                        SplitMethod.entries.indexOf(exp.splitMethodEnum()).coerceAtLeast(0)
                    )
                    val payerIndex = members.indexOfFirst { it.uid == exp.payerId }
                    if (payerIndex >= 0) binding.spPayer.setSelection(payerIndex)
                }
                lockPayerAsParticipant()
                rebuildSplitValueInputs()
                updateSharePreview()
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val current = binding.etDate.text?.toString().orEmpty()
        if (current.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            val parts = current.split("-")
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                binding.etDate.setText(
                    "%04d-%02d-%02d".format(year, month + 1, day)
                )
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun currentPayerId(): String? {
        if (members.isEmpty() || binding.spPayer.selectedItemPosition < 0) return null
        return members[binding.spPayer.selectedItemPosition].uid
    }

    private fun lockPayerAsParticipant() {
        val payerId = currentPayerId() ?: return
        participantChecks[payerId]?.let { cb ->
            cb.isChecked = true
            cb.isEnabled = false
        }
        participantChecks.forEach { (uid, cb) ->
            if (uid != payerId) cb.isEnabled = true
        }
    }

    private fun selectedParticipants(): List<String> {
        val payerId = currentPayerId()
        val selected = participantChecks.filter { it.value.isChecked }.keys.toMutableList()
        if (payerId != null && payerId !in selected) selected.add(0, payerId)
        return selected
    }

    private fun rebuildSplitValueInputs() {
        binding.llSplitValues.removeAllViews()
        valueInputs.clear()
        if (binding.spSplitMethod.selectedItemPosition < 0) return
        val method = SplitMethod.entries[binding.spSplitMethod.selectedItemPosition]
        if (method == SplitMethod.EQUAL) {
            updateSharePreview()
            return
        }

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
                setOnFocusChangeListener { _, _ -> updateSharePreview() }
            }
            valueInputs[uid] = et
            row.addView(tv)
            row.addView(et)
            binding.llSplitValues.addView(row)
        }
        updateSharePreview()
    }

    private fun buildExactAndPercentMaps(method: SplitMethod): Pair<Map<String, Double>, Map<String, Double>> {
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
        return exact to pct
    }

    private fun computePreviewShares(): List<ExpenseShare>? {
        val amount = binding.etAmount.text?.toString()?.toDoubleOrNull() ?: return null
        if (binding.spSplitMethod.selectedItemPosition < 0) return null
        val method = SplitMethod.entries[binding.spSplitMethod.selectedItemPosition]
        val participants = selectedParticipants()
        if (participants.size < 2) return null
        val (exact, pct) = buildExactAndPercentMaps(method)
        return try {
            expenseManager.computeShares(amount, participants, method, exact, pct)
        } catch (_: Exception) {
            null
        }
    }

    private fun updateSharePreview() {
        val shares = computePreviewShares()
        val existing = binding.root.findViewWithTag<TextView>("sharePreview")
        if (existing != null) {
            binding.llSplitValues.removeView(existing)
        }
        if (shares == null) return
        val preview = TextView(this).apply {
            tag = "sharePreview"
            setPadding(0, 24, 0, 8)
            text = buildString {
                append("Assigned shares:\n")
                shares.forEach { share ->
                    val name = members.find { it.uid == share.userId }?.name ?: share.userId
                    append("• $name: ${formatMoney(share.amount)}\n")
                }
            }
        }
        binding.llSplitValues.addView(preview)
    }

    private fun save() {
        val title = binding.etTitle.text?.toString().orEmpty()
        val amount = binding.etAmount.text?.toString()?.toDoubleOrNull()
        if (amount == null) {
            toast("Enter a valid amount")
            return
        }
        val date = todayIso()
        binding.etDate.setText(date)
        val note = binding.etNote.text?.toString().orEmpty()
        val category = ExpenseCategory.entries[binding.spCategory.selectedItemPosition]
        val method = SplitMethod.entries[binding.spSplitMethod.selectedItemPosition]
        val participants = selectedParticipants()
        if (members.isEmpty() || binding.spPayer.selectedItemPosition < 0) {
            toast("No members loaded")
            return
        }
        val payerId = members[binding.spPayer.selectedItemPosition].uid
        val (exact, pct) = buildExactAndPercentMaps(method)

        val shares = try {
            expenseManager.validateExpenseInput(amount, payerId, participants, members.map { it.uid })
            expenseManager.computeShares(amount, participants, method, exact, pct)
        } catch (e: Exception) {
            toast(e.message ?: "Invalid split")
            return
        }

        val message = buildString {
            append("Confirm shares (must sum to ${formatMoney(amount)}):\n\n")
            shares.forEach { share ->
                val name = members.find { it.uid == share.userId }?.name ?: share.userId
                append("$name: ${formatMoney(share.amount)}\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Review shares")
            .setMessage(message)
            .setPositiveButton("Save") { _, _ -> persist(title, amount, date, category, payerId, participants, method, note, exact, pct) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun persist(
        title: String,
        amount: Double,
        date: String,
        category: ExpenseCategory,
        payerId: String,
        participants: List<String>,
        method: SplitMethod,
        note: String,
        exact: Map<String, Double>,
        pct: Map<String, Double>
    ) {
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
