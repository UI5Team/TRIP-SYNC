package com.tripsync.ui;

import java.util.List;
import java.util.Optional;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.GroupRepository;
import com.tripsync.data.TripRepository;
import com.tripsync.data.UserRepository;
import com.tripsync.model.AppUser;
import com.tripsync.model.Group;
import com.tripsync.model.Trip;
import com.tripsync.nav.AppContext;
import com.tripsync.nav.Route;
import com.tripsync.nav.Router;
import com.tripsync.util.Dates;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 왼쪽에 항상 붙어 있는 내비게이션 사이드바.
 *
 * <p>설계서의 참고 디자인을 따라, 넓은 데스크톱 화면의 왼쪽 띠를 길잡이로 쓴다.
 * 어느 화면에 있든 모임 목록과 내가 속한 모임들이 한눈에 보이므로
 * 뒤로가기를 여러 번 누르지 않고 바로 건너뛸 수 있다.</p>
 *
 * <p>참고 디자인의 검색창과 '친구' 메뉴는 설계서 4.2 의 제외 기능이라 넣지 않았다.</p>
 */
public class Sidebar extends VBox {

    private final VBox navBox = new VBox(2);
    private final VBox groupListBox = new VBox(2);
    private final MenuButton userButton = new MenuButton();

    /** 목록 구역의 제목. 모임을 열면 그 모임의 여행 목록으로 바뀐다. */
    private final Label listTitle = new Label();

    /** 여행 목록을 보고 있을 때만 보이는 '모든 모임' 되돌리기 버튼 */
    private final Button listBackButton = new Button("모든 모임");

    /** 목록 구역의 '추가' 버튼. 문맥에 따라 모임 추가 / 여행 추가로 바뀐다. */
    private final Button listAddButton = new Button();

    public Sidebar() {
        getStyleClass().add("sidebar");
        setPrefWidth(236);
        setMinWidth(236);

        getChildren().addAll(
                buildBrand(),
                navBox,
                buildSeparator(),
                buildGroupSectionHeader(),
                buildGroupScroller(),
                buildUserArea());

        refresh();
    }

    /** 화면이 바뀌거나 데이터가 바뀌면 다시 그린다. 라우터가 이동할 때마다 호출한다. */
    public final void refresh() {
        renderNav();
        renderGroups();
        renderUser();
    }

    // ----- 구성 요소 -----

    private Region buildBrand() {
        Label mark = new Label("✈");
        mark.getStyleClass().add("brand-mark");

        Label name = new Label("TripSync");
        name.getStyleClass().add("brand-name");

        Label tagline = new Label("Travel Planner");
        tagline.getStyleClass().add("brand-tagline");

        HBox row = new HBox(9, mark, name, tagline);
        row.setAlignment(Pos.BOTTOM_LEFT);
        row.getStyleClass().add("sidebar-brand");
        return row;
    }

    private Region buildSeparator() {
        Separator separator = new Separator();
        separator.getStyleClass().add("sidebar-separator");
        return separator;
    }

    /**
     * 목록 구역의 머리말.
     *
     * <p>모임을 열지 않았으면 '내 모임', 열었으면 그 모임의 '여행' 목록을 보여준다.
     * 참고 디자인의 '내 여행 목록' 자리를 문맥에 따라 바꿔 쓰는 셈이다.
     * 사이드바에 목록을 두 개 쌓으면 세로가 금방 모자라기 때문이다.</p>
     */
    private Region buildGroupSectionHeader() {
        listTitle.getStyleClass().add("sidebar-section-title");

        listBackButton.getStyleClass().add("sidebar-add-button");
        listBackButton.setOnAction(e -> {
            AppContext.clear();
            Router.resetTo(Route.GROUP_LIST, null);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        listAddButton.getStyleClass().add("sidebar-add-button");

        HBox row = new HBox(6, listTitle, listBackButton, spacer, listAddButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("sidebar-section-header");
        return row;
    }

    private Region buildGroupScroller() {
        ScrollPane scroller = new ScrollPane(groupListBox);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.getStyleClass().add("sidebar-scroll");
        VBox.setVgrow(scroller, Priority.ALWAYS);
        groupListBox.getStyleClass().add("sidebar-group-list");
        return scroller;
    }

    private Region buildUserArea() {
        userButton.getStyleClass().add("user-chip");
        userButton.setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox(userButton);
        box.getStyleClass().add("sidebar-user-area");
        return box;
    }

    // ----- 그리기 -----

    private void renderNav() {
        navBox.getChildren().clear();
        navBox.getStyleClass().add("sidebar-nav");

        Route active = Router.currentRoute();

        navBox.getChildren().add(navItem("홈",
                "모임 목록",
                active == Route.GROUP_LIST,
                true,
                () -> Router.resetTo(Route.GROUP_LIST, null)));

        // 모임 상세와 여행 상세는 해당 대상을 연 적이 있을 때만 의미가 있다
        Optional<Long> groupId = AppContext.currentGroupId();
        navBox.getChildren().add(navItem("＃",
                AppContext.currentGroupName().orElse("선택한 모임 없음"),
                active == Route.GROUP_DETAIL,
                groupId.isPresent(),
                () -> groupId.ifPresent(id -> Router.navigate(Route.GROUP_DETAIL, id))));

        Optional<Long> tripId = AppContext.currentTripId();
        navBox.getChildren().add(navItem("🗓",
                AppContext.currentTripTitle().orElse("선택한 여행 없음"),
                active == Route.TRIP_DETAIL,
                tripId.isPresent(),
                () -> tripId.ifPresent(id -> Router.navigate(Route.TRIP_DETAIL, id))));
    }

    private Region navItem(String icon, String text, boolean active, boolean enabled, Runnable action) {
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("nav-icon");

        Label textLabel = new Label(text);
        textLabel.getStyleClass().add("nav-text");

        HBox row = new HBox(10, iconLabel, textLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("nav-item");
        if (active) {
            row.getStyleClass().add("nav-item-active");
        }
        if (!enabled) {
            row.getStyleClass().add("nav-item-disabled");
        } else {
            row.setOnMouseClicked(e -> action.run());
        }
        return row;
    }

    /** 목록 구역을 문맥에 맞춰 그린다. 모임을 열었으면 그 모임의 여행 목록을 보여준다. */
    private void renderGroups() {
        groupListBox.getChildren().clear();

        Optional<Long> openGroupId = AppContext.currentGroupId();
        boolean showingTrips = openGroupId.isPresent();

        listTitle.setText(showingTrips ? "여행" : "내 모임");
        listAddButton.setText(showingTrips ? "＋ 여행" : "＋ 모임");
        listBackButton.setVisible(showingTrips);
        listBackButton.setManaged(showingTrips);

        if (showingTrips) {
            long groupId = openGroupId.get();
            listAddButton.setOnAction(e -> Router.navigate(Route.TRIP_CREATE, groupId));
            renderTripList(groupId);
        } else {
            listAddButton.setOnAction(e -> Router.navigate(Route.GROUP_CREATE));
            renderGroupList();
        }
    }

    private void renderGroupList() {
        List<Group> groups;
        try {
            groups = GroupRepository.findAllForUser(UserRepository.currentUserId());
        } catch (DataAccessException | IllegalStateException e) {
            groupListBox.getChildren().add(hintLabel("목록을 불러오지 못했습니다"));
            return;
        }

        if (groups.isEmpty()) {
            groupListBox.getChildren().add(hintLabel("참여 중인 모임이 없습니다"));
            return;
        }

        for (Group group : groups) {
            groupListBox.getChildren().add(listItem(
                    group.name(),
                    "참여자 " + group.memberCount() + "명 · " + group.inviteCode(),
                    false,
                    () -> Router.navigate(Route.GROUP_DETAIL, group.id())));
        }
    }

    private void renderTripList(long groupId) {
        List<Trip> trips;
        try {
            trips = TripRepository.findAllByGroup(groupId);
        } catch (DataAccessException e) {
            groupListBox.getChildren().add(hintLabel("여행을 불러오지 못했습니다"));
            return;
        }

        if (trips.isEmpty()) {
            groupListBox.getChildren().add(hintLabel("아직 여행이 없습니다"));
            return;
        }

        Long activeTripId = AppContext.currentTripId().orElse(null);
        for (Trip trip : trips) {
            groupListBox.getChildren().add(listItem(
                    trip.title(),
                    Dates.period(trip.startDate(), trip.endDate()),
                    Long.valueOf(trip.id()).equals(activeTripId),
                    () -> Router.navigate(Route.TRIP_DETAIL, trip.id())));
        }
    }

    private Region listItem(String name, String meta, boolean active, Runnable onOpen) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("sidebar-group-name");

        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("sidebar-group-meta");

        VBox box = new VBox(2, nameLabel, metaLabel);
        box.getStyleClass().add("sidebar-group-item");
        if (active) {
            box.getStyleClass().add("sidebar-group-item-active");
        }
        box.setOnMouseClicked(e -> onOpen.run());
        return box;
    }

    private Label hintLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("sidebar-hint");
        label.setWrapText(true);
        return label;
    }

    private void renderUser() {
        List<AppUser> users;
        Optional<AppUser> current;
        try {
            users = UserRepository.findAll();
            current = UserRepository.currentUser();
        } catch (DataAccessException e) {
            userButton.setText("프로필 오류");
            return;
        }

        userButton.setText("  " + current.map(AppUser::displayName).orElse("프로필 없음"));
        userButton.getItems().clear();

        for (AppUser user : users) {
            MenuItem item = new MenuItem(user.displayName());
            item.setOnAction(e -> switchUser(user));
            userButton.getItems().add(item);
        }

        userButton.getItems().add(new SeparatorMenuItem());

        MenuItem addItem = new MenuItem("＋ 새 프로필 추가");
        addItem.setOnAction(e -> addUser());
        userButton.getItems().add(addItem);
    }

    // ----- 프로필 전환 -----

    /**
     * 프로필을 바꾼다.
     *
     * <p>이 기능이 있어야 초대 코드 참여를 한 PC 에서 끝까지 보여줄 수 있다.
     * A 가 모임을 만들고 코드를 복사한 뒤 B 로 전환해 그 코드를 입력하면,
     * B 의 모임 목록에 모임이 나타난다.</p>
     */
    private void switchUser(AppUser user) {
        UserRepository.setCurrentUser(user.id());
        AppContext.clear();
        Router.resetTo(Route.GROUP_LIST, null);
        Toast.success(user.displayName() + " 프로필로 전환했습니다.");
    }

    private void addUser() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("새 프로필 추가");
        dialog.setHeaderText("초대 코드 참여를 시연하기 위한 로컬 프로필입니다.");
        dialog.setContentText("표시 이름:");

        dialog.showAndWait().ifPresent(rawName -> {
            String name = rawName.trim();
            if (name.isEmpty()) {
                Toast.warn("이름을 입력해 주세요.");
                return;
            }
            try {
                AppUser created = UserRepository.insert(name);
                switchUser(created);
            } catch (DataAccessException e) {
                Toast.warn(e.getMessage());
            }
        });
    }
}
