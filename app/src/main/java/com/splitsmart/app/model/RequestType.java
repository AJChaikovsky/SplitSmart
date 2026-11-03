package com.splitsmart.app.model;

public enum RequestType {
    FRIEND,
    GROUP_INVITE;

    public static RequestType fromString(String value) {
        for (RequestType type : values()) {
            if (type.name().equalsIgnoreCase(value)) return type;
        }
        return FRIEND;
    }
}
