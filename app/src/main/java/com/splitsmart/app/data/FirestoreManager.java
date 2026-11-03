package com.splitsmart.app.data;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Thin wrapper over Cloud Firestore. All methods block and must run off the main thread. */
public class FirestoreManager {
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_GROUPS = "groups";
    public static final String COLLECTION_EXPENSES = "expenses";
    public static final String COLLECTION_SETTLEMENTS = "settlements";
    public static final String COLLECTION_FRIENDSHIPS = "friendships";
    public static final String COLLECTION_REQUESTS = "requests";

    private static volatile FirestoreManager instance;

    private final FirebaseFirestore db;

    public FirestoreManager() {
        this(FirebaseFirestore.getInstance());
    }

    public FirestoreManager(FirebaseFirestore db) {
        this.db = db;
    }

    public static FirestoreManager getInstance() {
        if (instance == null) {
            synchronized (FirestoreManager.class) {
                if (instance == null) instance = new FirestoreManager();
            }
        }
        return instance;
    }

    public CollectionReference users() { return db.collection(COLLECTION_USERS); }
    public CollectionReference groups() { return db.collection(COLLECTION_GROUPS); }
    public CollectionReference expenses() { return db.collection(COLLECTION_EXPENSES); }
    public CollectionReference settlements() { return db.collection(COLLECTION_SETTLEMENTS); }
    public CollectionReference friendships() { return db.collection(COLLECTION_FRIENDSHIPS); }

    /** Returns the document's fields, or null if it does not exist. */
    public Map<String, Object> getDocument(String collection, String id) {
        DocumentSnapshot snapshot = TaskAwaiter.await(db.collection(collection).document(id).get());
        return snapshot.exists() ? snapshot.getData() : null;
    }

    public void setDocument(String collection, String id, Map<String, Object> data) {
        TaskAwaiter.await(db.collection(collection).document(id).set(data));
    }

    public void updateDocument(String collection, String id, Map<String, Object> data) {
        TaskAwaiter.await(db.collection(collection).document(id).update(data));
    }

    public void arrayUnion(String collection, String id, Map<String, Object> fieldValues) {
        Map<String, Object> updates = new HashMap<>();
        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            updates.put(entry.getKey(), FieldValue.arrayUnion(entry.getValue()));
        }
        TaskAwaiter.await(db.collection(collection).document(id).update(updates));
    }

    public void deleteDocument(String collection, String id) {
        TaskAwaiter.await(db.collection(collection).document(id).delete());
    }

    public List<Map<String, Object>> queryEquals(String collection, String field, Object value) {
        return documents(TaskAwaiter.await(
                db.collection(collection).whereEqualTo(field, value).get()));
    }

    public List<Map<String, Object>> queryArrayContains(String collection, String field, Object value) {
        return documents(TaskAwaiter.await(
                db.collection(collection).whereArrayContains(field, value).get()));
    }

    public List<Map<String, Object>> queryEqualsOrdered(String collection, String field, Object value,
                                                        String orderBy, boolean ascending) {
        Query.Direction direction = ascending ? Query.Direction.ASCENDING : Query.Direction.DESCENDING;
        return documents(TaskAwaiter.await(
                db.collection(collection).whereEqualTo(field, value).orderBy(orderBy, direction).get()));
    }

    private static List<Map<String, Object>> documents(QuerySnapshot snapshot) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            Map<String, Object> data = document.getData();
            if (data != null) results.add(data);
        }
        return results;
    }
}
