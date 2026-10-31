package com.tripsync.controller;

import java.util.List;

import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Cards;
import com.tripsync.ui.EmptyState;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * 화면 1 — 시작 화면 및 모임 목록.
 *
 * <p><b>[1단계]</b> 지금은 저장소가 없어 샘플 모임을 하드코딩해 보여준다.
 * 화면 이동이 전부 동작하는지 확인하는 것이 1단계의 목표이고,
 * 2단계에서 이 부분을 SQLite 저장소 조회로 교체한다.</p>
 */
public class GroupListController implements RoutedController {

    @FXML private Button createGroupButton;
    @FXML private TextField inviteCodeField;
    @FXML private Button joinButton;
    @FXML private Label joinErrorLabel;
    @FXML private Label groupCountLabel;
    @FXML private Button toggleEmptyPreviewButton;
    @FXML private VBox groupListBox;

    /** 빈 상태를 보여주는 중인지 (1단계 미리보기용 토글) */
    private boolean showingEmptyPreview = false;

    /** [1단계] 임시 샘플 데이터 — 2단계에서 저장소 조회로 대체한다. */
    private record SampleGroup(String name, String inviteCode, int tripCount) {
    }

    private static final List<SampleGroup> SAMPLE_GROUPS = List.of(
            new SampleGroup("졸업 여행 준비방", "TS4K7Q", 2),
            new SampleGroup("동아리 MT", "TSB2XM", 1)
    );

    @FXML
    private void initialize() {
        createGroupButton.setOnAction(e -> Router.navigate(Route.GROUP_CREATE));
        joinButton.setOnAction(e -> handleJoin());
        toggleEmptyPreviewButton.setOnAction(e -> {
            showingEmptyPreview = !showingEmptyPreview;
            toggleEmptyPreviewButton.setText(showingEmptyPreview ? "샘플 목록 보기" : "빈 상태 미리보기");
            renderGroups();
        });

        // 코드를 고쳐 입력하기 시작하면 이전 오류 메시지를 지운다
        inviteCodeField.textProperty().addListener((obs, old, now) -> hideJoinError());
    }

    @Override
    public void onEnter(Object param) {
        renderGroups();
    }

    /** 목록 영역을 다시 그린다. 설계서의 "저장 후 재조회" 흐름이 이 메서드로 모인다. */
    private void renderGroups() {
        groupListBox.getChildren().clear();

        List<SampleGroup> groups = showingEmptyPreview ? List.of() : SAMPLE_GROUPS;
        groupCountLabel.setText(groups.isEmpty() ? "" : groups.size() + "개");

        if (groups.isEmpty()) {
            groupListBox.getChildren().add(buildEmptyState());
            return;
        }

        for (SampleGroup group : groups) {
            groupListBox.getChildren().add(buildGroupCard(group));
        }
    }

    private Node buildGroupCard(SampleGroup group) {
        String meta = group.tripCount() == 0
                ? "아직 여행이 없습니다"
                : "여행 " + group.tripCount() + "개";

        return Cards.clickable(
                group.name(),
                meta,
                group.inviteCode(),
                () -> Router.navigate(Route.GROUP_DETAIL, group.name()));
    }

    /** 설계서 10장: 참여 중인 모임이 없을 때의 안내 */
    private Node buildEmptyState() {
        return EmptyState
                .of("아직 참여 중인 모임이 없습니다",
                        "모임을 만들어 친구를 초대하거나, 받은 초대 코드를 입력해 참여해 보세요.")
                .primary("모임 만들기", () -> Router.navigate(Route.GROUP_CREATE))
                .secondary("초대 코드 입력하기", () -> inviteCodeField.requestFocus())
                .build();
    }

    /**
     * 초대 코드 참여.
     *
     * <p><b>[1단계]</b> 입력값 형식만 검사해 오류 표시 흐름을 확인한다.
     * 실제 코드 조회는 2단계에서 저장소를 붙이며 구현한다.</p>
     */
    private void handleJoin() {
        String code = inviteCodeField.getText() == null ? "" : inviteCodeField.getText().trim();

        if (code.isEmpty()) {
            showJoinError("초대 코드를 입력해 주세요.");
            return;
        }

        boolean exists = SAMPLE_GROUPS.stream()
                .anyMatch(g -> g.inviteCode().equalsIgnoreCase(code));

        if (!exists) {
            showJoinError("코드를 확인해 주세요. 해당 코드의 모임을 찾을 수 없습니다.");
            return;
        }

        hideJoinError();
        inviteCodeField.clear();
        Router.navigate(Route.GROUP_DETAIL, code);
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
