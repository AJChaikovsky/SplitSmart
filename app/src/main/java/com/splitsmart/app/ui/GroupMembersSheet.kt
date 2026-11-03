package com.splitsmart.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.splitsmart.app.R
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.data.UserRepository
import com.splitsmart.app.databinding.ItemGroupMemberBinding
import com.splitsmart.app.databinding.SheetGroupMembersBinding
import kotlinx.coroutines.Job

class GroupMembersSheet : BottomSheetDialogFragment() {
    private var viewBinding: SheetGroupMembersBinding? = null
    private val binding get() = requireNotNull(viewBinding)
    private val groupId get() = requireArguments().getString(HomeActivity.EXTRA_GROUP_ID).orEmpty()
    private val groupManager = GroupManager.getInstance()
    private val accountManager = AccountManager.getInstance()
    private var loading: Job? = null
    private var busy = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = SheetGroupMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnAddMember.setOnClickListener {
            val email = binding.etMemberEmail.text?.toString().orEmpty()
            mutate(action = { groupManager.addMemberByEmail(groupId, email) }, onSuccess = {
                binding.etMemberEmail.setText("")
                requireContext().toast(getString(R.string.member_added))
                notifyChanged()
                refresh()
            })
        }
        binding.btnLeaveGroup.setOnClickListener {
            requireContext().confirm(getString(R.string.leave_group), getString(R.string.leave_group_confirm)) {
                mutate(action = { groupManager.leaveGroup(groupId) }, onSuccess = {
                    requireContext().toast(getString(R.string.group_left))
                    requireActivity().finish()
                })
            }
        }
        refresh()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            maxHeight = (resources.displayMetrics.heightPixels * 0.85).toInt()
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun refresh() {
        loading?.cancel()
        binding.progress.visibility = View.VISIBLE
        loading = loadData(fetch = {
            val group = groupManager.getGroup(groupId)
            val members = group.memberIds.map { uid ->
                val user = UserRepository.getInstance().getById(uid)
                Triple(uid, user?.name ?: uid, user?.email.orEmpty())
            }
            group to members
        }, render = { (group, members) ->
            val current = accountManager.currentUid()
            val owner = current == group.ownerId
            binding.tvDescription.text = group.description
            binding.tvDescription.visibility = if (group.description.isBlank()) View.GONE else View.VISIBLE
            binding.layoutAddMember.visibility = if (owner) View.VISIBLE else View.GONE
            binding.btnLeaveGroup.visibility = if (!owner && current != null && current in group.memberIds) View.VISIBLE else View.GONE
            binding.llMemberList.removeAllViews()
            members.forEach { (uid, name, email) ->
                val row = ItemGroupMemberBinding.inflate(layoutInflater, binding.llMemberList, false)
                row.tvAvatar.text = initials(name)
                row.tvMemberName.text = if (uid == group.ownerId) getString(R.string.member_owner, name) else name
                row.tvMemberEmail.text = email
                row.btnRemoveMember.visibility = if (owner && uid != group.ownerId) View.VISIBLE else View.GONE
                row.btnRemoveMember.setOnClickListener {
                    requireContext().confirm(getString(R.string.remove_member_title), getString(R.string.remove_member_confirm, name)) {
                        mutate(action = { groupManager.removeMember(groupId, uid) }, onSuccess = {
                            requireContext().toast(getString(R.string.member_removed))
                            notifyChanged()
                            refresh()
                        })
                    }
                }
                binding.llMemberList.addView(row.root)
            }
            binding.progress.visibility = View.GONE
        }, onError = { showError(it) })
    }

    private fun <T> mutate(action: suspend () -> T, onSuccess: (T) -> Unit) {
        if (busy) return
        busy = true
        binding.btnAddMember.isEnabled = false
        binding.btnLeaveGroup.isEnabled = false
        binding.progress.visibility = View.VISIBLE
        loadData(fetch = action, render = {
            busy = false
            binding.btnAddMember.isEnabled = true
            binding.btnLeaveGroup.isEnabled = true
            onSuccess(it)
        }, onError = {
            busy = false
            binding.btnAddMember.isEnabled = true
            binding.btnLeaveGroup.isEnabled = true
            showError(it)
        })
    }

    private fun notifyChanged() {
        parentFragmentManager.setFragmentResult(GroupDetailActivity.MEMBERS_CHANGED, Bundle())
    }

    private fun showError(message: String) {
        binding.progress.visibility = View.GONE
        requireContext().toast(message)
    }

    override fun onDestroyView() {
        viewBinding = null
        super.onDestroyView()
    }
}
