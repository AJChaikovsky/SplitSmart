package com.splitsmart.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.splitsmart.app.R
import com.splitsmart.app.business.AccountManager
import com.splitsmart.app.business.GroupManager
import com.splitsmart.app.databinding.SheetGroupOptionsBinding

class GroupOptionsSheet : BottomSheetDialogFragment() {
    private var viewBinding: SheetGroupOptionsBinding? = null
    private val binding get() = requireNotNull(viewBinding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = SheetGroupOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val groupId = requireArguments().getString(HomeActivity.EXTRA_GROUP_ID).orEmpty()
        binding.themeToggle.text = getString(R.string.dark_mode)
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnManageMembers.setOnClickListener {
            (requireActivity() as GroupDetailActivity).showMembers()
            dismiss()
        }
        binding.btnGroupSettings.setOnClickListener {
            startActivity(Intent(requireContext(), EditGroupActivity::class.java)
                .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId))
            dismiss()
        }
        loadData(fetch = { GroupManager.getInstance().getGroup(groupId) }, render = { group ->
            binding.btnGroupSettings.visibility = if (group.ownerId == AccountManager.getInstance().currentUid())
                View.VISIBLE else View.GONE
        })
    }

    override fun onDestroyView() {
        viewBinding = null
        super.onDestroyView()
    }
}
