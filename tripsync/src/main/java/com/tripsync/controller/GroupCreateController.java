package com.tripsync.controller;

import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Toast;
import com.tripsync.util.InviteCode;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * 화면 2 — 모임 생성.
 *
 * <p><b>[1단계]</b> 입력 검증과 화면 이동까지 구현한다.
 * 실제 저장은 2단계에서 SQLite 저장소를 붙이며 추가한다.</p>
 */
public class GroupCreateController implements RoutedController {

    private static final int MAX_NAME_LENGTH = 30;

    @FXML private TextField groupNameField;
    @FXML private Label nameErrorLabel;
    @FXML private Label inviteCodeLabel;
    @FXML private Button regenerateButton;
    @FXML private Button cancelButton;
    @FXML private Button submitButton;

    /** 화면에 표시 중인, 저장되면 쓰일 초대 코드 */
    private String inviteCode;

    @FXML
    private void initialize() {
        regenerateButton.setOnAction(e -> refreshInviteCode());
        cancelButton.setOnAction(e -> Router.back());
        submitButton.setOnAction(e -> handleSubmit());

        // 다시 입력하기 시작하면 오류 표시를 지운다
        groupNameField.textProperty().addListener((obs, old, now) -> hideNameError());

        // Enter 로도 생성할 수 있게 한다
        groupNameField.setOnAction(e -> handleSubmit());
    }

    @Override
    public void onEnter(Object param) {
        groupNameField.clear();
        hideNameError();
        refreshInviteCode();
        groupNameField.requestFocus();
    }

    private void refreshInviteCode() {
        inviteCode = InviteCode.generate();
        inviteCodeLabel.setText(inviteCode);
    }

    private void handleSubmit() {
        String name = groupNameField.getText() == null ? "" : groupNameField.getText().trim();

        // 설계서 10장: 필수 입력 누락 시 저장하지 않고 해당 필드 아래에 안내
        if (name.isEmpty()) {
            showNameError("모임 이름을 입력해 주세요.");
            return;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            showNameError("모임 이름은 " + MAX_NAME_LENGTH + "자 이내로 입력해 주세요.");
            return;
        }

        // [2단계에서 추가] 여기서 저장소에 모임을 저장한다.

        Toast.success("'" + name + "' 모임을 만들었습니다. 초대 코드: " + inviteCode);

        // 생성한 모임의 상세 화면으로 이동한다.
        // 생성 화면은 히스토리에 남기지 않아야 '뒤로'가 목록으로 바로 돌아간다.
        Router.back();
        Router.navigate(Route.GROUP_DETAIL, name);
    }

    private void showNameError(String message) {
        nameErrorLabel.setText(message);
        nameErrorLabel.setVisible(true);
        nameErrorLabel.setManaged(true);
        if (!groupNameField.getStyleClass().contains("field-error")) {
            groupNameField.getStyleClass().add("field-error");
        }
        groupNameField.requestFocus();
    }

    private void hideNameError() {
        nameErrorLabel.setVisible(false);
        nameErrorLabel.setManaged(false);
        groupNameField.getStyleClass().remove("field-error");
    }
}
