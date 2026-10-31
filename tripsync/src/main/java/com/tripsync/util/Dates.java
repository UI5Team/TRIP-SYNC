package com.tripsync.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class Dates {

    private static final DateTimeFormatter MEDIUM = DateTimeFormatter.ofPattern("yyyy. M. d.");

    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy. M. d. (E)");

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("M/d");

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private Dates() {
    }

    public static String medium(LocalDate date) {
        return date == null ? "" : date.format(MEDIUM);
    }

    public static String full(LocalDate date) {
        return date == null ? "" : date.format(FULL);
    }

    public static String shortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT);
    }

    public static String time(LocalTime time) {
        return time == null ? "" : time.format(TIME);
    }

    public static String period(LocalDate start, LocalDate end) {
        return medium(start) + " ~ " + medium(end);
    }

    public static String duration(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return "";
        }
        long nights = ChronoUnit.DAYS.between(start, end);
        return nights == 0 ? "당일치기" : nights + "박 " + (nights + 1) + "일";
    }

    public static LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        if (text.matches("^\\d{4}$")) {
            text = text.substring(0, 2) + ":" + text.substring(2);
        }
        if (!text.contains(":")) {
            return null;
        }
        String[] parts = text.split(":", 2);
        try {
            return LocalTime.of(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException | java.time.DateTimeException e) {
            return null;
        }
    }
}
