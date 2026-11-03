package com.splitsmart.app.business;

import java.util.regex.Pattern;

/** Money math is done in whole cents so shares never drift by a fraction of a cent. */
public final class MoneyUtils {
    private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private MoneyUtils() {
    }

    public static long toCents(double amount) {
        return Math.round(amount * 100.0);
    }

    public static double fromCents(long cents) {
        return cents / 100.0;
    }

    public static double roundToCents(double amount) {
        return fromCents(toCents(amount));
    }

    public static boolean hasAtMostTwoDecimals(double amount) {
        double cents = amount * 100.0;
        return Math.abs(cents - Math.round(cents)) < 1e-9;
    }

    public static void requireIsoDate(String date) {
        Preconditions.require(ISO_DATE.matcher(date.trim()).matches(), "Date must be in YYYY-MM-DD format");
    }
}
