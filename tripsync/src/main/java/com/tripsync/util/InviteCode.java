package com.tripsync.util;

import java.security.SecureRandom;

public final class InviteCode {

    private static final String PREFIX = "TS";
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int RANDOM_LENGTH = 4;

    private static final SecureRandom RANDOM = new SecureRandom();

    private InviteCode() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\s+", "").toUpperCase();
    }

    public static boolean isValidFormat(String code) {
        String normalized = normalize(code);
        return normalized.matches("^" + PREFIX + "[" + ALPHABET + "]{" + RANDOM_LENGTH + "}$");
    }
}
