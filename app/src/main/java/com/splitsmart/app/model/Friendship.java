package com.splitsmart.app.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Friendship {
    private String id = "";
    private List<String> userIds = new ArrayList<>();
    private String createdBy = "";
    private long createdAt = 0L;

    public Friendship() {
    }

    public Friendship(String id, List<String> userIds, String createdBy, long createdAt) {
        this.id = id;
        this.userIds = new ArrayList<>(userIds);
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<String> getUserIds() { return userIds; }
    public String getCreatedBy() { return createdBy; }
    public long getCreatedAt() { return createdAt; }

    /** Deterministic document id so a pair of users has at most one friendship. */
    public static String docIdFor(String uidA, String uidB) {
        return uidA.compareTo(uidB) <= 0 ? uidA + "_" + uidB : uidB + "_" + uidA;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("userIds", userIds);
        map.put("createdBy", createdBy);
        map.put("createdAt", createdAt);
        return map;
    }

    public static Friendship fromMap(Map<String, Object> map) {
        return new Friendship(
                MapReader.str(map, "id", ""),
                MapReader.strList(map, "userIds"),
                MapReader.str(map, "createdBy", ""),
                MapReader.lng(map, "createdAt", 0L)
        );
    }
}
