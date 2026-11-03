package com.splitsmart.app.model;

public enum RequestStatus {
    PENDING,
    ACCEPTED,
    DENIED;

    public static RequestStatus fromString(String value) {
        for (RequestStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) return status;
        }
        return PENDING;
    }
}
