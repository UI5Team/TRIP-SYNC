package com.tripsync.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

public final class Dialogs {

    private static final String STYLESHEET = "/com/tripsync/css/app.css";

    private Dialogs() {
    }

    public static boolean confirm(String title, String message, String detail, String confirmText) {
        ButtonType confirmButton = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("취소", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(title);
        alert.getButtonTypes().setAll(cancelButton, confirmButton);
        alert.initStyle(StageStyle.TRANSPARENT);

        DialogPane pane = alert.getDialogPane();
        pane.setHeaderText(null);
        pane.setGraphic(null);
        pane.getStyleClass().add("ts-dialog");
        pane.setContent(buildContent(title, message, detail));

        var css = Dialogs.class.getResource(STYLESHEET);
        if (css != null) {
            pane.getStylesheets().add(css.toExternalForm());
        }

        pane.lookupButton(confirmButton).getStyleClass().add("dialog-confirm");
        pane.lookupButton(cancelButton).getStyleClass().add("dialog-cancel");

        makeSceneTransparent(pane.getScene());
        alert.setOnShowing(e -> makeSceneTransparent(pane.getScene()));

        return alert.showAndWait()
                .filter(choice -> choice == confirmButton)
                .isPresent();
    }

    private static void makeSceneTransparent(Scene scene) {
        if (scene != null) {
            scene.setFill(Color.TRANSPARENT);
        }
    }

    private static VBox buildContent(String title, String message, String detail) {
        Label glyph = new Label("!");
        glyph.getStyleClass().add("dialog-icon-glyph");

        StackPane icon = new StackPane(glyph);
        icon.getStyleClass().add("dialog-icon");
        icon.setMinSize(42, 42);
        icon.setPrefSize(42, 42);
        icon.setMaxSize(42, 42);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");

        HBox head = new HBox(13, icon, titleLabel);
        head.setAlignment(Pos.CENTER_LEFT);

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("dialog-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(360);

        VBox box = new VBox(12, head, messageLabel);

        if (detail != null && !detail.isBlank()) {
            Label detailLabel = new Label(detail);
            detailLabel.getStyleClass().add("dialog-detail");
            detailLabel.setWrapText(true);
            detailLabel.setMaxWidth(360);
            box.getChildren().add(detailLabel);
        }

        box.getStyleClass().add("dialog-content");
        return box;
    }
}
