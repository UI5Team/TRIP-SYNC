package com.tripsync.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class EmptyState {

    private final String title;
    private final String description;
    private final HBox actions = new HBox(10);

    private EmptyState(String title, String description) {
        this.title = title;
        this.description = description;
        this.actions.setAlignment(Pos.CENTER);
    }

    public static EmptyState of(String title, String description) {
        return new EmptyState(title, description);
    }

    public EmptyState primary(String text, Runnable action) {
        return addButton(text, action, "button-primary");
    }

    public EmptyState secondary(String text, Runnable action) {
        return addButton(text, action, "button-ghost");
    }

    private EmptyState addButton(String text, Runnable action, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setOnAction(e -> action.run());
        actions.getChildren().add(button);
        return this;
    }

    public Node build() {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("empty-description");
        descLabel.setWrapText(true);

        VBox box = new VBox(8, titleLabel, descLabel);
        box.getStyleClass().add("empty-state");
        box.setAlignment(Pos.CENTER);

        if (!actions.getChildren().isEmpty()) {
            VBox.setMargin(actions, new javafx.geometry.Insets(10, 0, 0, 0));
            box.getChildren().add(actions);
        }
        return box;
    }
}
