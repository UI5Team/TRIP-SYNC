package com.tripsync.ui;

import java.util.List;
import java.util.Optional;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.GroupRepository;
import com.tripsync.data.TripRepository;
import com.tripsync.data.UserRepository;
import com.tripsync.model.Group;
import com.tripsync.model.Trip;
import com.tripsync.nav.AppBar;
import com.tripsync.nav.AppContext;
import com.tripsync.nav.Route;
import com.tripsync.nav.Router;
import com.tripsync.util.Dates;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class Sidebar extends VBox {

    private static final double CONTENT_WIDTH = AppBar.BRAND_WIDTH - 48;

    private final VBox navBox = new VBox(2);
    private final VBox listBox = new VBox(3);
    private final Label listTitle = new Label();
    private final Button listAddButton = new Button("＋");
    private final Button listBackButton = new Button("‹ 모든 모임");
    private final VBox listHeader = new VBox(4);

    public Sidebar() {
        getStyleClass().add("sidebar");
        setPrefWidth(AppBar.BRAND_WIDTH);
        setMinWidth(AppBar.BRAND_WIDTH);
        setMaxWidth(AppBar.BRAND_WIDTH);
        setFillWidth(true);

        navBox.getStyleClass().add("sidebar-nav");
        navBox.setFillWidth(true);
        listBox.getStyleClass().add("sidebar-group-list");
        listBox.setFillWidth(true);

        getChildren().addAll(navBox, buildSeparator(), buildListHeader(), buildListScroller());
        refresh();
    }

    public final void refresh() {
        renderNav();
        renderList();
    }

    private Region buildSeparator() {
        Separator separator = new Separator();
        separator.getStyleClass().add("sidebar-separator");
        return separator;
    }

    private Region buildListHeader() {
        listTitle.getStyleClass().add("sidebar-section-title");
        listTitle.setTextOverrun(OverrunStyle.ELLIPSIS);

        listAddButton.getStyleClass().add("sidebar-add-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(6, listTitle, spacer, listAddButton);
        topRow.setAlignment(Pos.CENTER_LEFT);

        listBackButton.getStyleClass().add("sidebar-back-link");
        listBackButton.setOnAction(e -> {
            AppContext.clear();
            Router.resetTo(Route.GROUP_LIST, null);
        });

        listHeader.getChildren().addAll(topRow, listBackButton);
        listHeader.getStyleClass().add("sidebar-section-header");
        return listHeader;
    }

    private Region buildListScroller() {
        ScrollPane scroller = new ScrollPane(listBox);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.getStyleClass().add("sidebar-scroll");
        VBox.setVgrow(scroller, Priority.ALWAYS);
        return scroller;
    }

    private void renderNav() {
        navBox.getChildren().clear();
        Route active = Router.currentRoute();

        Optional<Long> groupId = AppContext.currentGroupId();
        Optional<Long> tripId = AppContext.currentTripId();

        navBox.getChildren().add(navItem("⌂", "홈",
                active == Route.GROUP_LIST || active == Route.GROUP_CREATE, true,
                () -> Router.resetTo(Route.GROUP_LIST, null),
                null));

        navBox.getChildren().add(navItem("🧳", "내 여행",
                active == Route.GROUP_DETAIL || active == Route.TRIP_CREATE,
                groupId.isPresent(),
                () -> groupId.ifPresent(id -> Router.navigate(Route.GROUP_DETAIL, id)),
                "모임을 먼저 선택하세요"));

        navBox.getChildren().add(navItem("📅", "캘린더",
                active == Route.TRIP_DETAIL || active == Route.SCHEDULE_EDIT,
                tripId.isPresent(),
                () -> tripId.ifPresent(id -> Router.navigate(Route.TRIP_DETAIL, id)),
                "여행을 먼저 선택하세요"));

        navBox.getChildren().add(navItem("🗺", "지도",
                false,
                tripId.isPresent(),
                () -> tripId.ifPresent(id -> Router.navigate(Route.TRIP_DETAIL, id)),
                "여행을 먼저 선택하세요"));

        navBox.getChildren().add(navItem("♡", "위시리스트",
                active == Route.WISHLIST,
                tripId.isPresent(),
                () -> tripId.ifPresent(id -> Router.navigate(Route.WISHLIST, id)),
                "여행을 먼저 선택하세요"));

        navBox.getChildren().add(navItem("👥", "친구",
                active == Route.FRIENDS, true,
                () -> Router.navigate(Route.FRIENDS, "친구"),
                null));

        navBox.getChildren().add(navItem("⚙", "설정",
                active == Route.SETTINGS, true,
                () -> Router.navigate(Route.SETTINGS, "설정"),
                null));
    }

    private Region navItem(String icon, String text, boolean active, boolean enabled,
                           Runnable action, String disabledHint) {
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("nav-icon");

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("nav-text");
        textLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        HBox row = new HBox(12, iconLabel, textLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("nav-item");
        row.setMaxWidth(Double.MAX_VALUE);

        if (active) {
            row.getStyleClass().add("nav-item-active");
        }
        if (enabled) {
            row.setOnMouseClicked(e -> action.run());
        } else {
            row.getStyleClass().add("nav-item-disabled");
            if (disabledHint != null) {
                Tooltip.install(row, new Tooltip(disabledHint));
            }
        }
        return row;
    }

    private void renderList() {
        listBox.getChildren().clear();

        Optional<Long> openGroupId = AppContext.currentGroupId();
        boolean showingTrips = openGroupId.isPresent();

        listTitle.setText(showingTrips ? "내 여행 목록" : "내 모임");
        listTitle.setMaxWidth(CONTENT_WIDTH - 34);

        listBackButton.setVisible(showingTrips);
        listBackButton.setManaged(showingTrips);

        if (showingTrips) {
            long groupId = openGroupId.get();
            listAddButton.setTooltip(new Tooltip("여행 추가"));
            listAddButton.setOnAction(e -> Router.navigate(Route.TRIP_CREATE, groupId));
            renderTrips(groupId);
        } else {
            listAddButton.setTooltip(new Tooltip("모임 추가"));
            listAddButton.setOnAction(e -> Router.navigate(Route.GROUP_CREATE));
            renderGroups();
        }
    }

    private void renderGroups() {
        List<Group> groups;
        try {
            groups = GroupRepository.findAllForUser(UserRepository.currentUserId());
        } catch (DataAccessException | IllegalStateException e) {
            listBox.getChildren().add(hintLabel("목록을 불러오지 못했습니다"));
            return;
        }

        if (groups.isEmpty()) {
            listBox.getChildren().add(hintLabel("참여 중인 모임이 없습니다"));
            return;
        }

        for (Group group : groups) {
            listBox.getChildren().add(listItem(
                    group.name(),
                    "참여자 " + group.memberCount() + "명",
                    group.inviteCode(),
                    false,
                    () -> Router.navigate(Route.GROUP_DETAIL, group.id())));
        }
    }

    private void renderTrips(long groupId) {
        List<Trip> trips;
        try {
            trips = TripRepository.findAllByGroup(groupId);
        } catch (DataAccessException e) {
            listBox.getChildren().add(hintLabel("여행을 불러오지 못했습니다"));
            return;
        }

        if (trips.isEmpty()) {
            listBox.getChildren().add(hintLabel("아직 여행이 없습니다"));
            return;
        }

        Long activeTripId = AppContext.currentTripId().orElse(null);
        for (Trip trip : trips) {
            listBox.getChildren().add(listItem(
                    trip.title(),
                    Dates.period(trip.startDate(), trip.endDate()),
                    null,
                    Long.valueOf(trip.id()).equals(activeTripId),
                    () -> Router.navigate(Route.TRIP_DETAIL, trip.id())));
        }
    }

    private Region listItem(String name, String meta, String tag, boolean active, Runnable onOpen) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("sidebar-group-name");
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLabel.setMaxWidth(CONTENT_WIDTH);

        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("sidebar-group-meta");
        metaLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        metaLabel.setMaxWidth(CONTENT_WIDTH);

        VBox box = new VBox(2, nameLabel, metaLabel);
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        box.getStyleClass().add("sidebar-group-item");

        if (tag != null) {
            Label tagLabel = new Label(tag);
            tagLabel.getStyleClass().add("sidebar-group-tag");
            box.getChildren().add(tagLabel);
        }
        if (active) {
            box.getStyleClass().add("sidebar-group-item-active");
        }

        Tooltip.install(box, new Tooltip(name + "\n" + meta));
        box.setOnMouseClicked(e -> onOpen.run());
        return box;
    }

    private Label hintLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("sidebar-hint");
        label.setWrapText(true);
        label.setMaxWidth(CONTENT_WIDTH);
        return label;
    }
}
