package com.tripsync.controller;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.GroupRepository;
import com.tripsync.data.UserRepository;
import com.tripsync.model.Group;
import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Toast;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * 화면 2 — 모임 생성.
 *
 * <p>모임을 저장하면 만든 사람이 자동으로 첫 참여자가 되고, 겹치지 않는 초대 코드가
 * 함께 발급된다. 두 작업은 저장소에서 한 트랜잭션으로 처리된다.</p>
 */
public class GroupCreateController implements RoutedController {

    private static final int MAX_NAME_LENGTH = 30;

    @FXML private TextField groupNameField;
    @FXML private Label nameErrorLabel;
    @FXML private Button cancelButton;
    @FXML private Button submitButton;

    @FXML
    private void initialize() {
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
        groupNameField.requestFocus();
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

        try {
            Group created = GroupRepository.create(name, UserRepository.currentUserId());

            Toast.success("'" + created.name() + "' 모임을 만들었습니다. 초대 코드 "
                    + created.inviteCode());

            // 생성 화면은 히스토리에 남기지 않아야 '뒤로'가 목록으로 바로 돌아간다
            Router.back();
            Router.navigate(Route.GROUP_DETAIL, created.id());

        } catch (DataAccessException e) {
            // 설계서 10장: 저장 실패 → 입력값을 유지하고 재시도 안내
            showNameError(e.getMessage() + " 입력한 내용은 그대로 두었으니 다시 시도해 주세요.");
        }
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
