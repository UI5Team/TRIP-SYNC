package com.tripsync.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public final class Toast {

    private static StackPane layer;

    private Toast() {
    }

    public static void init(StackPane overlayLayer) {
        layer = overlayLayer;
        layer.setAlignment(Pos.BOTTOM_CENTER);
        layer.setPadding(new Insets(0, 0, 36, 0));

        layer.setMouseTransparent(true);
    }

    public static void success(String message) {
        show(message, "toast-success");
    }

    public static void warn(String message) {
        show(message, "toast-warn");
    }

    private static void show(String message, String styleClass) {
        if (layer == null) {

            return;
        }

        Label toast = new Label(message);
        toast.getStyleClass().addAll("toast", styleClass);
        toast.setOpacity(0);
        layer.getChildren().add(toast);

        FadeTransition in = new FadeTransition(Duration.millis(160), toast);
        in.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.millis(1900));

        FadeTransition out = new FadeTransition(Duration.millis(260), toast);
        out.setToValue(0);

        SequentialTransition sequence = new SequentialTransition(in, stay, out);
        sequence.setOnFinished(e -> layer.getChildren().remove(toast));
        sequence.play();
    }
}
