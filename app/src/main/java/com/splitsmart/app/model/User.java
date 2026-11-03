package com.splitsmart.app.model;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String uid = "";
    private String name = "";
    private String email = "";
    private String uscId = "";

    public User() {
    }

    public User(String uid, String name, String email, String uscId) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.uscId = uscId;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUscId() { return uscId; }
    public void setUscId(String uscId) { this.uscId = uscId; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("name", name);
        map.put("email", email);
        map.put("uscId", uscId);
        return map;
    }

    public static User fromMap(Map<String, Object> map) {
        return new User(
                MapReader.str(map, "uid", ""),
                MapReader.str(map, "name", ""),
                MapReader.str(map, "email", ""),
                MapReader.str(map, "uscId", "")
        );
    }
}
