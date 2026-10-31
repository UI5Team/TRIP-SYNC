package com.tripsync.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.TripRepository;
import com.tripsync.model.Trip;
import com.tripsync.nav.AppContext;
import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.EmptyState;
import com.tripsync.ui.Toast;
import com.tripsync.util.Dates;

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
 * <p><b>[3단계]</b> 여행 정보와 날짜 탭은 저장된 여행에서 읽어온다.
 * 일정 CRUD 는 4단계, 위시리스트는 5단계에서 채운다.</p>
 */
public class TripDetailController implements RoutedController {

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

    private Trip trip;

    /** 날짜 탭에서 지금 선택된 날짜 */
    private LocalDate selectedDate;

    @FXML
    private void initialize() {
        addScheduleButton.setOnAction(e -> openScheduleEditor());
        openWishlistButton.setOnAction(e -> openWishlist());
        manageWishlistButton.setOnAction(e -> openWishlist());

        editScheduleButton.setOnAction(e -> openScheduleEditor());
        deleteScheduleButton.setOnAction(e -> Toast.warn("[4단계에서 구현] 일정 삭제"));

        // 선택된 일정이 없으면 수정/삭제는 누를 수 없어야 한다
        setScheduleActionsEnabled(false);
    }

    @Override
    public void onEnter(Object param) {
        Optional<Trip> loaded = loadTrip(param);

        if (loaded.isEmpty()) {
            showMissingTrip();
            return;
        }

        trip = loaded.get();
        AppContext.setCurrentTrip(trip.id(), trip.title());

        tripTitleLabel.setText(trip.title());
        tripPeriodLabel.setText(Dates.full(trip.startDate()) + " ~ " + Dates.full(trip.endDate())
                + "  ·  " + Dates.duration(trip.startDate(), trip.endDate()));

        setHeaderActionsEnabled(true);

        // 오늘이 여행 기간 안이면 오늘을, 아니면 첫날을 연다.
        // 여행 중에 앱을 켰을 때 날짜 탭을 다시 누르지 않아도 되게 하기 위해서다.
        LocalDate today = LocalDate.now();
        selectedDate = trip.covers(today) ? today : trip.startDate();

        renderDateTabs();
        renderScheduleList();
        renderScheduleDetail();
        renderWishlistSummary();
    }

    private Optional<Trip> loadTrip(Object param) {
        Long tripId = param instanceof Long id ? id : AppContext.currentTripId().orElse(null);
        if (tripId == null) {
            return Optional.empty();
        }
        try {
            return TripRepository.findById(tripId);
        } catch (DataAccessException e) {
            Toast.warn(e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 여행 기간의 날짜를 DAY 탭으로 깔아 준다.
     *
     * <p>기간이 길면 탭이 가로로 넘칠 수 있으므로 FXML 에서 스크롤되도록 감싸 두었다.</p>
     */
    private void renderDateTabs() {
        dateTabBar.getChildren().clear();

        for (int dayIndex = 0; dayIndex < trip.dayCount(); dayIndex++) {
            LocalDate date = trip.dateAt(dayIndex);
            int dayNumber = dayIndex + 1;

            Button tab = new Button("DAY " + dayNumber + "  " + Dates.shortDate(date));
            tab.getStyleClass().add(date.equals(selectedDate) ? "button-secondary" : "button-ghost");
            tab.setOnAction(e -> {
                selectedDate = date;
                renderDateTabs();
                renderScheduleList();
                renderScheduleDetail();
            });

            dateTabBar.getChildren().add(tab);
        }
    }

    /** 좌측 패널: 선택한 날짜의 일정 목록. [4단계에서 저장소 연결] */
    private void renderScheduleList() {
        scheduleListBox.getChildren().clear();

        List<String> schedules = List.of();
        scheduleCountLabel.setText(schedules.isEmpty() ? "" : schedules.size() + "개");

        if (schedules.isEmpty()) {
            scheduleListBox.getChildren().add(EmptyState
                    .of(Dates.shortDate(selectedDate) + " 일정이 없습니다",
                            "장소와 시간을 추가해 하루를 채워 보세요.")
                    .primary("일정 추가", this::openScheduleEditor)
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

    /** 우측 패널: 공동 위시리스트 요약. [5단계에서 저장소 연결] */
    private void renderWishlistSummary() {
        wishlistSummaryBox.getChildren().clear();

        List<String> wishes = List.of();
        wishlistCountLabel.setText(wishes.isEmpty() ? "" : wishes.size() + "곳");

        if (wishes.isEmpty()) {
            wishlistSummaryBox.getChildren().add(EmptyState
                    .of("후보 장소가 없습니다",
                            "가고 싶은 곳을 모아두고 일정에 등록할 수 있습니다.")
                    .primary("위시리스트 열기", this::openWishlist)
                    .build());
        }
    }

    /** 여행을 찾지 못한 경우 (삭제됐거나 번호가 잘못됨) */
    private void showMissingTrip() {
        trip = null;
        tripTitleLabel.setText("여행을 찾을 수 없습니다");
        tripPeriodLabel.setText("요청한 여행이 삭제되었거나 번호가 올바르지 않습니다.");
        setHeaderActionsEnabled(false);
        setScheduleActionsEnabled(false);

        dateTabBar.getChildren().clear();
        scheduleCountLabel.setText("");
        wishlistCountLabel.setText("");
        scheduleListBox.getChildren().clear();
        wishlistSummaryBox.getChildren().clear();

        scheduleDetailBox.getChildren().clear();
        scheduleDetailBox.getChildren().add(EmptyState
                .of("여행 정보를 불러올 수 없습니다",
                        "모임으로 돌아가 여행을 다시 선택해 주세요.")
                .primary("모임 목록으로", () -> Router.resetTo(Route.GROUP_LIST, null))
                .build());
    }

    private void openScheduleEditor() {
        if (trip != null) {
            Router.navigate(Route.SCHEDULE_EDIT, selectedDate);
        }
    }

    private void openWishlist() {
        if (trip != null) {
            Router.navigate(Route.WISHLIST, trip.id());
        }
    }

    private void setHeaderActionsEnabled(boolean enabled) {
        addScheduleButton.setDisable(!enabled);
        openWishlistButton.setDisable(!enabled);
        manageWishlistButton.setDisable(!enabled);
    }

    private void setScheduleActionsEnabled(boolean enabled) {
        editScheduleButton.setDisable(!enabled);
        deleteScheduleButton.setDisable(!enabled);
    }
}
