package com.tripsync.nav;

public enum Route {

    GROUP_LIST("group-list.fxml", "모임 목록", false),

    GROUP_CREATE("group-create.fxml", "모임 만들기", true),

    GROUP_DETAIL("group-detail.fxml", "모임 상세", true),

    TRIP_CREATE("trip-create.fxml", "여행 만들기", true),

    TRIP_DETAIL("trip-detail.fxml", "여행 상세", true),

    SCHEDULE_EDIT("schedule-edit.fxml", "일정 편집", true),

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

    public String fxmlPath() {
        return VIEW_DIR + fileName;
    }

    public String title() {
        return title;
    }

    public boolean isBackEnabled() {
        return backEnabled;
    }
}
