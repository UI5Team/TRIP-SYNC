package com.tripsync.model;

import java.time.LocalDateTime;

/**
 * 모임 하나.
 *
 * <p>설계서 9장의 Group(groupId, groupName, inviteCode) 에 대응한다.
 * 모임은 여행을 여러 개 가질 수 있고, 참여자를 여러 명 가질 수 있다.</p>
 *
 * <p>불변 객체(record)로 둔 이유: 저장소에서 읽어온 값을 화면이 들고 다니는 동안
 * 아무도 몰래 바꾸지 못하게 하기 위해서다. 값을 바꿀 일이 생기면
 * 새 객체를 만들어 저장소에 넘긴다.</p>
 *
 * @param id         DB 가 매긴 번호. 아직 저장 전이면 {@link #UNSAVED}
 * @param name       모임 이름 (필수 입력)
 * @param inviteCode 자동 생성된 초대 코드. 전체에서 유일하다
 * @param createdAt  만들어진 시각
 * @param memberCount 참여자 수. 목록 조회 시 함께 계산해 담는다
 */
public record Group(long id,
                    String name,
                    String inviteCode,
                    LocalDateTime createdAt,
                    int memberCount) {

    /** 아직 DB 에 저장되지 않은 객체의 id */
    public static final long UNSAVED = -1L;

    /** 저장 전 상태의 새 모임을 만든다. */
    public static Group of(String name, String inviteCode) {
        return new Group(UNSAVED, name, inviteCode, LocalDateTime.now(), 0);
    }

    public boolean isSaved() {
        return id != UNSAVED;
    }
}
