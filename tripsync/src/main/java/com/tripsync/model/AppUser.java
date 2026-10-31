package com.tripsync.model;

/**
 * 로컬 프로필 한 명.
 *
 * <p>로그인 서버는 이 과제의 범위 밖이므로, 설계서 9장이 허용한 대로
 * 표시 이름만 가진 단순한 형태로 둔다. 이 프로필은 한 PC 안에서
 * "모임을 만든 사람"과 "초대 코드로 들어온 사람"을 구분해 보여주기 위한 장치이고,
 * 비밀번호나 인증이 걸린 계정이 아니다.</p>
 *
 * @param id          DB 가 매긴 번호. 아직 저장 전이면 {@link #UNSAVED}
 * @param displayName 화면에 보이는 이름
 */
public record AppUser(long id, String displayName) {

    /** 아직 DB 에 저장되지 않은 객체의 id */
    public static final long UNSAVED = -1L;

    /** 저장 전 상태의 새 프로필을 만든다. */
    public static AppUser of(String displayName) {
        return new AppUser(UNSAVED, displayName);
    }

    public boolean isSaved() {
        return id != UNSAVED;
    }
}
