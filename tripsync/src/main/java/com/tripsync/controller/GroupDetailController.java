package com.tripsync.controller;

import java.util.List;
import java.util.Optional;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.GroupRepository;
import com.tripsync.data.TripRepository;
import com.tripsync.model.Group;
import com.tripsync.model.Trip;
import com.tripsync.nav.AppContext;
import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Dialogs;
import com.tripsync.ui.EmptyState;
import com.tripsync.ui.Toast;
import com.tripsync.util.Dates;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class GroupDetailController implements RoutedController {

    @FXML private Label groupNameLabel;
    @FXML private Label inviteCodeLabel;
    @FXML private Button copyCodeButton;
    @FXML private Label memberLabel;
    @FXML private Label tripCountLabel;
    @FXML private Button createTripButton;
    @FXML private VBox tripListBox;

    private Group group;

    @FXML
    private void initialize() {
        createTripButton.setOnAction(e -> {
            if (group != null) {
                Router.navigate(Route.TRIP_CREATE, group.id());
            }
        });
        copyCodeButton.setOnAction(e -> copyInviteCode());
    }

    @Override
    public void onEnter(Object param) {
        Optional<Group> loaded = loadGroup(param);

        if (loaded.isEmpty()) {
            showMissingGroup();
            return;
        }

        group = loaded.get();
        AppContext.setCurrentGroup(group.id(), group.name());

        groupNameLabel.setText(group.name());
        inviteCodeLabel.setText(group.inviteCode());
        createTripButton.setDisable(false);
        copyCodeButton.setDisable(false);

        renderMembers();
        renderTrips();
    }

    private Optional<Group> loadGroup(Object param) {
        Long groupId = param instanceof Long id ? id : AppContext.currentGroupId().orElse(null);
        if (groupId == null) {
            return Optional.empty();
        }
        try {
            return GroupRepository.findById(groupId);
        } catch (DataAccessException e) {
            Toast.warn(e.getMessage());
            return Optional.empty();
        }
    }

    private void renderMembers() {
        try {
            List<String> names = GroupRepository.findMemberNames(group.id());

            String summary = names.size() <= 3
                    ? String.join(", ", names)
                    : String.join(", ", names.subList(0, 3)) + " 외 " + (names.size() - 3) + "명";

            memberLabel.setText("참여자 " + names.size() + "명 · " + summary
                    + "  —  이 코드를 입력하면 모임에 참여할 수 있습니다.");

        } catch (DataAccessException e) {
            memberLabel.setText("참여자 정보를 불러오지 못했습니다.");
        }
    }

    private void renderTrips() {
        tripListBox.getChildren().clear();

        List<Trip> trips;
        try {
            trips = TripRepository.findAllByGroup(group.id());
        } catch (DataAccessException e) {
            tripCountLabel.setText("");
            tripListBox.getChildren().add(EmptyState
                    .of("여행 목록을 불러오지 못했습니다", e.getMessage())
                    .primary("다시 시도", this::renderTrips)
                    .build());
            return;
        }

        tripCountLabel.setText(trips.isEmpty() ? "" : trips.size() + "개");

        if (trips.isEmpty()) {
            tripListBox.getChildren().add(EmptyState
                    .of("아직 여행이 없습니다",
                            "첫 여행을 만들고 날짜별 일정을 채워 보세요.")
                    .primary("새 여행 만들기", () -> Router.navigate(Route.TRIP_CREATE, group.id()))
                    .build());
            return;
        }

        for (Trip trip : trips) {
            tripListBox.getChildren().add(buildTripCard(trip));
        }
    }

    private Node buildTripCard(Trip trip) {
        Label title = new Label(trip.title());
        title.getStyleClass().add("card-title");

        String meta = Dates.period(trip.startDate(), trip.endDate())
                + " · " + Dates.duration(trip.startDate(), trip.endDate())
                + " · 일정 " + trip.scheduleCount() + "개";

        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("card-meta");

        VBox texts = new VBox(4, title, metaLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteButton = new Button("삭제");
        deleteButton.getStyleClass().add("button-danger");
        deleteButton.setOnAction(e -> {
            e.consume();
            handleDelete(trip);
        });

        Label chevron = new Label("›");
        chevron.getStyleClass().add("card-meta");

        HBox row = new HBox(12, texts, spacer, deleteButton, chevron);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("card", "card-clickable");
        row.setOnMouseClicked(e -> Router.navigate(Route.TRIP_DETAIL, trip.id()));
        return row;
    }

    private void handleDelete(Trip trip) {
        String message = TripRepository.hasContent(trip.id())
                ? "'" + trip.title() + "' 여행을 삭제하면 그 안의 일정과 위시리스트도 함께 사라집니다.\n"
                        + "이 작업은 되돌릴 수 없습니다."
                : "'" + trip.title() + "' 여행을 삭제합니다.";

        if (!Dialogs.confirm("여행 삭제", message, "삭제")) {
            return;
        }

        try {
            TripRepository.delete(trip.id());
            Toast.success("'" + trip.title() + "' 여행을 삭제했습니다.");
            renderTrips();
        } catch (DataAccessException e) {
            Toast.warn(e.getMessage());
        }
    }

    private void showMissingGroup() {
        group = null;
        groupNameLabel.setText("모임을 찾을 수 없습니다");
        inviteCodeLabel.setText("——————");
        memberLabel.setText("요청한 모임이 삭제되었거나 번호가 올바르지 않습니다.");
        tripCountLabel.setText("");
        createTripButton.setDisable(true);
        copyCodeButton.setDisable(true);

        tripListBox.getChildren().clear();
        tripListBox.getChildren().add(EmptyState
                .of("모임 정보를 불러올 수 없습니다",
                        "모임 목록으로 돌아가 다시 선택해 주세요.")
                .primary("모임 목록으로", () -> Router.resetTo(Route.GROUP_LIST, null))
                .build());
    }

    private void copyInviteCode() {
        if (group == null) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(group.inviteCode());
        Clipboard.getSystemClipboard().setContent(content);
        Toast.success("초대 코드 " + group.inviteCode() + " 를 복사했습니다.");
    }
}
