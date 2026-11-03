package com.splitsmart.app.model;

import java.util.HashMap;
import java.util.Map;

public class Settlement {
    private String id = "";
    private String groupId = "";
    private String fromUserId = "";
    private String toUserId = "";
    private double amount = 0.0;
    private String date = "";
    private String note = "";
    private String creatorId = "";

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("groupId", groupId);
        map.put("fromUserId", fromUserId);
        map.put("toUserId", toUserId);
        map.put("amount", amount);
        map.put("date", date);
        map.put("note", note);
        map.put("creatorId", creatorId);
        return map;
    }

    public static Settlement fromMap(Map<String, Object> map) {
        Settlement settlement = new Settlement();
        settlement.id = MapReader.str(map, "id", "");
        settlement.groupId = MapReader.str(map, "groupId", "");
        settlement.fromUserId = MapReader.str(map, "fromUserId", "");
        settlement.toUserId = MapReader.str(map, "toUserId", "");
        settlement.amount = MapReader.dbl(map, "amount", 0.0);
        settlement.date = MapReader.str(map, "date", "");
        settlement.note = MapReader.str(map, "note", "");
        settlement.creatorId = MapReader.str(map, "creatorId", "");
        return settlement;
    }
}
