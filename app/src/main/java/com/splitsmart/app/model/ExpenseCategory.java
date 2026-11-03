package com.splitsmart.app.model;

public enum ExpenseCategory {
    FOOD,
    HOUSING,
    TRANSPORTATION,
    ENTERTAINMENT,
    OTHER;

    public static ExpenseCategory fromString(String value) {
        if (value == null || value.trim().isEmpty()) return OTHER;
        for (ExpenseCategory category : values()) {
            if (category.name().equalsIgnoreCase(value)) return category;
        }
        return OTHER;
    }
}
