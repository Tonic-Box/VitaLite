package com.tonic.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class PriceFormatter {
    private static final NumberFormat COMMA_FORMAT = NumberFormat.getNumberInstance(Locale.US);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.#");

    /**
     * Formats a gold amount according to the following rules:
     * - Under 100,000: formatted with commas + "gp" (e.g., "99,999gp")
     * - 100,000 to 9,999,999: divided by 1000 + "k" (e.g., "100k", "9,999k")
     * - 10,000,000+: divided by 1,000,000 + "m" (e.g., "10m", "999.5m")
     *
     * @param amount the gold amount to format
     * @return formatted string
     */
    public static String format(long amount) {
        if (amount < 100_000) {
            // Under 100k: format with commas and "gp"
            return COMMA_FORMAT.format(amount) + "gp";
        } else if (amount < 10_000_000) {
            // 100k to 10m: divide by 1000 and use "k"
            long thousands = amount / 1000;
            return COMMA_FORMAT.format(thousands) + "k";
        } else {
            // 10m+: divide by 1,000,000 and use "m"
            double millions = amount / 1_000_000.0;
            return DECIMAL_FORMAT.format(millions) + "m";
        }
    }

    /**
     * Alternative version with configurable decimal places for millions
     */
    public static String format(long amount, int millionDecimalPlaces) {
        if (amount < 100_000) {
            return COMMA_FORMAT.format(amount) + "gp";
        } else if (amount < 10_000_000) {
            long thousands = amount / 1000;
            return COMMA_FORMAT.format(thousands) + "k";
        } else {
            double millions = amount / 1_000_000.0;

            if (millionDecimalPlaces == 0) {
                return COMMA_FORMAT.format((long) millions) + "m";
            } else {
                String pattern = "#,##0." + "#".repeat(millionDecimalPlaces);
                DecimalFormat df = new DecimalFormat(pattern);
                return df.format(millions) + "m";
            }
        }
    }

    /**
     * Compact version without commas in the k/m values
     */
    public static String formatCompact(long amount) {
        if (amount < 100_000) {
            return COMMA_FORMAT.format(amount) + "gp";
        } else if (amount < 10_000_000) {
            return (amount / 1000) + "k";
        } else {
            double millions = amount / 1_000_000.0;
            // Show one decimal place only if needed
            if (millions == (long) millions) {
                return (long) millions + "m";
            } else {
                return String.format("%.1f", millions) + "m";
            }
        }
    }
}
