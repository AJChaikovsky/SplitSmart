package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.splitsmart.app.R
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.business.BalanceManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ActivityBalanceBinding

class BalanceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBalanceBinding
    private val balanceManager = BalanceManager.getInstance()
    private val accountManager = AccountManager.getInstance()
    private val userRepository = UserRepository.getInstance()
    private lateinit var groupId: String
    private val nameCache = mutableMapOf<String, String>()

    private val adapter = object : RecyclerView.Adapter<VH>() {
        private val items = mutableListOf<BalanceManager.PairBalance>()

        fun submit(list: List<BalanceManager.PairBalance>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_balance, parent, false)
            return VH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val b = items[position]
            val from = nameCache[b.fromUserId] ?: b.fromUserId
            val to = nameCache[b.toUserId] ?: b.toUserId
            holder.text.text = "$from owes $to ${formatMoney(b.amount)}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBalanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.balances)

        groupId = intent.getStringExtra(HomeActivity.EXTRA_GROUP_ID).orEmpty()
        binding.rvBalances.layoutManager = LinearLayoutManager(this)
        binding.rvBalances.adapter = adapter
        binding.btnCreateSettlement.setOnClickListener {
            startActivity(
                Intent(this, CreateSettlementActivity::class.java)
                    .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val uid = accountManager.currentUid() ?: return
        launchIo {
            val balances = balanceManager.getBalancesForGroup(groupId)
            balances.forEach { b ->
                if (!nameCache.containsKey(b.fromUserId)) {
                    nameCache[b.fromUserId] = userRepository.getById(b.fromUserId)?.name ?: b.fromUserId
                }
                if (!nameCache.containsKey(b.toUserId)) {
                    nameCache[b.toUserId] = userRepository.getById(b.toUserId)?.name ?: b.toUserId
                }
            }
            val summary = balanceManager.summarizeForUser(uid, balances)
            runOnUiThread {
                binding.tvToPay.text = "${getString(R.string.total_to_pay)}: ${formatMoney(summary.totalToPay)}"
                binding.tvToReceive.text =
                    "${getString(R.string.total_to_receive)}: ${formatMoney(summary.totalToReceive)}"
                adapter.submit(balances)
                binding.tvEmpty.visibility = if (balances.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private class VH(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tvBalance)
    }
}
