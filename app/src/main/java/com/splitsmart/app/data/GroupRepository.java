package com.splitsmart.app.data;

import com.splitsmart.app.model.Group;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupRepository {
    private static volatile GroupRepository instance;

    private final FirestoreManager firestore;

    public GroupRepository() {
        this(FirestoreManager.getInstance());
    }

    public GroupRepository(FirestoreManager firestore) {
        this.firestore = firestore;
    }

    public static GroupRepository getInstance() {
        if (instance == null) {
            synchronized (GroupRepository.class) {
                if (instance == null) instance = new GroupRepository();
            }
        }
        return instance;
    }

    /** Saves the group, assigning a new id if it has none, and returns it. */
    public Group save(Group group) {
        if (group.getId().trim().isEmpty()) group.setId(UUID.randomUUID().toString());
        firestore.setDocument(FirestoreManager.COLLECTION_GROUPS, group.getId(), group.toMap());
        return group;
    }

    public Group getById(String id) {
        Map<String, Object> data = firestore.getDocument(FirestoreManager.COLLECTION_GROUPS, id);
        return data == null ? null : Group.fromMap(data);
    }

    public List<Group> getGroupsForUser(String userId) {
        List<Group> groups = new ArrayList<>();
        for (Map<String, Object> data : firestore.queryArrayContains(
                FirestoreManager.COLLECTION_GROUPS, "memberIds", userId)) {
            groups.add(Group.fromMap(data));
        }
        groups.sort(Comparator.comparingLong(Group::getCreatedAt).reversed());
        return groups;
    }

    public void delete(String id) {
        firestore.deleteDocument(FirestoreManager.COLLECTION_GROUPS, id);
    }

    /** Self-join without reading the group first (invite accept). */
    public void addMemberFields(String groupId, String memberUid, String memberEmail) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("memberIds", memberUid);
        fields.put("memberEmails", memberEmail);
        firestore.arrayUnion(FirestoreManager.COLLECTION_GROUPS, groupId, fields);
    }
}
