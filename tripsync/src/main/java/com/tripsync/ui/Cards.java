package com.tripsync.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class Cards {

    private Cards() {
    }

    public static Node clickable(String title, String meta, String trailing, Runnable onOpen) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("card-meta");

        VBox texts = new VBox(4, titleLabel, metaLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(14, texts, spacer);
        row.setAlignment(Pos.CENTER_LEFT);

        if (trailing != null && !trailing.isBlank()) {
            Label tag = new Label(trailing);
            tag.getStyleClass().add("invite-code");
            row.getChildren().add(tag);
        }

        Label chevron = new Label("›");
        chevron.getStyleClass().add("card-meta");
        row.getChildren().add(chevron);

        row.getStyleClass().addAll("card", "card-clickable");
        row.setOnMouseClicked(e -> onOpen.run());
        return row;
    }
}
