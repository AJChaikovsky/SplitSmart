package com.splitsmart.app.data;

import com.splitsmart.app.model.User;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserRepository {
    private static volatile UserRepository instance;

    private final FirestoreManager firestore;

    public UserRepository() {
        this(FirestoreManager.getInstance());
    }

    public UserRepository(FirestoreManager firestore) {
        this.firestore = firestore;
    }

    public static UserRepository getInstance() {
        if (instance == null) {
            synchronized (UserRepository.class) {
                if (instance == null) instance = new UserRepository();
            }
        }
        return instance;
    }

    public void save(User user) {
        firestore.setDocument(FirestoreManager.COLLECTION_USERS, user.getUid(), user.toMap());
    }

    public User getById(String uid) {
        Map<String, Object> data = firestore.getDocument(FirestoreManager.COLLECTION_USERS, uid);
        return data == null ? null : User.fromMap(data);
    }

    public User getByEmail(String email) {
        return first(firestore.queryEquals(
                FirestoreManager.COLLECTION_USERS, "email", email.toLowerCase(Locale.ROOT)));
    }

    public User getByUscId(String uscId) {
        return first(firestore.queryEquals(
                FirestoreManager.COLLECTION_USERS, "uscId", uscId.trim()));
    }

    public void updateName(String uid, String name) {
        firestore.updateDocument(
                FirestoreManager.COLLECTION_USERS,
                uid,
                Collections.singletonMap("name", name)
        );
    }

    private static User first(List<Map<String, Object>> results) {
        return results.isEmpty() ? null : User.fromMap(results.get(0));
    }
}
