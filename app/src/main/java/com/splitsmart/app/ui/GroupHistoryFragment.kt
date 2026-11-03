package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.splitsmart.app.R
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.business.HistoryManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ActivityHistoryBinding
import com.splitsmart.app.model.ExpenseCategory
import com.splitsmart.app.model.Transaction
import com.splitsmart.app.model.TransactionType
import com.splitsmart.app.model.User
import kotlinx.coroutines.Job

class GroupHistoryFragment : GroupTabFragment(R.layout.activity_history) {
    private var viewBinding: ActivityHistoryBinding? = null
    private val binding get() = requireNotNull(viewBinding)
    private var loading: Job? = null
    private val historyManager = HistoryManager.getInstance()
    private val groupManager = GroupManager.getInstance()
    private val userRepository = UserRepository.getInstance()
    private var members: List<User> = emptyList()
    private var restoredMemberId: String? = null
    private val items = mutableListOf<Transaction>()
    private val adapter = object : RecyclerView.Adapter<TransactionHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TransactionHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        )
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: TransactionHolder, position: Int) {
            val transaction = items[position]
            val settlement = transaction.type == TransactionType.SETTLEMENT
            holder.title.text = if (settlement) getString(R.string.settlement_label) else transaction.title
            holder.date.text = transaction.date
            holder.amount.text = formatMoney(transaction.amount)
            holder.category.text = if (settlement) getString(R.string.settlement_label)
                else displayChoice(transaction.category.ifBlank { "OTHER" })
            holder.icon.setImageResource(if (settlement) R.drawable.ic_swap else when (
                ExpenseCategory.fromString(transaction.category)
            ) {
                ExpenseCategory.FOOD -> R.drawable.ic_food
                ExpenseCategory.HOUSING -> R.drawable.ic_house
                ExpenseCategory.TRANSPORTATION -> R.drawable.ic_car
                ExpenseCategory.ENTERTAINMENT -> R.drawable.ic_play
                ExpenseCategory.OTHER -> R.drawable.ic_receipt
            })
            holder.itemView.setOnClickListener {
                startActivity(Intent(requireContext(), TransactionDetailActivity::class.java)
                    .putExtra(HistoryActivity.EXTRA_TX_ID, transaction.id)
                    .putExtra(HistoryActivity.EXTRA_TX_TYPE, transaction.type.name)
                    .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId))
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewBinding = ActivityHistoryBinding.bind(view)
        val context = requireContext()
        binding.rvHistory.layoutManager = LinearLayoutManager(context)
        binding.rvHistory.adapter = adapter
        binding.spType.adapter = context.choiceAdapter(listOf("ALL") + TransactionType.entries.map { it.name })
        binding.spCategory.adapter = context.choiceAdapter(listOf("ALL") + ExpenseCategory.entries.map { it.name })
        binding.spSort.adapter = context.choiceAdapter(listOf("NEWEST", "OLDEST"))
        binding.spMember.adapter = context.choiceAdapter(listOf(getString(R.string.all_members)))
        binding.btnToggleFilters.setOnClickListener {
            val expanded = binding.layoutFilters.visibility != View.VISIBLE
            binding.layoutFilters.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.btnToggleFilters.isSelected = expanded
        }
        binding.btnApply.setOnClickListener {
            binding.layoutFilters.visibility = View.GONE
            binding.btnToggleFilters.isSelected = false
            refresh()
        }
        binding.etSearch.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                refresh()
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
                true
            } else false
        }
        restoredMemberId = savedInstanceState?.getString("memberId")
        savedInstanceState?.let { state ->
            binding.etSearch.setText(state.getString("keyword", ""))
            binding.spType.setSelection(state.getInt("type", 0))
            binding.spCategory.setSelection(state.getInt("category", 0))
            binding.spSort.setSelection(state.getInt("sort", 0))
            val expanded = state.getBoolean("filtersExpanded", false)
            binding.layoutFilters.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.btnToggleFilters.isSelected = expanded
        }
    }

    override fun refresh() {
        val type = binding.spType.selectedItem?.toString()
        val category = binding.spCategory.selectedItem?.toString()
        val memberIndex = binding.spMember.selectedItemPosition - 1
        val filter = HistoryManager.HistoryFilter(
            keyword = binding.etSearch.text?.toString(),
            type = type?.takeIf { it != "ALL" }?.let(TransactionType::valueOf),
            category = category?.takeIf { it != "ALL" }?.let(ExpenseCategory::valueOf),
            memberId = members.getOrNull(memberIndex)?.uid ?: restoredMemberId,
            sortOrder = if (binding.spSort.selectedItem?.toString() == "OLDEST")
                HistoryManager.SortOrder.OLDEST else HistoryManager.SortOrder.NEWEST
        )
        loading?.cancel()
        binding.progress.visibility = View.VISIBLE
        loading = loadData(fetch = {
            val users = groupManager.getGroup(groupId).memberIds.mapNotNull { userRepository.getById(it) }
            val currentFilter = filter.copy(memberId = filter.memberId?.takeIf { id -> users.any { it.uid == id } })
            users to historyManager.getHistory(groupId, currentFilter)
        }, render = { (users, transactions) ->
            members = users
            binding.spMember.adapter = requireContext().choiceAdapter(
                listOf(getString(R.string.all_members)) + users.map { "${it.name} (${it.email})" })
            binding.spMember.setSelection(users.indexOfFirst { it.uid == filter.memberId } + 1)
            restoredMemberId = null
            items.clear()
            items.addAll(transactions)
            adapter.notifyDataSetChanged()
            binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.progress.visibility = View.GONE
        }, onError = {
            binding.progress.visibility = View.GONE
            requireContext().toast(it)
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewBinding?.let { current ->
            outState.putString("keyword", current.etSearch.text?.toString())
            outState.putInt("type", current.spType.selectedItemPosition.coerceAtLeast(0))
            outState.putInt("category", current.spCategory.selectedItemPosition.coerceAtLeast(0))
            outState.putInt("sort", current.spSort.selectedItemPosition.coerceAtLeast(0))
            outState.putString("memberId", members.getOrNull(current.spMember.selectedItemPosition - 1)?.uid ?: restoredMemberId)
            outState.putBoolean("filtersExpanded", current.layoutFilters.visibility == View.VISIBLE)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.rvHistory.adapter = null
        viewBinding = null
        super.onDestroyView()
    }

    private class TransactionHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivCategory)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val date: TextView = view.findViewById(R.id.tvMeta)
        val amount: TextView = view.findViewById(R.id.tvAmount)
        val category: TextView = view.findViewById(R.id.tvCategory)
    }
}
