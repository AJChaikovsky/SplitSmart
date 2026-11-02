package com.splitsmart.app.model

enum class SplitMethod {
    EQUAL,
    EXACT,
    PERCENTAGE;

    companion object {
        fun fromString(value: String?): SplitMethod {
            if (value.isNullOrBlank()) return EQUAL
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: EQUAL
        }
    }
}
