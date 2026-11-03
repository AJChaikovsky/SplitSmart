package com.splitsmart.app.ui;

import androidx.fragment.app.Fragment;

/** A tab inside GroupDetailActivity that reloads its data whenever it becomes visible. */
public abstract class GroupTabFragment extends Fragment {
    protected GroupTabFragment(int layoutId) {
        super(layoutId);
    }

    protected String getGroupId() {
        String groupId = requireArguments().getString(HomeActivity.EXTRA_GROUP_ID);
        return groupId == null ? "" : groupId;
    }

    public abstract void refresh();

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }
}
