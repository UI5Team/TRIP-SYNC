package com.tripsync.controller;

import java.util.List;
import java.util.Optional;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.GroupRepository;
import com.tripsync.model.Group;
import com.tripsync.nav.AppContext;
import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.EmptyState;
import com.tripsync.ui.Toast;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

/**
 * 화면 3 — 모임 상세 및 여행 목록.
 *
 * <p><b>[2단계]</b> 모임 정보와 참여자는 저장소에서 읽어온다.
 * 여행 목록은 3단계에서 연결한다.</p>
 */
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

    /** 이전 화면이 넘겨준 모임 id 로 모임을 읽어온다. */
    private Optional<Group> loadGroup(Object param) {
        if (!(param instanceof Long groupId)) {
            return Optional.empty();
        }
        try {
            return GroupRepository.findById(groupId);
        } catch (DataAccessException e) {
            Toast.warn(e.getMessage());
            return Optional.empty();
        }
    }

    /** 참여자 이름을 나열한다. 이름이 많으면 뒤는 "외 N명"으로 줄인다. */
    private void renderMembers() {
        try {
            List<String> names = GroupRepository.findMemberNames(group.id());

            String summary;
            if (names.size() <= 3) {
                summary = String.join(", ", names);
            } else {
                summary = String.join(", ", names.subList(0, 3))
                        + " 외 " + (names.size() - 3) + "명";
            }

            memberLabel.setText("참여자 " + names.size() + "명 · " + summary
                    + "  —  이 코드를 입력하면 모임에 참여할 수 있습니다.");

        } catch (DataAccessException e) {
            memberLabel.setText("참여자 정보를 불러오지 못했습니다.");
        }
    }

    /** [3단계에서 구현] 저장소에서 이 모임의 여행을 조회한다. */
    private void renderTrips() {
        tripListBox.getChildren().clear();
        tripCountLabel.setText("");

        // 설계서 10장: 모임에 여행이 없을 때의 안내
        tripListBox.getChildren().add(EmptyState
                .of("아직 여행이 없습니다",
                        "첫 여행을 만들고 날짜별 일정을 채워 보세요.")
                .primary("새 여행 만들기", () -> Router.navigate(Route.TRIP_CREATE, group.id()))
                .build());
    }

    /** 모임을 찾지 못한 경우 (링크가 끊겼거나 삭제됨) */
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

    /** 초대 코드를 클립보드에 복사한다. 네트워크를 쓰지 않는 OS 기본 기능이다. */
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
