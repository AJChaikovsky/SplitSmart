package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.business.ExpenseManager
import com.splitsmart.app.business.SettlementManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ActivityTransactionDetailBinding
import com.splitsmart.app.model.TransactionType

class TransactionDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionDetailBinding
    private val expenseManager = ExpenseManager.getInstance()
    private val settlementManager = SettlementManager.getInstance()
    private val userRepository = UserRepository.getInstance()
    private val accountManager = AccountManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Transaction"

        val id = intent.getStringExtra(HistoryActivity.EXTRA_TX_ID).orEmpty()
        val type = intent.getStringExtra(HistoryActivity.EXTRA_TX_TYPE).orEmpty()
        if (id.isBlank() || type.isBlank()) {
            toast("Missing transaction")
            finish()
            return
        }

        when (type) {
            TransactionType.EXPENSE.name -> loadExpense(id)
            TransactionType.SETTLEMENT.name -> loadSettlement(id)
            else -> {
                toast("Unknown transaction type")
                finish()
            }
        }
    }

    private fun loadExpense(id: String) {
        launchIo(onError = {
            toast(it)
            finish()
        }) {
            val e = expenseManager.getExpense(id)
            val payer = userRepository.getById(e.payerId)?.name ?: e.payerId
            runOnUiThread {
                binding.tvTitle.text = e.title
                binding.tvDetails.text = buildString {
                    appendLine("Type: Expense")
                    appendLine("Amount: ${formatMoney(e.amount)}")
                    appendLine("Date: ${e.date}")
                    appendLine("Category: ${e.category}")
                    appendLine("Payer: $payer")
                    appendLine("Split: ${e.splitMethod}")
                    if (e.note.isNotBlank()) appendLine("Note: ${e.note}")
                }
                binding.btnOpenExpense.visibility = View.VISIBLE
                binding.btnOpenExpense.setOnClickListener {
                    startActivity(
                        Intent(this@TransactionDetailActivity, ExpenseDetailActivity::class.java)
                            .putExtra(CreateExpenseActivity.EXTRA_EXPENSE_ID, e.id)
                    )
                }
            }
        }
    }

    private fun loadSettlement(id: String) {
        launchIo(onError = {
            toast(it)
            finish()
        }) {
            val s = settlementManager.getSettlement(id)
            val from = userRepository.getById(s.fromUserId)?.name ?: s.fromUserId
            val to = userRepository.getById(s.toUserId)?.name ?: s.toUserId
            val isCreator = s.creatorId == accountManager.currentUid()
            runOnUiThread {
                binding.tvTitle.text = "Settlement"
                binding.tvDetails.text = buildString {
                    appendLine("Type: Settlement")
                    appendLine("From: $from")
                    appendLine("To: $to")
                    appendLine("Amount: ${formatMoney(s.amount)}")
                    appendLine("Date: ${s.date}")
                    if (s.note.isNotBlank()) appendLine("Note: ${s.note}")
                }
                if (isCreator) {
                    binding.btnDeleteSettlement.visibility = View.VISIBLE
                    binding.btnDeleteSettlement.setOnClickListener {
                        confirm("Delete settlement", "Delete this settlement?") {
                            launchIo(onError = { toast(it) }) {
                                settlementManager.deleteSettlement(s.id)
                                runOnUiThread {
                                    toast("Deleted")
                                    finish()
                                }
                            }
                        }
                    }
                } else {
                    binding.btnDeleteSettlement.visibility = View.GONE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
