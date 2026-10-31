package com.tripsync.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 날짜·시간을 화면에 보여줄 문자열로 바꾸는 곳.
 *
 * <p>같은 날짜를 화면마다 다른 모양으로 쓰면 사용자가 매번 다시 읽어야 한다.
 * 포맷을 여기 한 곳에 모아두고 전부 이것만 쓴다.</p>
 *
 * <p>저장할 때는 포맷을 거치지 않고 {@code LocalDate.toString()} 의 ISO 형식
 * ({@code 2026-11-14}) 을 그대로 쓴다. 보여주기용과 저장용을 섞지 않기 위해서다.</p>
 */
public final class Dates {

    /** 2026. 11. 14. */
    private static final DateTimeFormatter MEDIUM = DateTimeFormatter.ofPattern("yyyy. M. d.");

    /** 2026. 11. 14. (토) */
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy. M. d. (E)");

    /** 11/14 */
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("M/d");

    /** 14:30 */
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

    /** "2026. 11. 14. ~ 2026. 11. 17." */
    public static String period(LocalDate start, LocalDate end) {
        return medium(start) + " ~ " + medium(end);
    }

    /** "3박 4일" 또는 "당일치기" */
    public static String duration(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return "";
        }
        long nights = ChronoUnit.DAYS.between(start, end);
        return nights == 0 ? "당일치기" : nights + "박 " + (nights + 1) + "일";
    }

    /**
     * 사용자가 적은 시간 문자열을 해석한다.
     * "14:30", "1430", "9:5" 를 모두 받아주고, 해석할 수 없으면 {@code null} 을 돌려준다.
     */
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
