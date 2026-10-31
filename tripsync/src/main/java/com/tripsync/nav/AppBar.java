package com.tripsync.nav;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * 본문 위에 항상 떠 있는 상단 바.
 *
 * <p>왼쪽에 뒤로 버튼과 현재 화면 이름, 오른쪽에 오프라인 배지를 둔다.
 * 서비스명은 사이드바 맨 위로 옮겼으므로 여기서는 반복하지 않는다.</p>
 */
public class AppBar extends HBox {

    private final Button backButton;
    private final Label screenTitle;

    public AppBar() {
        getStyleClass().add("app-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);

        backButton = new Button("←");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> Router.back());
        backButton.setTooltip(new Tooltip("이전 화면으로"));

        screenTitle = new Label();
        screenTitle.getStyleClass().add("screen-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 이 앱이 네트워크를 전혀 쓰지 않는다는 점을 화면에서도 드러낸다
        Label offlineBadge = new Label("● 오프라인 저장");
        offlineBadge.getStyleClass().add("offline-badge");
        offlineBadge.setTooltip(new Tooltip(
                "인터넷 연결 없이 이 PC 안에만 저장됩니다. 외부 서버로 전송하지 않습니다."));

        getChildren().addAll(backButton, screenTitle, spacer, offlineBadge);
    }

    /** 화면이 바뀔 때마다 라우터가 호출한다. */
    public void update(String title, boolean backEnabled) {
        screenTitle.setText(title);
        backButton.setVisible(backEnabled);
        backButton.setManaged(backEnabled);
    }
}
