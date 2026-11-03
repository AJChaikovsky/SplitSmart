package com.splitsmart.app.model;

public enum SplitMethod {
    EQUAL,
    EXACT,
    PERCENTAGE;

    public static SplitMethod fromString(String value) {
        if (value == null || value.trim().isEmpty()) return EQUAL;
        for (SplitMethod method : values()) {
            if (method.name().equalsIgnoreCase(value)) return method;
        }
        return EQUAL;
    }
}
