package com.splitsmart.app.data;

import com.splitsmart.app.model.Friendship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class FriendRepository {
    private static volatile FriendRepository instance;

    private final FirestoreManager firestore;

    public FriendRepository() {
        this(FirestoreManager.getInstance());
    }

    public FriendRepository(FirestoreManager firestore) {
        this.firestore = firestore;
    }

    public static FriendRepository getInstance() {
        if (instance == null) {
            synchronized (FriendRepository.class) {
                if (instance == null) instance = new FriendRepository();
            }
        }
        return instance;
    }

    public Friendship save(Friendship friendship) {
        if (friendship.getId().trim().isEmpty()) {
            if (friendship.getUserIds().size() != 2) {
                throw new IllegalArgumentException("Friendship needs two users");
            }
            friendship.setId(Friendship.docIdFor(friendship.getUserIds().get(0), friendship.getUserIds().get(1)));
        }
        firestore.setDocument(FirestoreManager.COLLECTION_FRIENDSHIPS, friendship.getId(), friendship.toMap());
        return friendship;
    }

    public Friendship getById(String id) {
        Map<String, Object> data = firestore.getDocument(FirestoreManager.COLLECTION_FRIENDSHIPS, id);
        return data == null ? null : Friendship.fromMap(data);
    }

    public List<Friendship> listForUser(String userId) {
        List<Friendship> friendships = new ArrayList<>();
        for (Map<String, Object> data : firestore.queryArrayContains(
                FirestoreManager.COLLECTION_FRIENDSHIPS, "userIds", userId)) {
            friendships.add(Friendship.fromMap(data));
        }
        friendships.sort(Comparator.comparingLong(Friendship::getCreatedAt).reversed());
        return friendships;
    }

    public void delete(String id) {
        firestore.deleteDocument(FirestoreManager.COLLECTION_FRIENDSHIPS, id);
    }
}
