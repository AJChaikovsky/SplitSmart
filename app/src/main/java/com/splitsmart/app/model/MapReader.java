package com.splitsmart.app.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Null-safe reads from Firestore document maps. */
final class MapReader {
    private MapReader() {
    }

    static String str(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value instanceof String ? (String) value : fallback;
    }

    static double dbl(Map<String, Object> map, String key, double fallback) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    static Double nullableDbl(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    static long lng(Map<String, Object> map, String key, long fallback) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    static List<String> strList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof List)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item instanceof String) result.add((String) item);
        }
        return result;
    }
}
