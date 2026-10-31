package com.tripsync.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

public final class Dialogs {

    private Dialogs() {
    }

    public static boolean confirm(String title, String message, String confirmText) {
        ButtonType confirmButton = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("취소", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.getButtonTypes().setAll(cancelButton, confirmButton);

        Label content = new Label(message);
        content.setWrapText(true);
        content.setMaxWidth(380);
        alert.getDialogPane().setContent(content);

        return alert.showAndWait()
                .filter(choice -> choice == confirmButton)
                .isPresent();
    }
}
