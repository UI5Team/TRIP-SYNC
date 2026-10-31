package com.tripsync.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.SessionStore;
import com.tripsync.data.TripRepository;
import com.tripsync.model.Category;
import com.tripsync.model.ScheduleItem;
import com.tripsync.model.Trip;
import com.tripsync.nav.AppContext;
import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Dialogs;
import com.tripsync.ui.EmptyState;
import com.tripsync.ui.MonthCalendar;
import com.tripsync.ui.TimelineRow;
import com.tripsync.ui.Toast;
import com.tripsync.util.Dates;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class TripDetailController implements RoutedController {

    private static final Category[] FILTERS = {
            Category.FOOD, Category.SIGHT, Category.STAY, Category.MOVE
    };

    @FXML private HBox viewModeBar;
    @FXML private VBox calendarHolder;
    @FXML private Label dayHeaderLabel;
    @FXML private Label scheduleCountLabel;
    @FXML private VBox timelineBox;
    @FXML private Button addScheduleButton;

    @FXML private Label tripTitleLabel;
    @FXML private Label tripPeriodLabel;
    @FXML private HBox filterBar;
    @FXML private VBox detailBox;
    @FXML private Label detailHintLabel;
    @FXML private Button openWishlistButton;
    @FXML private Button editScheduleButton;
    @FXML private Button deleteScheduleButton;

    private final MonthCalendar calendar = new MonthCalendar();

    private Trip trip;
    private LocalDate selectedDate;
    private Long selectedScheduleId;
    private final Set<Category> activeFilters = EnumSet.noneOf(Category.class);

    @FXML
    private void initialize() {
        calendarHolder.getChildren().add(calendar);
        calendar.setOnDateSelected(this::onDateSelected);

        addScheduleButton.setOnAction(e -> openEditor(null));
        openWishlistButton.setOnAction(e -> openWishlist());
        editScheduleButton.setOnAction(e -> openEditor(selectedScheduleId));
        deleteScheduleButton.setOnAction(e -> deleteSelected());

        buildViewModeBar();
        buildFilterBar();
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
        tripPeriodLabel.setText(Dates.period(trip.startDate(), trip.endDate())
                + "  ·  " + Dates.duration(trip.startDate(), trip.endDate()));

        LocalDate today = LocalDate.now();
        selectedDate = trip.covers(today) ? today : trip.startDate();
        selectedScheduleId = null;

        calendar.setRange(trip.startDate(), trip.endDate());
        calendar.setSelected(selectedDate);

        setHeaderActionsEnabled(true);
        refreshAll();
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

    private void buildViewModeBar() {
        viewModeBar.getChildren().clear();
        addModeButton("일", MonthCalendar.Mode.DAY);
        addModeButton("주", MonthCalendar.Mode.WEEK);
        addModeButton("월", MonthCalendar.Mode.MONTH);
    }

    private void addModeButton(String label, MonthCalendar.Mode mode) {
        Button button = new Button(label);
        button.getStyleClass().add("segment");
        if (calendar.getMode() == mode) {
            button.getStyleClass().add("segment-active");
        }
        button.setOnAction(e -> {
            calendar.setMode(mode);
            buildViewModeBar();
        });
        viewModeBar.getChildren().add(button);
    }

    private void buildFilterBar() {
        filterBar.getChildren().clear();
        filterBar.getChildren().add(filterChip("전체", null));
        for (Category category : FILTERS) {
            filterBar.getChildren().add(filterChip(category.icon() + " " + category.label(), category));
        }
    }

    private Node filterChip(String text, Category category) {
        boolean active = category == null ? activeFilters.isEmpty() : activeFilters.contains(category);

        Label label = new Label(text);
        label.getStyleClass().add("filter-chip-label");

        StackPane chip = new StackPane(label);
        chip.getStyleClass().add("filter-chip");
        if (active) {
            chip.getStyleClass().add("filter-chip-active");
            if (category != null) {
                chip.getStyleClass().add(category.styleClass());
            }
        }

        chip.setOnMouseClicked(e -> {
            if (category == null) {
                activeFilters.clear();
            } else if (!activeFilters.add(category)) {
                activeFilters.remove(category);
            }
            buildFilterBar();
            renderTimeline();
        });
        return chip;
    }

    private void onDateSelected(LocalDate date) {
        selectedDate = date;
        selectedScheduleId = null;
        refreshAll();
    }

    private void refreshAll() {
        calendar.setMarkedDates(SessionStore.scheduledDates(trip.id()));
        dayHeaderLabel.setText(Dates.full(selectedDate));
        renderTimeline();
        renderDetail();
    }

    private List<ScheduleItem> visibleSchedules() {
        List<ScheduleItem> items = SessionStore.schedulesOn(trip.id(), selectedDate);
        if (activeFilters.isEmpty()) {
            return items;
        }
        List<ScheduleItem> filtered = new ArrayList<>();
        for (ScheduleItem item : items) {
            if (activeFilters.contains(item.category())) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private void renderTimeline() {
        timelineBox.getChildren().clear();

        List<ScheduleItem> all = SessionStore.schedulesOn(trip.id(), selectedDate);
        List<ScheduleItem> items = visibleSchedules();
        scheduleCountLabel.setText(all.isEmpty() ? "" : all.size() + "개");

        if (all.isEmpty()) {
            timelineBox.getChildren().add(EmptyState
                    .of("이 날짜에 일정이 없습니다", "장소와 시간을 추가해 하루를 채워 보세요.")
                    .primary("일정 추가", () -> openEditor(null))
                    .secondary("샘플 일정 채우기", this::fillSample)
                    .build());
            return;
        }

        if (items.isEmpty()) {
            timelineBox.getChildren().add(EmptyState
                    .of("선택한 분류의 일정이 없습니다", "위쪽 분류 칩을 다시 눌러 전체를 볼 수 있습니다.")
                    .primary("전체 보기", () -> {
                        activeFilters.clear();
                        buildFilterBar();
                        renderTimeline();
                    })
                    .build());
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            ScheduleItem item = items.get(i);
            timelineBox.getChildren().add(new TimelineRow(
                    item,
                    Long.valueOf(item.id()).equals(selectedScheduleId),
                    i == 0,
                    i == items.size() - 1,
                    () -> selectSchedule(item.id()),
                    () -> openEditor(item.id()),
                    () -> deleteSchedule(item)));
        }
    }

    private void selectSchedule(long scheduleId) {
        selectedScheduleId = scheduleId;
        renderTimeline();
        renderDetail();
    }

    private void renderDetail() {
        detailBox.getChildren().clear();

        Optional<ScheduleItem> selected = selectedScheduleId == null
                ? Optional.empty()
                : SessionStore.scheduleById(trip.id(), selectedScheduleId);

        if (selected.isEmpty()) {
            setScheduleActionsEnabled(false);
            detailHintLabel.setText("왼쪽 타임라인에서 일정을 선택하세요.");
            detailBox.getChildren().add(EmptyState
                    .of("선택된 일정이 없습니다",
                            "왼쪽 목록에서 일정을 고르면 장소와 메모가 여기에 표시됩니다.")
                    .build());
            return;
        }

        ScheduleItem item = selected.get();
        setScheduleActionsEnabled(true);
        detailHintLabel.setText(Dates.full(item.date()));

        Label glyph = new Label(item.category().icon());
        glyph.getStyleClass().add("detail-icon-glyph");
        StackPane icon = new StackPane(glyph);
        icon.getStyleClass().addAll("detail-icon", item.category().styleClass());
        icon.setMinSize(52, 52);
        icon.setPrefSize(52, 52);
        icon.setMaxSize(52, 52);

        Label title = new Label(item.placeName());
        title.getStyleClass().add("detail-title");
        title.setWrapText(true);

        Label when = new Label(item.hasTime()
                ? Dates.time(item.time()) + " 시작"
                : "시간 미정");
        when.getStyleClass().add("detail-when");

        VBox titleBox = new VBox(3, title, when);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        HBox head = new HBox(14, icon, titleBox, categoryBadge(item.category()));
        head.setAlignment(Pos.CENTER_LEFT);

        detailBox.getChildren().add(head);
        detailBox.getChildren().add(detailField("메모",
                item.hasMemo() ? item.memo() : "메모가 없습니다."));
        detailBox.getChildren().add(detailField("분류", item.category().label()));
    }

    private Node categoryBadge(Category category) {
        Label badge = new Label(category.label());
        badge.getStyleClass().addAll("category-badge", category.styleClass());
        return badge;
    }

    private Node detailField(String label, String value) {
        Label name = new Label(label);
        name.getStyleClass().add("field-label");

        Label content = new Label(value);
        content.getStyleClass().add("detail-field-value");
        content.setWrapText(true);

        VBox box = new VBox(5, name, content);
        box.getStyleClass().add("detail-field");
        return box;
    }

    private void fillSample() {
        SessionStore.fillSampleDay(trip.id(), selectedDate);
        Toast.success("샘플 일정 5개를 채웠습니다. 레이아웃 확인용 데이터입니다.");
        refreshAll();
    }

    private void openEditor(Long scheduleId) {
        if (trip == null) {
            return;
        }
        Router.navigate(Route.SCHEDULE_EDIT,
                new ScheduleEditController.Request(trip.id(), selectedDate, scheduleId));
    }

    private void openWishlist() {
        if (trip != null) {
            Router.navigate(Route.WISHLIST, trip.id());
        }
    }

    private void deleteSelected() {
        if (selectedScheduleId == null) {
            return;
        }
        SessionStore.scheduleById(trip.id(), selectedScheduleId).ifPresent(this::deleteSchedule);
    }

    private void deleteSchedule(ScheduleItem item) {
        if (!Dialogs.confirm("일정 삭제",
                "'" + item.placeName() + "' 일정을 삭제합니다. 이 작업은 되돌릴 수 없습니다.",
                "삭제")) {
            return;
        }
        SessionStore.deleteSchedule(trip.id(), item.id());
        if (Long.valueOf(item.id()).equals(selectedScheduleId)) {
            selectedScheduleId = null;
        }
        Toast.success("일정을 삭제했습니다.");
        refreshAll();
    }

    private void showMissingTrip() {
        trip = null;
        tripTitleLabel.setText("여행을 찾을 수 없습니다");
        tripPeriodLabel.setText("요청한 여행이 삭제되었거나 번호가 올바르지 않습니다.");
        dayHeaderLabel.setText("");
        scheduleCountLabel.setText("");
        detailHintLabel.setText("");

        setHeaderActionsEnabled(false);
        setScheduleActionsEnabled(false);

        timelineBox.getChildren().clear();
        detailBox.getChildren().clear();
        detailBox.getChildren().add(EmptyState
                .of("여행 정보를 불러올 수 없습니다", "모임으로 돌아가 여행을 다시 선택해 주세요.")
                .primary("모임 목록으로", () -> Router.resetTo(Route.GROUP_LIST, null))
                .build());
    }

    private void setHeaderActionsEnabled(boolean enabled) {
        addScheduleButton.setDisable(!enabled);
        openWishlistButton.setDisable(!enabled);
    }

    private void setScheduleActionsEnabled(boolean enabled) {
        editScheduleButton.setDisable(!enabled);
        deleteScheduleButton.setDisable(!enabled);
    }
}
