package com.tripsync.nav;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * 앱 맨 위에 항상 떠 있는 상단 바.
 *
 * <p>왼쪽부터 뒤로 버튼, 서비스명, 현재 화면 이름, 오른쪽에 오프라인 배지를 둔다.
 * 지금 어느 화면에 있는지와 어떻게 돌아가는지를 항상 보이게 해서,
 * 설계서 13장 체크리스트의 "각 버튼이 어떤 화면으로 이동하는지 명확한가"를 만족시킨다.</p>
 */
public class AppBar extends HBox {

    private final Button backButton;
    private final Label screenTitle;

    public AppBar() {
        getStyleClass().add("app-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);

        backButton = new Button("← 뒤로");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> Router.back());

        Label brand = new Label("TripSync");
        brand.getStyleClass().add("brand");

        Label divider = new Label("/");
        divider.getStyleClass().add("app-bar-divider");

        screenTitle = new Label();
        screenTitle.getStyleClass().add("screen-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 이 앱이 네트워크를 전혀 쓰지 않는다는 점을 화면에서도 드러낸다
        Label offlineBadge = new Label("오프라인 저장");
        offlineBadge.getStyleClass().add("offline-badge");

        getChildren().addAll(backButton, brand, divider, screenTitle, spacer, offlineBadge);
    }

    /** 화면이 바뀔 때마다 라우터가 호출한다. */
    public void update(String title, boolean backEnabled) {
        screenTitle.setText(title);
        backButton.setVisible(backEnabled);
        backButton.setManaged(backEnabled);
    }
}
