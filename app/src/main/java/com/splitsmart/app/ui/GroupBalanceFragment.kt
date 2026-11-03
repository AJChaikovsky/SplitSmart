package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.splitsmart.app.R
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.business.BalanceManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ActivityBalanceBinding
import kotlinx.coroutines.Job

class GroupBalanceFragment : GroupTabFragment(R.layout.activity_balance) {
    private var viewBinding: ActivityBalanceBinding? = null
    private val binding get() = requireNotNull(viewBinding)
    private var loading: Job? = null
    private val balanceManager = BalanceManager.getInstance()
    private val accountManager = AccountManager.getInstance()
    private val userRepository = UserRepository.getInstance()
    private val names = mutableMapOf<String, String>()
    private val items = mutableListOf<BalanceManager.PairBalance>()
    private val adapter = object : RecyclerView.Adapter<BalanceHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = BalanceHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_balance, parent, false)
        )
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: BalanceHolder, position: Int) {
            val balance = items[position]
            val from = names[balance.fromUserId] ?: balance.fromUserId
            val to = names[balance.toUserId] ?: balance.toUserId
            holder.description.text = getString(R.string.balance_direction, from, to)
            holder.amount.text = formatMoney(balance.amount)
            holder.avatar.text = initials(from)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewBinding = ActivityBalanceBinding.bind(view)
        binding.rvBalances.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBalances.adapter = adapter
        binding.btnCreateSettlement.setOnClickListener {
            startActivity(Intent(requireContext(), CreateSettlementActivity::class.java)
                .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId))
        }
    }

    override fun refresh() {
        val uid = accountManager.currentUid() ?: return
        loading?.cancel()
        binding.progress.visibility = View.VISIBLE
        loading = loadData(fetch = {
            val balances = balanceManager.getBalancesForGroup(groupId)
            val userIds = balances.flatMap { listOf(it.fromUserId, it.toUserId) }.distinct()
            val labels = userIds.associateWith { userRepository.getById(it)?.name ?: it }
            Triple(balances, balanceManager.summarizeForUser(uid, balances), labels)
        }, render = { (balances, summary, labels) ->
            names.clear()
            names.putAll(labels)
            items.clear()
            items.addAll(balances)
            adapter.notifyDataSetChanged()
            binding.tvToPay.text = formatMoney(summary.totalToPay)
            binding.tvToReceive.text = formatMoney(summary.totalToReceive)
            binding.tvEmpty.visibility = if (balances.isEmpty()) View.VISIBLE else View.GONE
            binding.progress.visibility = View.GONE
        }, onError = {
            binding.progress.visibility = View.GONE
            requireContext().toast(it)
        })
    }

    override fun onDestroyView() {
        binding.rvBalances.adapter = null
        viewBinding = null
        super.onDestroyView()
    }

    private class BalanceHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.tvAvatar)
        val description: TextView = view.findViewById(R.id.tvBalance)
        val amount: TextView = view.findViewById(R.id.tvBalanceAmount)
    }
}
