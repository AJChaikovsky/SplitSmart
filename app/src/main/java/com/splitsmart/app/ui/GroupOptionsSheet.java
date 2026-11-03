package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.business.GroupManager;
import com.splitsmart.app.databinding.SheetGroupOptionsBinding;

public class GroupOptionsSheet extends BottomSheetDialogFragment {
    private SheetGroupOptionsBinding binding;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetGroupOptionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String extra = requireArguments().getString(HomeActivity.EXTRA_GROUP_ID);
        String groupId = extra == null ? "" : extra;
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnManageMembers.setOnClickListener(v -> {
            ((GroupDetailActivity) requireActivity()).showMembers();
            dismiss();
        });
        binding.btnGroupSettings.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), EditGroupActivity.class)
                    .putExtra(HomeActivity.EXTRA_GROUP_ID, groupId));
            dismiss();
        });
        UiUtils.loadData(this, () -> GroupManager.getInstance().getGroup(groupId), group -> {
            boolean owner = group.getOwnerId().equals(AccountManager.getInstance().currentUid());
            binding.btnGroupSettings.setVisibility(owner ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
