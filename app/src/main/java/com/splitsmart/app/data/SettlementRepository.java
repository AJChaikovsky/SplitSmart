package com.splitsmart.app.data;

import com.splitsmart.app.model.Settlement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SettlementRepository {
    private static volatile SettlementRepository instance;

    private final FirestoreManager firestore;

    public SettlementRepository() {
        this(FirestoreManager.getInstance());
    }

    public SettlementRepository(FirestoreManager firestore) {
        this.firestore = firestore;
    }

    public static SettlementRepository getInstance() {
        if (instance == null) {
            synchronized (SettlementRepository.class) {
                if (instance == null) instance = new SettlementRepository();
            }
        }
        return instance;
    }

    /** Saves the settlement, assigning a new id if it has none, and returns it. */
    public Settlement save(Settlement settlement) {
        if (settlement.getId().trim().isEmpty()) settlement.setId(UUID.randomUUID().toString());
        firestore.setDocument(FirestoreManager.COLLECTION_SETTLEMENTS, settlement.getId(), settlement.toMap());
        return settlement;
    }

    public Settlement getById(String id) {
        Map<String, Object> data = firestore.getDocument(FirestoreManager.COLLECTION_SETTLEMENTS, id);
        return data == null ? null : Settlement.fromMap(data);
    }

    /** All settlements in a group, newest date first. */
    public List<Settlement> getByGroup(String groupId) {
        List<Settlement> settlements = new ArrayList<>();
        for (Map<String, Object> data : firestore.queryEquals(
                FirestoreManager.COLLECTION_SETTLEMENTS, "groupId", groupId)) {
            settlements.add(Settlement.fromMap(data));
        }
        settlements.sort(Comparator.comparing(Settlement::getDate).reversed());
        return settlements;
    }

    public void delete(String id) {
        firestore.deleteDocument(FirestoreManager.COLLECTION_SETTLEMENTS, id);
    }

    public void deleteByGroup(String groupId) {
        for (Settlement settlement : getByGroup(groupId)) delete(settlement.getId());
    }
}
