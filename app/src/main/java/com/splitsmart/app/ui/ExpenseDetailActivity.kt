package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.business.ExpenseManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ActivityExpenseDetailBinding
import com.splitsmart.app.model.Expense

class ExpenseDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpenseDetailBinding
    private val expenseManager = ExpenseManager.getInstance()
    private val userRepository = UserRepository.getInstance()
    private val accountManager = AccountManager.getInstance()
    private lateinit var expenseId: String
    private var expense: Expense? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Expense"

        expenseId = intent.getStringExtra(CreateExpenseActivity.EXTRA_EXPENSE_ID).orEmpty()
        if (expenseId.isBlank()) {
            toast("Missing expense")
            finish()
            return
        }

        binding.btnEdit.setOnClickListener {
            val e = expense ?: return@setOnClickListener
            startActivity(
                Intent(this, CreateExpenseActivity::class.java)
                    .putExtra(HomeActivity.EXTRA_GROUP_ID, e.groupId)
                    .putExtra(CreateExpenseActivity.EXTRA_EXPENSE_ID, e.id)
            )
        }
        binding.btnDelete.setOnClickListener {
            confirm("Delete expense", "Are you sure you want to delete this expense?") {
                binding.progress.visibility = View.VISIBLE
                launchIo(onError = {
                    binding.progress.visibility = View.GONE
                    toast(it)
                }) {
                    expenseManager.deleteExpense(expenseId)
                    runOnUiThread {
                        binding.progress.visibility = View.GONE
                        toast("Deleted")
                        finish()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        binding.progress.visibility = View.VISIBLE
        launchIo {
            val e = expenseManager.getExpense(expenseId)
            expense = e
            val payer = userRepository.getById(e.payerId)
            val shareLines = e.shares.map { share ->
                val u = userRepository.getById(share.userId)
                "${u?.name ?: share.userId}: ${formatMoney(share.amount)}"
            }
            val isCreator = e.creatorId == accountManager.currentUid()
            runOnUiThread {
                binding.progress.visibility = View.GONE
                binding.tvTitle.text = e.title
                binding.tvAmount.text = formatMoney(e.amount)
                binding.tvDetails.text = buildString {
                    appendLine("Date: ${e.date}")
                    appendLine("Category: ${e.category}")
                    appendLine("Split: ${e.splitMethod}")
                    appendLine("Payer: ${payer?.name ?: e.payerId}")
                    if (e.note.isNotBlank()) appendLine("Note: ${e.note}")
                }
                binding.tvShares.text = shareLines.joinToString("\n")
                binding.btnEdit.visibility = if (isCreator) View.VISIBLE else View.GONE
                binding.btnDelete.visibility = if (isCreator) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
