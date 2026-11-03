package com.splitsmart.app.data;

import com.splitsmart.app.model.AppRequest;
import com.splitsmart.app.model.RequestStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RequestRepository {
    private static volatile RequestRepository instance;

    private final FirestoreManager firestore;

    public RequestRepository() {
        this(FirestoreManager.getInstance());
    }

    public RequestRepository(FirestoreManager firestore) {
        this.firestore = firestore;
    }

    public static RequestRepository getInstance() {
        if (instance == null) {
            synchronized (RequestRepository.class) {
                if (instance == null) instance = new RequestRepository();
            }
        }
        return instance;
    }

    public AppRequest save(AppRequest request) {
        if (request.getId().trim().isEmpty()) request.setId(UUID.randomUUID().toString());
        firestore.setDocument(FirestoreManager.COLLECTION_REQUESTS, request.getId(), request.toMap());
        return request;
    }

    public AppRequest getById(String id) {
        Map<String, Object> data = firestore.getDocument(FirestoreManager.COLLECTION_REQUESTS, id);
        return data == null ? null : AppRequest.fromMap(data);
    }

    public List<AppRequest> listIncoming(String userId) {
        return query("toUserId", userId);
    }

    public List<AppRequest> listOutgoing(String userId) {
        return query("fromUserId", userId);
    }

    public AppRequest updateStatus(String id, RequestStatus status) {
        AppRequest existing = getById(id);
        if (existing == null) throw new IllegalArgumentException("Request not found");
        existing.setStatus(status.name());
        firestore.setDocument(FirestoreManager.COLLECTION_REQUESTS, id, existing.toMap());
        return existing;
    }

    private List<AppRequest> query(String field, String userId) {
        List<AppRequest> requests = new ArrayList<>();
        for (Map<String, Object> data : firestore.queryEquals(
                FirestoreManager.COLLECTION_REQUESTS, field, userId)) {
            requests.add(AppRequest.fromMap(data));
        }
        requests.sort(Comparator.comparingLong(AppRequest::getCreatedAt).reversed());
        return requests;
    }
}
