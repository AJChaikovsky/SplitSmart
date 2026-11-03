package com.splitsmart.app.model;

import java.util.HashMap;
import java.util.Map;

/** A friend request or group invite that the recipient accepts or denies. */
public class AppRequest {
    private String id = "";
    private String type = RequestType.FRIEND.name();
    private String status = RequestStatus.PENDING.name();
    private String fromUserId = "";
    private String toUserId = "";
    private String groupId = "";
    private String groupName = "";
    private long createdAt = 0L;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public RequestType typeEnum() {
        return RequestType.fromString(type);
    }

    public RequestStatus statusEnum() {
        return RequestStatus.fromString(status);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("type", type);
        map.put("status", status);
        map.put("fromUserId", fromUserId);
        map.put("toUserId", toUserId);
        map.put("groupId", groupId);
        map.put("groupName", groupName);
        map.put("createdAt", createdAt);
        return map;
    }

    public static AppRequest fromMap(Map<String, Object> map) {
        AppRequest request = new AppRequest();
        request.id = MapReader.str(map, "id", "");
        request.type = MapReader.str(map, "type", RequestType.FRIEND.name());
        request.status = MapReader.str(map, "status", RequestStatus.PENDING.name());
        request.fromUserId = MapReader.str(map, "fromUserId", "");
        request.toUserId = MapReader.str(map, "toUserId", "");
        request.groupId = MapReader.str(map, "groupId", "");
        request.groupName = MapReader.str(map, "groupName", "");
        request.createdAt = MapReader.lng(map, "createdAt", 0L);
        return request;
    }
}
