package com.splitsmart.app.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.splitsmart.app.R
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.business.BalanceManager
import com.splitsmart.app.business.SettlementManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ActivityCreateSettlementBinding
import java.util.Calendar

class CreateSettlementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateSettlementBinding
    private val settlementManager = SettlementManager.getInstance()
    private val balanceManager = BalanceManager.getInstance()
    private val accountManager = AccountManager.getInstance()
    private val userRepository = UserRepository.getInstance()

    private lateinit var groupId: String
    private var creditors: List<Pair<String, Double>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateSettlementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.create_settlement)

        groupId = intent.getStringExtra(HomeActivity.EXTRA_GROUP_ID).orEmpty()
        if (groupId.isBlank()) {
            toast("Missing group")
            finish()
            return
        }
        binding.etDate.setText(todayIso())
        binding.etDate.isFocusable = false
        binding.etDate.isClickable = true
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.btnSave.setOnClickListener { save() }
        loadCreditors()
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
                binding.etDate.setText("%04d-%02d-%02d".format(year, month + 1, day))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadCreditors() {
        val uid = accountManager.currentUid()
        if (uid == null) {
            toast("Not logged in")
            finish()
            return
        }
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            val balances = balanceManager.getBalancesForGroup(groupId)
            creditors = balances.filter { it.fromUserId == uid }.map { it.toUserId to it.amount }
            val labels = creditors.map { (toId, amount) ->
                val name = userRepository.getById(toId)?.name ?: toId
                "$name — owed ${formatMoney(amount)}"
            }
            runOnUiThread {
                binding.progress.visibility = View.GONE
                if (labels.isEmpty()) {
                    toast("You don't owe anyone in this group")
                    finish()
                    return@runOnUiThread
                }
                binding.spPayTo.adapter = ArrayAdapter(
                    this@CreateSettlementActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    labels
                )
                binding.spPayTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val owed = creditors[position].second
                        binding.tvOwed.text = "You owe ${formatMoney(owed)}"
                        binding.etAmount.setText(String.format("%.2f", owed))
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }
            }
        }
    }

    private fun save() {
        if (creditors.isEmpty()) return
        val toUserId = creditors[binding.spPayTo.selectedItemPosition].first
        val amount = binding.etAmount.text?.toString()?.toDoubleOrNull()
        if (amount == null) {
            toast("Enter a valid amount")
            return
        }
        binding.progress.visibility = View.VISIBLE
        launchIo(onError = {
            binding.progress.visibility = View.GONE
            toast(it)
        }) {
            settlementManager.createSettlement(
                groupId = groupId,
                toUserId = toUserId,
                amount = amount,
                date = binding.etDate.text?.toString().orEmpty(),
                note = binding.etNote.text?.toString().orEmpty()
            )
            runOnUiThread {
                binding.progress.visibility = View.GONE
                toast("Settlement recorded")
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
