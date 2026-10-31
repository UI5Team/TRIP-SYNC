package com.tripsync.controller;

import java.util.List;
import java.util.Optional;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.GroupRepository;
import com.tripsync.data.UserRepository;
import com.tripsync.model.AppUser;
import com.tripsync.model.Group;
import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Cards;
import com.tripsync.ui.EmptyState;
import com.tripsync.ui.Toast;
import com.tripsync.util.InviteCode;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class GroupListController implements RoutedController {

    @FXML private Button createGroupButton;
    @FXML private TextField inviteCodeField;
    @FXML private Button joinButton;
    @FXML private Label joinErrorLabel;
    @FXML private Label groupCountLabel;
    @FXML private Label currentUserLabel;
    @FXML private VBox groupListBox;

    @FXML
    private void initialize() {
        createGroupButton.setOnAction(e -> Router.navigate(Route.GROUP_CREATE));
        joinButton.setOnAction(e -> handleJoin());

        inviteCodeField.setOnAction(e -> handleJoin());

        inviteCodeField.textProperty().addListener((obs, old, now) -> hideJoinError());
    }

    @Override
    public void onEnter(Object param) {
        renderGroups();
    }

    private void renderGroups() {
        groupListBox.getChildren().clear();

        List<Group> groups;
        try {
            groups = GroupRepository.findAllForUser(UserRepository.currentUserId());
            currentUserLabel.setText(UserRepository.currentUser()
                    .map(user -> user.displayName() + " 님의 목록")
                    .orElse(""));

        } catch (DataAccessException e) {
            groupCountLabel.setText("");
            groupListBox.getChildren().add(EmptyState
                    .of("모임 목록을 불러오지 못했습니다", e.getMessage())
                    .primary("다시 시도", this::renderGroups)
                    .build());
            return;
        }

        groupCountLabel.setText(groups.isEmpty() ? "" : groups.size() + "개");

        if (groups.isEmpty()) {
            groupListBox.getChildren().add(buildEmptyState());
            return;
        }

        for (Group group : groups) {
            groupListBox.getChildren().add(buildGroupCard(group));
        }
    }

    private Node buildGroupCard(Group group) {
        String meta = "참여자 " + group.memberCount() + "명";

        return Cards.clickable(
                group.name(),
                meta,
                group.inviteCode(),
                () -> Router.navigate(Route.GROUP_DETAIL, group.id()));
    }

    private Node buildEmptyState() {
        return EmptyState
                .of("아직 참여 중인 모임이 없습니다",
                        "모임을 만들어 친구를 초대하거나, 받은 초대 코드를 입력해 참여해 보세요.")
                .primary("모임 만들기", () -> Router.navigate(Route.GROUP_CREATE))
                .secondary("초대 코드 입력하기", () -> inviteCodeField.requestFocus())
                .build();
    }

    private void handleJoin() {
        String code = InviteCode.normalize(inviteCodeField.getText());

        if (code.isEmpty()) {
            showJoinError("초대 코드를 입력해 주세요.");
            return;
        }
        if (!InviteCode.isValidFormat(code)) {
            showJoinError("코드 형식이 올바르지 않습니다. TS 로 시작하는 6자리 코드입니다. (예: TS4K7Q)");
            return;
        }

        try {
            Optional<Group> found = GroupRepository.findByInviteCode(code);

            if (found.isEmpty()) {
                showJoinError("코드를 확인해 주세요. 해당 코드의 모임을 찾을 수 없습니다.");
                return;
            }

            Group group = found.get();
            long userId = UserRepository.currentUserId();
            String userName = UserRepository.currentUser()
                    .map(AppUser::displayName)
                    .orElse("나");

            boolean newlyJoined = GroupRepository.join(group.id(), userId);

            inviteCodeField.clear();
            hideJoinError();

            Toast.success(newlyJoined
                    ? userName + " 님이 '" + group.name() + "' 모임에 참여했습니다."
                    : "이미 참여 중인 모임입니다.");

            Router.navigate(Route.GROUP_DETAIL, group.id());

        } catch (DataAccessException e) {

            showJoinError(e.getMessage() + " 잠시 후 다시 시도해 주세요.");
        }
    }

    private void showJoinError(String message) {
        joinErrorLabel.setText(message);
        joinErrorLabel.setVisible(true);
        joinErrorLabel.setManaged(true);
        if (!inviteCodeField.getStyleClass().contains("field-error")) {
            inviteCodeField.getStyleClass().add("field-error");
        }
    }

    private void hideJoinError() {
        joinErrorLabel.setVisible(false);
        joinErrorLabel.setManaged(false);
        inviteCodeField.getStyleClass().remove("field-error");
    }
}
