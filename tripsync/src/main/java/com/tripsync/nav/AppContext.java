package com.tripsync.nav;

import java.util.Optional;

/**
 * 지금 사용자가 어느 모임·어느 여행을 보고 있는지 기억하는 곳.
 *
 * <p>사이드바의 '내 모임' 같은 항목은 현재 문맥을 알아야 한다. 그렇다고 화면마다
 * 사이드바를 직접 호출하게 두면 화면과 사이드바가 서로 얽힌다.
 * 화면은 자기가 연 대상을 여기에 적어두기만 하고, 사이드바는 여기만 읽는다.</p>
 */
public final class AppContext {

    private static Long currentGroupId;
    private static String currentGroupName;

    private static Long currentTripId;
    private static String currentTripTitle;

    private AppContext() {
    }

    /** 모임 상세 화면이 열릴 때 호출한다. */
    public static void setCurrentGroup(long groupId, String groupName) {
        currentGroupId = groupId;
        currentGroupName = groupName;
        // 다른 모임으로 옮겼다면 이전 모임의 여행 문맥은 더 이상 유효하지 않다
        currentTripId = null;
        currentTripTitle = null;
    }

    /** 여행 상세 화면이 열릴 때 호출한다. [3단계부터 사용] */
    public static void setCurrentTrip(long tripId, String title) {
        currentTripId = tripId;
        currentTripTitle = title;
    }

    public static Optional<Long> currentGroupId() {
        return Optional.ofNullable(currentGroupId);
    }

    public static Optional<String> currentGroupName() {
        return Optional.ofNullable(currentGroupName);
    }

    public static Optional<Long> currentTripId() {
        return Optional.ofNullable(currentTripId);
    }

    public static Optional<String> currentTripTitle() {
        return Optional.ofNullable(currentTripTitle);
    }

    /** 프로필을 바꾸면 이전 사용자의 문맥은 버린다. */
    public static void clear() {
        currentGroupId = null;
        currentGroupName = null;
        currentTripId = null;
        currentTripTitle = null;
    }
}
