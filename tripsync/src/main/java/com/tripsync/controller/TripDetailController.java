package com.tripsync.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.EmptyState;
import com.tripsync.ui.Toast;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 화면 5 — 여행 상세. 이 프로그램에서 가장 오래 머무르는 핵심 화면이다.
 *
 * <p>설계서 8장에 따라 좌(일정 목록)·중(일정 상세)·우(위시리스트) 3분할로 배치한다.
 * 모바일처럼 화면을 넘겨 다니지 않고, 한 화면에서 비교하며 편집하게 하는 것이 목적이다.</p>
 *
 * <p><b>[1단계]</b> 레이아웃과 화면 이동만 완성한다.
 * 일정 CRUD 는 4단계, 위시리스트 연동은 5단계에서 채운다.</p>
 */
public class TripDetailController implements RoutedController {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("M/d");
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("yyyy. M. d. (E)");

    @FXML private Label tripTitleLabel;
    @FXML private Label tripPeriodLabel;
    @FXML private Button openWishlistButton;
    @FXML private Button addScheduleButton;
    @FXML private HBox dateTabBar;

    @FXML private Label scheduleCountLabel;
    @FXML private VBox scheduleListBox;

    @FXML private Button editScheduleButton;
    @FXML private Button deleteScheduleButton;
    @FXML private VBox scheduleDetailBox;

    @FXML private Label wishlistCountLabel;
    @FXML private Button manageWishlistButton;
    @FXML private VBox wishlistSummaryBox;

    private String tripTitle = "여행";

    /** [1단계] 샘플 기간 — 3단계에서 저장된 여행의 실제 기간으로 바뀐다 */
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate = LocalDate.now().plusDays(3);

    /** 현재 선택된 날짜 */
    private LocalDate selectedDate;

    @FXML
    private void initialize() {
        addScheduleButton.setOnAction(e -> Router.navigate(Route.SCHEDULE_EDIT, selectedDate));
        openWishlistButton.setOnAction(e -> Router.navigate(Route.WISHLIST, tripTitle));
        manageWishlistButton.setOnAction(e -> Router.navigate(Route.WISHLIST, tripTitle));

        editScheduleButton.setOnAction(e -> Router.navigate(Route.SCHEDULE_EDIT, selectedDate));
        deleteScheduleButton.setOnAction(e -> Toast.warn("[4단계에서 구현] 일정 삭제"));

        // 선택된 일정이 없으면 수정/삭제는 누를 수 없어야 한다
        setScheduleActionsEnabled(false);
    }

    @Override
    public void onEnter(Object param) {
        if (param instanceof String title && !title.isBlank()) {
            tripTitle = title;
        }

        tripTitleLabel.setText(tripTitle);
        tripPeriodLabel.setText(startDate.format(FULL_DATE) + " ~ " + endDate.format(FULL_DATE));

        selectedDate = startDate;
        renderDateTabs();
        renderScheduleList();
        renderScheduleDetail();
        renderWishlistSummary();
    }

    /** 여행 기간의 날짜를 버튼으로 깔아 날짜 선택을 만든다. */
    private void renderDateTabs() {
        dateTabBar.getChildren().clear();

        LocalDate date = startDate;
        int dayNumber = 1;
        while (!date.isAfter(endDate)) {
            LocalDate thisDate = date;
            int thisDayNumber = dayNumber;

            Button tab = new Button("DAY " + thisDayNumber + "  " + thisDate.format(DAY_LABEL));
            tab.getStyleClass().add(thisDate.equals(selectedDate) ? "button-secondary" : "button-ghost");
            tab.setOnAction(e -> {
                selectedDate = thisDate;
                renderDateTabs();
                renderScheduleList();
                renderScheduleDetail();
            });

            dateTabBar.getChildren().add(tab);
            date = date.plusDays(1);
            dayNumber++;
        }
    }

    /** 좌측 패널: 선택한 날짜의 일정 목록 */
    private void renderScheduleList() {
        scheduleListBox.getChildren().clear();

        // [4단계에서 교체] 저장소에서 selectedDate 의 일정을 조회한다
        List<String> schedules = List.of();
        scheduleCountLabel.setText(schedules.isEmpty() ? "" : schedules.size() + "개");

        if (schedules.isEmpty()) {
            scheduleListBox.getChildren().add(EmptyState
                    .of("이 날짜에 일정이 없습니다",
                            "장소와 시간을 추가해 하루를 채워 보세요.")
                    .primary("일정 추가", () -> Router.navigate(Route.SCHEDULE_EDIT, selectedDate))
                    .build());
        }
    }

    /** 중앙 패널: 선택한 일정의 상세 */
    private void renderScheduleDetail() {
        scheduleDetailBox.getChildren().clear();
        setScheduleActionsEnabled(false);

        scheduleDetailBox.getChildren().add(EmptyState
                .of("선택된 일정이 없습니다",
                        "왼쪽 목록에서 일정을 선택하면 장소와 메모가 여기에 표시됩니다.")
                .build());
    }

    /** 우측 패널: 공동 위시리스트 요약 */
    private void renderWishlistSummary() {
        wishlistSummaryBox.getChildren().clear();

        // [5단계에서 교체] 저장소에서 이 여행의 위시리스트를 조회한다
        List<String> wishes = List.of();
        wishlistCountLabel.setText(wishes.isEmpty() ? "" : wishes.size() + "곳");

        if (wishes.isEmpty()) {
            wishlistSummaryBox.getChildren().add(EmptyState
                    .of("후보 장소가 없습니다",
                            "가고 싶은 곳을 모아두고 일정에 등록할 수 있습니다.")
                    .primary("위시리스트 열기", () -> Router.navigate(Route.WISHLIST, tripTitle))
                    .build());
        }
    }

    private void setScheduleActionsEnabled(boolean enabled) {
        editScheduleButton.setDisable(!enabled);
        deleteScheduleButton.setDisable(!enabled);
    }
}
