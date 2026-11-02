package com.splitsmart.app.model

enum class ExpenseCategory {
    FOOD,
    HOUSING,
    TRANSPORTATION,
    ENTERTAINMENT,
    OTHER;

    companion object {
        fun fromString(value: String?): ExpenseCategory {
            if (value.isNullOrBlank()) return OTHER
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
