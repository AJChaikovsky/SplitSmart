package com.splitsmart.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.splitsmart.app.R;
import com.splitsmart.app.business.FriendManager;
import com.splitsmart.app.business.GroupManager;
import com.splitsmart.app.databinding.ActivityCreateGroupBinding;
import com.splitsmart.app.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreateGroupActivity extends AppCompatActivity {
    private ActivityCreateGroupBinding binding;
    private final GroupManager groupManager = GroupManager.getInstance();
    private final FriendManager friendManager = FriendManager.getInstance();
    private static final String STATE_CHECKED = "checkedFriends";
    private final Map<String, MaterialCheckBox> friendChecks = new LinkedHashMap<>();
    /** Friends ticked before a rotation or theme change, re-applied once the list reloads. */
    private ArrayList<String> restoredChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.create_group));
        }

        if (savedInstanceState != null) restoredChecked = savedInstanceState.getStringArrayList(STATE_CHECKED);
        loadFriends();

        binding.btnSave.setOnClickListener(v -> {
            String name = UiUtils.text(binding.etName);
            String description = UiUtils.text(binding.etDescription);
            List<String> selected = new ArrayList<>();
            for (Map.Entry<String, MaterialCheckBox> entry : friendChecks.entrySet()) {
                if (entry.getValue().isChecked()) selected.add(entry.getKey());
            }
            binding.progress.setVisibility(View.VISIBLE);
            UiUtils.launchIo(this, () -> groupManager.createGroup(name, description, selected), group -> {
                binding.progress.setVisibility(View.GONE);
                startActivity(new Intent(this, GroupDetailActivity.class)
                        .putExtra(HomeActivity.EXTRA_GROUP_ID, group.getId()));
                finish();
            }, this::showError);
        });
    }

    private void loadFriends() {
        binding.progress.setVisibility(View.VISIBLE);
        UiUtils.launchIo(this, friendManager::listFriends, friends -> {
            binding.progress.setVisibility(View.GONE);
            renderFriendChecks(friends);
        }, this::showError);
    }

    private void renderFriendChecks(List<User> friends) {
        binding.llFriendChecks.removeAllViews();
        friendChecks.clear();
        if (friends.isEmpty()) {
            binding.tvNoFriendsHint.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvNoFriendsHint.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (User friend : friends) {
            MaterialCheckBox checkBox = (MaterialCheckBox) inflater.inflate(
                    R.layout.item_friend_check, binding.llFriendChecks, false);
            checkBox.setText(friend.getName() + " (" + friend.getEmail() + ")");
            if (restoredChecked != null) checkBox.setChecked(restoredChecked.contains(friend.getUid()));
            friendChecks.put(friend.getUid(), checkBox);
            binding.llFriendChecks.addView(checkBox);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        ArrayList<String> checked = new ArrayList<>();
        for (Map.Entry<String, MaterialCheckBox> entry : friendChecks.entrySet()) {
            if (entry.getValue().isChecked()) checked.add(entry.getKey());
        }
        // Before the list loads, carry forward whatever was restored.
        outState.putStringArrayList(STATE_CHECKED, friendChecks.isEmpty() && restoredChecked != null
                ? restoredChecked : checked);
        super.onSaveInstanceState(outState);
    }

    private void showError(String message) {
        binding.progress.setVisibility(View.GONE);
        UiUtils.toast(this, message);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
