package com.splitsmart.app.ui

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
        binding.etDate.setText(todayIso())
        binding.btnSave.setOnClickListener { save() }
        loadCreditors()
    }

    private fun loadCreditors() {
        val uid = accountManager.currentUid() ?: return
        binding.progress.visibility = View.VISIBLE
        launchIo {
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
                    this, android.R.layout.simple_spinner_dropdown_item, labels
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
