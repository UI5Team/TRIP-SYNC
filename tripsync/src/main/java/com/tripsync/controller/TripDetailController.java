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
import com.tripsync.model.WishlistItem;
import com.tripsync.nav.AppContext;
import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Dialogs;
import com.tripsync.ui.EmptyState;
import com.tripsync.ui.MapPanel;
import com.tripsync.ui.MonthCalendar;
import com.tripsync.ui.TimelineRow;
import com.tripsync.ui.Toast;
import com.tripsync.util.Dates;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
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
    @FXML private StackPane mapHolder;

    @FXML private HBox wishTabBar;
    @FXML private HBox wishStrip;
    @FXML private Button manageWishlistButton;

    private final MonthCalendar calendar = new MonthCalendar();
    private final MapPanel map = new MapPanel();

    private Trip trip;
    private LocalDate selectedDate;
    private Long selectedScheduleId;
    private final Set<Category> activeFilters = EnumSet.noneOf(Category.class);

    @FXML
    private void initialize() {
        calendarHolder.getChildren().add(calendar);
        calendar.setOnDateSelected(this::onDateSelected);

        mapHolder.getChildren().add(map);

        addScheduleButton.setOnAction(e -> openEditor(null, null));
        manageWishlistButton.setOnAction(e -> openWishlist());

        buildViewModeBar();
        buildFilterBar();
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

        addScheduleButton.setDisable(false);
        manageWishlistButton.setDisable(false);
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
        }

        chip.setOnMouseClicked(e -> {
            if (category == null) {
                activeFilters.clear();
            } else if (!activeFilters.add(category)) {
                activeFilters.remove(category);
            }
            selectedScheduleId = null;
            buildFilterBar();
            refreshFiltered();
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
        refreshFiltered();
    }

    private void refreshFiltered() {
        renderTimeline();
        renderMap();
        renderWishStrip();
    }

    private boolean passesFilter(Category category) {
        return activeFilters.isEmpty() || activeFilters.contains(category);
    }

    private List<ScheduleItem> visibleSchedules() {
        List<ScheduleItem> filtered = new ArrayList<>();
        for (ScheduleItem item : SessionStore.schedulesOn(trip.id(), selectedDate)) {
            if (passesFilter(item.category())) {
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
                    .of("이 날짜에 일정이 없습니다", "아래 버튼으로 장소와 시간을 추가해 보세요.")
                    .secondary("샘플 일정 채우기", this::fillSample)
                    .build());
            return;
        }

        if (items.isEmpty()) {
            timelineBox.getChildren().add(EmptyState
                    .of("선택한 분류의 일정이 없습니다", "위쪽 분류 칩을 눌러 전체를 다시 볼 수 있습니다.")
                    .primary("전체 보기", () -> {
                        activeFilters.clear();
                        buildFilterBar();
                        refreshFiltered();
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
                    () -> openEditor(item.id(), null),
                    () -> deleteSchedule(item)));
        }
    }

    private void renderMap() {
        map.setItems(visibleSchedules());
    }

    private void selectSchedule(long scheduleId) {
        selectedScheduleId = scheduleId;
        renderTimeline();
    }

    private void renderWishStrip() {
        wishStrip.getChildren().clear();
        buildWishTabs();

        List<WishlistItem> all = SessionStore.wishesOf(trip.id());
        List<WishlistItem> items = new ArrayList<>();
        for (WishlistItem item : all) {
            if (passesFilter(item.category())) {
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            Label hint = new Label(all.isEmpty()
                    ? "후보 장소가 없습니다. ‘＋ 장소 추가’ 로 가고 싶은 곳을 모아 보세요."
                    : "선택한 분류에 해당하는 후보가 없습니다.");
            hint.getStyleClass().add("wish-empty");
            wishStrip.getChildren().add(hint);
            return;
        }

        for (WishlistItem item : items) {
            wishStrip.getChildren().add(wishCard(item));
        }
    }

    private void buildWishTabs() {
        wishTabBar.getChildren().clear();
        List<WishlistItem> all = SessionStore.wishesOf(trip.id());
        Label count = new Label("전체 " + all.size());
        count.getStyleClass().add("wish-count");
        wishTabBar.getChildren().add(count);
    }

    private Node wishCard(WishlistItem item) {
        Label name = new Label(item.placeName());
        name.getStyleClass().add("wish-card-title");
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMaxWidth(150);

        Label badge = new Label(item.category().label());
        badge.getStyleClass().addAll("category-badge", item.category().styleClass());

        Label note = new Label(item.hasNote() ? item.note() : " ");
        note.getStyleClass().add("wish-card-note");
        note.setWrapText(true);
        note.setMaxWidth(150);

        Button add = new Button("일정에 추가");
        add.getStyleClass().add("wish-card-button");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(e -> openEditor(null, item));

        VBox card = new VBox(6, name, badge, note);
        card.getStyleClass().add("wish-card");
        card.setPrefWidth(174);
        card.setMinWidth(174);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        card.getChildren().addAll(spacer, add);
        card.setAlignment(Pos.TOP_LEFT);
        return card;
    }

    private void fillSample() {
        SessionStore.fillSampleDay(trip.id(), selectedDate);
        SessionStore.fillSampleWishes(trip.id());
        Toast.success("샘플 일정과 후보 장소를 채웠습니다. 레이아웃 확인용 데이터입니다.");
        refreshAll();
    }

    private void openEditor(Long scheduleId, WishlistItem fromWish) {
        if (trip == null) {
            return;
        }
        Router.navigate(Route.SCHEDULE_EDIT,
                new ScheduleEditController.Request(trip.id(), selectedDate, scheduleId, fromWish));
    }

    private void openWishlist() {
        if (trip != null) {
            Router.navigate(Route.WISHLIST, trip.id());
        }
    }

    private void deleteSchedule(ScheduleItem item) {
        if (!Dialogs.confirm("일정 삭제",
                "'" + item.placeName() + "' 일정을 삭제합니다.",
                "이 작업은 되돌릴 수 없습니다.",
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

        addScheduleButton.setDisable(true);
        manageWishlistButton.setDisable(true);

        timelineBox.getChildren().clear();
        timelineBox.getChildren().add(EmptyState
                .of("여행 정보를 불러올 수 없습니다", "모임으로 돌아가 여행을 다시 선택해 주세요.")
                .primary("모임 목록으로", () -> Router.resetTo(Route.GROUP_LIST, null))
                .build());

        wishStrip.getChildren().clear();
        wishTabBar.getChildren().clear();
        map.setItems(List.of());
    }
}
