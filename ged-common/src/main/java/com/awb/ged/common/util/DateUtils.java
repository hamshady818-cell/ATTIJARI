package com.awb.ged.common.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * <h1>DateUtils</h1>
 * <p>
 * Utility class for date and time manipulations in the GED-AWB system.
 * Designed to enforce working exclusively in UTC using Java's {@link Instant} class.
 * </p>
 * <p>
 * As per architecture guidelines, timezone conversions must never occur at the backend core level;
 * they are deferred entirely to the presentation or client-side UI.
 * </p>
 */
public final class DateUtils {

    /**
     * ISO-8601 UTC formatter (e.g. 2026-07-14T12:00:00Z)
     */
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private DateUtils() {
        // Prevent instantiation
    }

    /**
     * Obtains the current system timestamp in UTC.
     *
     * @return the current {@link Instant}
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Formats an {@link Instant} to its ISO-8601 UTC string representation.
     *
     * @param instant the instant to format
     * @return the formatted ISO string (e.g., "2026-07-14T12:00:00Z"), or null if instant is null
     */
    public static String formatIso(Instant instant) {
        if (instant == null) {
            return null;
        }
        return ISO_FORMATTER.format(instant);
    }

    /**
     * Parses an ISO-8601 UTC string representation back into an {@link Instant}.
     *
     * @param isoString the ISO string to parse (e.g., "2026-07-14T12:00:00Z")
     * @return the parsed {@link Instant}
     * @throws DateTimeParseException if the text cannot be parsed
     */
    public static Instant parseIso(String isoString) {
        if (isoString == null || isoString.trim().isEmpty()) {
            return null;
        }
        return Instant.parse(isoString);
    }

    /**
     * Returns a timestamp truncated to the specified unit (e.g. days or hours).
     * Useful for statistics and audit timelines.
     *
     * @param instant the instant to truncate
     * @param unit the chrono unit (e.g. {@link ChronoUnit#DAYS})
     * @return the truncated {@link Instant}
     */
    public static Instant truncateTo(Instant instant, ChronoUnit unit) {
        if (instant == null || unit == null) {
            return null;
        }
        return instant.truncatedTo(unit);
    }

    /**
     * Checks if a target timestamp is older than a specified duration relative to now.
     * Useful for checking validation limits, token TTLs, or temporary resource expiry.
     *
     * @param target the timestamp to check
     * @param amount the amount of time
     * @param unit the unit of time
     * @return true if the target timestamp is older than now minus duration
     */
    public static boolean isOlderThan(Instant target, long amount, ChronoUnit unit) {
        if (target == null || unit == null) {
            return false;
        }
        Instant threshold = Instant.now().minus(amount, unit);
        return target.isBefore(threshold);
    }
}
