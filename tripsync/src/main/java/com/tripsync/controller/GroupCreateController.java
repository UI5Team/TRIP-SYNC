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

        groupNameField.textProperty().addListener((obs, old, now) -> hideNameError());

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

            Router.back();
            Router.navigate(Route.GROUP_DETAIL, created.id());

        } catch (DataAccessException e) {

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
