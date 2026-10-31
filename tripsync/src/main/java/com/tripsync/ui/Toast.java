package com.tripsync.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 화면 아래쪽에 잠깐 나타났다 사라지는 알림.
 *
 * <p>설계서 7장 화면 5는 "저장 후에는 성공 메시지를 보여주고 최신 목록을 다시 조회한다"고
 * 정하고 있다. 저장할 때마다 확인 창을 띄우면 흐름이 끊기므로, 작업을 막지 않는
 * 토스트로 결과만 알린다. 반대로 사용자가 반드시 읽어야 하는 실패 안내는
 * 입력란 옆 오류 메시지나 다이얼로그로 따로 처리한다.</p>
 */
public final class Toast {

    private static StackPane layer;

    private Toast() {
    }

    /** 토스트가 뜰 레이어를 등록한다. 앱 시작 시 한 번만 호출. */
    public static void init(StackPane overlayLayer) {
        layer = overlayLayer;
        layer.setAlignment(Pos.BOTTOM_CENTER);
        layer.setPadding(new Insets(0, 0, 36, 0));
        // 토스트가 없을 때는 레이어가 마우스 클릭을 가로채지 않도록 한다
        layer.setMouseTransparent(true);
    }

    /** 성공 메시지 */
    public static void success(String message) {
        show(message, "toast-success");
    }

    /** 경고 / 실패 메시지 */
    public static void warn(String message) {
        show(message, "toast-warn");
    }

    private static void show(String message, String styleClass) {
        if (layer == null) {
            // 레이어가 없으면 조용히 무시한다 (테스트 등에서 UI 없이 호출된 경우)
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
