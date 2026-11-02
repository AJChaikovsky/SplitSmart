package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Transaction"

        val id = intent.getStringExtra(HistoryActivity.EXTRA_TX_ID).orEmpty()
        val type = intent.getStringExtra(HistoryActivity.EXTRA_TX_TYPE).orEmpty()

        if (type == TransactionType.EXPENSE.name) {
            launchIo {
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
        } else {
            launchIo {
                val s = settlementManager.getSettlement(id)
                val from = userRepository.getById(s.fromUserId)?.name ?: s.fromUserId
                val to = userRepository.getById(s.toUserId)?.name ?: s.toUserId
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
                    binding.btnDeleteSettlement.visibility = View.VISIBLE
                    binding.btnDeleteSettlement.setOnClickListener {
                        confirm("Delete settlement", "Delete this settlement?") {
                            launchIo {
                                settlementManager.deleteSettlement(s.id)
                                runOnUiThread {
                                    toast("Deleted")
                                    finish()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
