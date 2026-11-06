package com.splitsmart.app.business;

import com.google.firebase.firestore.FirebaseFirestore;
import com.splitsmart.app.data.FirestoreManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory stand-in for Cloud Firestore so managers can be tested without a network. */
class FakeFirestore extends FirestoreManager {
    private final Map<String, Map<String, Map<String, Object>>> collections = new HashMap<>();

    FakeFirestore() {
        super((FirebaseFirestore) null);
    }

    private Map<String, Map<String, Object>> collection(String name) {
        return collections.computeIfAbsent(name, key -> new LinkedHashMap<>());
    }

    /** Copies lists so stored documents never share state with the caller, like a real database. */
    private static Map<String, Object> copy(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            result.put(entry.getKey(), value instanceof List ? new ArrayList<>((List<?>) value) : value);
        }
        return result;
    }

    int count(String collectionName) {
        return collection(collectionName).size();
    }

    @Override
    public Map<String, Object> getDocument(String collection, String id) {
        Map<String, Object> data = collection(collection).get(id);
        return data == null ? null : copy(data);
    }

    @Override
    public void setDocument(String collection, String id, Map<String, Object> data) {
        collection(collection).put(id, copy(data));
    }

    @Override
    public void updateDocument(String collection, String id, Map<String, Object> data) {
        Map<String, Object> existing = collection(collection).get(id);
        if (existing == null) throw new IllegalStateException("No document " + collection + "/" + id);
        existing.putAll(copy(data));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void arrayUnion(String collection, String id, Map<String, Object> fieldValues) {
        Map<String, Object> existing = collection(collection).get(id);
        if (existing == null) throw new IllegalStateException("No document " + collection + "/" + id);
        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            List<Object> list = existing.get(entry.getKey()) instanceof List
                    ? new ArrayList<>((List<Object>) existing.get(entry.getKey()))
                    : new ArrayList<>();
            if (!list.contains(entry.getValue())) list.add(entry.getValue());
            existing.put(entry.getKey(), list);
        }
    }

    @Override
    public void deleteDocument(String collection, String id) {
        collection(collection).remove(id);
    }

    @Override
    public List<Map<String, Object>> queryEquals(String collection, String field, Object value) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> data : collection(collection).values()) {
            if (value.equals(data.get(field))) results.add(copy(data));
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> queryArrayContains(String collection, String field, Object value) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> data : collection(collection).values()) {
            Object list = data.get(field);
            if (list instanceof List && ((List<?>) list).contains(value)) results.add(copy(data));
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> queryEqualsOrdered(String collection, String field, Object value,
                                                        String orderBy, boolean ascending) {
        return queryEquals(collection, field, value);
    }
}
