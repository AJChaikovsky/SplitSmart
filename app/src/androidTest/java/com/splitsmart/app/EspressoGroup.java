package com.splitsmart.app;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.splitsmart.app.business.GroupManager;
import com.splitsmart.app.model.Group;
import com.splitsmart.app.ui.CreateExpenseActivity;
import com.splitsmart.app.ui.HomeActivity;

/**
 * Locates (or creates) the seeded “Espresso” group used by signed-in black-box tests.
 * Split-preview tests need at least three members already in that group.
 */
final class EspressoGroup {
    static final String NAME = "Espresso";

    private EspressoGroup() {
    }

    /** Finds the Espresso group, creating an empty one owned by the test account if missing. */
    static Group requireGroup() {
        TestAccount.ensureSignedIn();
        GroupManager groups = GroupManager.getInstance();
        for (Group group : groups.listGroupsForCurrentUser()) {
            if (NAME.equals(group.getName()) && group.getId() != null && !group.getId().trim().isEmpty()) {
                return group;
            }
        }
        return groups.createGroup(NAME, "Seeded group for Espresso black-box tests", java.util.Collections.emptyList());
    }

    static Group requireGroupWithMembers(int minimumMembers) {
        Group group = requireGroup();
        assumeTrue(
                "Seed the Espresso group with at least " + minimumMembers
                        + " members (invite friends and accept) before running split preview tests",
                group.getMemberIds().size() >= minimumMembers);
        return group;
    }

    static Intent addExpenseIntent(String groupId) {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, CreateExpenseActivity.class);
        intent.putExtra(HomeActivity.EXTRA_GROUP_ID, groupId);
        return intent;
    }
}
