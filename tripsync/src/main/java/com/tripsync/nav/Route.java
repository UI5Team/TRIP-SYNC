package com.tripsync.nav;

/**
 * 앱 안의 모든 화면 목록.
 *
 * <p>설계서 6장 "정보 구조 및 화면 이동 흐름"의 7개 화면을 그대로 옮긴 것이다.
 * 화면을 추가할 때는 여기에 상수를 하나 더 넣고 같은 이름의 FXML 파일을 만들면 된다.</p>
 */
public enum Route {

    /** 화면 1 — 시작 화면 및 모임 목록 */
    GROUP_LIST("group-list.fxml", "모임 목록", false),

    /** 화면 2 — 모임 생성 */
    GROUP_CREATE("group-create.fxml", "모임 만들기", true),

    /** 화면 3 — 모임 상세 및 여행 목록 */
    GROUP_DETAIL("group-detail.fxml", "모임 상세", true),

    /** 화면 4 — 여행 생성 */
    TRIP_CREATE("trip-create.fxml", "여행 만들기", true),

    /** 화면 5 — 여행 상세(일정 중심) */
    TRIP_DETAIL("trip-detail.fxml", "여행 상세", true),

    /** 화면 6 — 일정 추가/수정 */
    SCHEDULE_EDIT("schedule-edit.fxml", "일정 편집", true),

    /** 화면 7 — 공동 위시리스트 */
    WISHLIST("wishlist.fxml", "공동 위시리스트", true);

    private static final String VIEW_DIR = "/com/tripsync/view/";

    private final String fileName;
    private final String title;
    private final boolean backEnabled;

    Route(String fileName, String title, boolean backEnabled) {
        this.fileName = fileName;
        this.title = title;
        this.backEnabled = backEnabled;
    }

    /** 클래스패스 기준 FXML 경로 */
    public String fxmlPath() {
        return VIEW_DIR + fileName;
    }

    /** 상단 바에 표시할 화면 이름 */
    public String title() {
        return title;
    }

    /** 이 화면에서 '뒤로' 버튼을 보여줄지 여부 */
    public boolean isBackEnabled() {
        return backEnabled;
    }
}
