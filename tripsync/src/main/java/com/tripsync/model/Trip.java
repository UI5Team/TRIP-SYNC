package com.tripsync.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 여행 하나.
 *
 * <p>설계서 9장의 Trip(tripId, groupId, title, startDate, endDate) 에 대응한다.
 * 모임 하나에 여행을 여러 개 만들 수 있지만, 편집은 한 번에 한 여행씩 한다.</p>
 *
 * @param id            DB 가 매긴 번호. 저장 전이면 {@link #UNSAVED}
 * @param groupId       이 여행이 속한 모임
 * @param title         여행명 (필수)
 * @param startDate     시작일 (필수)
 * @param endDate       종료일 (필수, 시작일보다 빠를 수 없다)
 * @param updatedAt     마지막으로 수정된 시각
 * @param scheduleCount 등록된 일정 개수. 목록 조회 시 함께 계산해 담는다
 */
public record Trip(long id,
                   long groupId,
                   String title,
                   LocalDate startDate,
                   LocalDate endDate,
                   LocalDateTime updatedAt,
                   int scheduleCount) {

    /** 아직 DB 에 저장되지 않은 객체의 id */
    public static final long UNSAVED = -1L;

    public boolean isSaved() {
        return id != UNSAVED;
    }

    /** 여행 기간에 포함된 날짜 수. 당일치기면 1 이다. */
    public int dayCount() {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }

    /** 주어진 날짜가 이 여행 기간 안에 있는지 */
    public boolean covers(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 여행 기간의 N 번째 날짜. {@code dayIndex} 는 0 부터 시작한다.
     * 여행 상세 화면의 DAY 탭을 만들 때 쓴다.
     */
    public LocalDate dateAt(int dayIndex) {
        return startDate.plusDays(dayIndex);
    }
}
