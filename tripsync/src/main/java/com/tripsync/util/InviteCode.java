package com.tripsync.util;

import java.security.SecureRandom;

/**
 * 모임 초대 코드를 만드는 유틸리티.
 *
 * <p>형식은 {@code TS} + 영숫자 4자 (예: {@code TS4K7Q}) 로 고정한다.
 * 앞에 TS 를 붙여 사용자가 "이건 TripSync 코드구나"를 바로 알 수 있게 하고,
 * 뒤 4자리만 무작위로 만들어 손으로 옮겨 적기 쉽게 했다.</p>
 *
 * <p>사람이 눈으로 읽고 따라 치는 코드이므로 혼동하기 쉬운 글자
 * (0/O, 1/I/L) 는 후보에서 뺐다.</p>
 */
public final class InviteCode {

    private static final String PREFIX = "TS";
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int RANDOM_LENGTH = 4;

    private static final SecureRandom RANDOM = new SecureRandom();

    private InviteCode() {
    }

    /** 새 초대 코드를 만든다. 예: {@code TS4K7Q} */
    public static String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** 사용자가 입력한 코드를 비교 가능한 형태로 다듬는다 (공백 제거 + 대문자). */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\s+", "").toUpperCase();
    }

    /** 코드 형식이 맞는지 검사한다. */
    public static boolean isValidFormat(String code) {
        String normalized = normalize(code);
        return normalized.matches("^" + PREFIX + "[" + ALPHABET + "]{" + RANDOM_LENGTH + "}$");
    }
}
