package com.splitsmart.app.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.splitsmart.app.R;
import com.splitsmart.app.business.AccountManager;
import com.splitsmart.app.business.FriendManager;
import com.splitsmart.app.business.GroupManager;
import com.splitsmart.app.data.UserRepository;
import com.splitsmart.app.databinding.ItemGroupMemberBinding;
import com.splitsmart.app.databinding.SheetGroupMembersBinding;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.List;

public class GroupMembersSheet extends BottomSheetDialogFragment {
    private SheetGroupMembersBinding binding;
    private final GroupManager groupManager = GroupManager.getInstance();
    private final AccountManager accountManager = AccountManager.getInstance();
    private UiTask loading;
    private boolean busy = false;

    private static class MemberRow {
        final String uid;
        final String name;
        final String email;

        MemberRow(String uid, String name, String email) {
            this.uid = uid;
            this.name = name;
            this.email = email;
        }
    }

    private static class MembersData {
        final Group group;
        final List<MemberRow> members;

        MembersData(Group group, List<MemberRow> members) {
            this.group = group;
            this.members = members;
        }
    }

    private String getGroupId() {
        String groupId = requireArguments().getString(HomeActivity.EXTRA_GROUP_ID);
        return groupId == null ? "" : groupId;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetGroupMembersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String groupId = getGroupId();
        binding.btnClose.setOnClickListener(v -> dismiss());
        binding.btnAddMember.setOnClickListener(v -> {
            String email = UiUtils.text(binding.etMemberEmail);
            mutate(() -> {
                groupManager.inviteMemberByEmail(groupId, email);
                return null;
            }, ignored -> {
                binding.etMemberEmail.setText("");
                UiUtils.toast(requireContext(), getString(R.string.group_invite_sent));
                notifyChanged();
                refresh();
            });
        });
        binding.btnAddFromFriends.setOnClickListener(v -> showInviteFriends());
        binding.btnLeaveGroup.setOnClickListener(v ->
                UiUtils.confirm(requireContext(), getString(R.string.leave_group),
                        getString(R.string.leave_group_confirm), () -> mutate(() -> {
                            groupManager.leaveGroup(groupId);
                            return null;
                        }, ignored -> {
                            UiUtils.toast(requireContext(), getString(R.string.group_left));
                            requireActivity().finish();
                        })));
        refresh();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetBehavior<?> behavior = ((BottomSheetDialog) dialog).getBehavior();
            behavior.setMaxHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.85));
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void refresh() {
        String groupId = getGroupId();
        if (loading != null) loading.cancel();
        binding.progress.setVisibility(View.VISIBLE);
        loading = UiUtils.loadData(this, () -> {
            Group group = groupManager.getGroup(groupId);
            List<MemberRow> members = new ArrayList<>();
            for (String uid : group.getMemberIds()) {
                User user = UserRepository.getInstance().getById(uid);
                members.add(new MemberRow(uid, user != null ? user.getName() : uid, user != null ? user.getEmail() : ""));
            }
            return new MembersData(group, members);
        }, this::render, this::showError);
    }

    private void render(MembersData data) {
        Group group = data.group;
        String current = accountManager.currentUid();
        boolean owner = group.getOwnerId().equals(current);
        binding.tvDescription.setText(group.getDescription());
        binding.tvDescription.setVisibility(group.getDescription().trim().isEmpty() ? View.GONE : View.VISIBLE);
        binding.layoutAddMember.setVisibility(owner ? View.VISIBLE : View.GONE);
        binding.btnAddFromFriends.setVisibility(owner ? View.VISIBLE : View.GONE);
        binding.btnLeaveGroup.setVisibility(
                !owner && current != null && group.getMemberIds().contains(current) ? View.VISIBLE : View.GONE);
        binding.llMemberList.removeAllViews();
        for (MemberRow member : data.members) {
            ItemGroupMemberBinding row = ItemGroupMemberBinding.inflate(getLayoutInflater(), binding.llMemberList, false);
            boolean isOwner = member.uid.equals(group.getOwnerId());
            row.tvAvatar.setText(UiUtils.initials(member.name));
            row.tvMemberName.setText(isOwner ? getString(R.string.member_owner, member.name) : member.name);
            row.tvMemberEmail.setText(member.email);
            row.btnRemoveMember.setVisibility(owner && !isOwner ? View.VISIBLE : View.GONE);
            row.btnRemoveMember.setOnClickListener(v ->
                    UiUtils.confirm(requireContext(), getString(R.string.remove_member_title),
                            getString(R.string.remove_member_confirm, member.name),
                            () -> mutate(() -> groupManager.removeMember(group.getId(), member.uid), ignored -> {
                                UiUtils.toast(requireContext(), getString(R.string.member_removed));
                                notifyChanged();
                                refresh();
                            })));
            binding.llMemberList.addView(row.getRoot());
        }
        binding.progress.setVisibility(View.GONE);
    }

    private void showInviteFriends() {
        String groupId = getGroupId();
        mutate(() -> {
            Group group = groupManager.getGroup(groupId);
            List<User> candidates = new ArrayList<>();
            for (User friend : FriendManager.getInstance().listFriends()) {
                if (!group.getMemberIds().contains(friend.getUid())) candidates.add(friend);
            }
            return candidates;
        }, candidates -> {
            binding.progress.setVisibility(View.GONE);
            if (candidates.isEmpty()) {
                UiUtils.toast(requireContext(), getString(R.string.no_friends_to_add));
                return;
            }
            String[] labels = new String[candidates.size()];
            for (int i = 0; i < labels.length; i++) {
                labels[i] = candidates.get(i).getName() + " (" + candidates.get(i).getEmail() + ")";
            }
            boolean[] checked = new boolean[candidates.size()];
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.add_from_friends)
                    .setMultiChoiceItems(labels, checked, (dialog, index, selected) -> checked[index] = selected)
                    .setPositiveButton(R.string.invite_short, (dialog, which) -> {
                        List<User> chosen = new ArrayList<>();
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) chosen.add(candidates.get(i));
                        }
                        inviteFriends(chosen);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    private void inviteFriends(List<User> friends) {
        if (friends.isEmpty()) return;
        String groupId = getGroupId();
        mutate(() -> {
            int sent = 0;
            for (User friend : friends) {
                try {
                    groupManager.inviteMemberByUid(groupId, friend.getUid());
                    sent++;
                } catch (RuntimeException ignored) {
                    // Keep sending the remaining invitations when one is already pending.
                }
            }
            return sent;
        }, sent -> {
            binding.progress.setVisibility(View.GONE);
            UiUtils.toast(requireContext(), getString(sent > 0 ? R.string.group_invite_sent : R.string.no_new_invites));
        });
    }

    /** Runs a change with the action buttons disabled so it cannot be submitted twice. */
    private <T> void mutate(UiUtils.Work<T> action, UiUtils.Callback<T> onSuccess) {
        if (busy) return;
        busy = true;
        setActionsEnabled(false);
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.loadData(this, action, result -> {
            busy = false;
            setActionsEnabled(true);
            onSuccess.accept(result);
        }, message -> {
            busy = false;
            setActionsEnabled(true);
            showError(message);
        });
    }

    private void setActionsEnabled(boolean enabled) {
        binding.btnAddFromFriends.setEnabled(enabled);
        binding.btnAddMember.setEnabled(enabled);
        binding.btnLeaveGroup.setEnabled(enabled);
    }

    private void notifyChanged() {
        getParentFragmentManager().setFragmentResult(GroupDetailActivity.MEMBERS_CHANGED, new Bundle());
    }

    private void showError(String message) {
        binding.progress.setVisibility(View.GONE);
        UiUtils.toast(requireContext(), message);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
