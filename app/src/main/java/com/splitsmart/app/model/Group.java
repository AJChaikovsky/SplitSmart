package com.splitsmart.app.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Group {
    private String id = "";
    private String name = "";
    private String description = "";
    private String ownerId = "";
    private List<String> memberEmails = new ArrayList<>();
    private List<String> memberIds = new ArrayList<>();
    private long createdAt = System.currentTimeMillis();

    public Group() {
    }

    public Group(String id, String name, String description, String ownerId,
                 List<String> memberEmails, List<String> memberIds, long createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.memberEmails = new ArrayList<>(memberEmails);
        this.memberIds = new ArrayList<>(memberIds);
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public List<String> getMemberEmails() { return memberEmails; }
    public void setMemberEmails(List<String> memberEmails) { this.memberEmails = new ArrayList<>(memberEmails); }
    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = new ArrayList<>(memberIds); }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("description", description);
        map.put("ownerId", ownerId);
        map.put("memberEmails", memberEmails);
        map.put("memberIds", memberIds);
        map.put("createdAt", createdAt);
        return map;
    }

    public static Group fromMap(Map<String, Object> map) {
        return new Group(
                MapReader.str(map, "id", ""),
                MapReader.str(map, "name", ""),
                MapReader.str(map, "description", ""),
                MapReader.str(map, "ownerId", ""),
                MapReader.strList(map, "memberEmails"),
                MapReader.strList(map, "memberIds"),
                MapReader.lng(map, "createdAt", 0L)
        );
    }
}
