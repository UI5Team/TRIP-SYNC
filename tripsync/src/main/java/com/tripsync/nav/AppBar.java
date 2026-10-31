package com.tripsync.nav;

import com.tripsync.ui.UserMenu;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class AppBar extends HBox {

    public static final double BRAND_WIDTH = 236;

    private final Button backButton;
    private final Label screenTitle;
    private final UserMenu userMenu;

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

        Label offlineBadge = new Label("● 오프라인 저장");
        offlineBadge.getStyleClass().add("offline-badge");
        offlineBadge.setTooltip(new Tooltip(
                "인터넷 연결 없이 이 PC 안에만 저장됩니다. 외부 서버로 전송하지 않습니다."));

        userMenu = new UserMenu();

        getChildren().addAll(buildBrand(), backButton, screenTitle, spacer, offlineBadge, userMenu);
    }

    private Region buildBrand() {
        Label mark = new Label("✈");
        mark.getStyleClass().add("brand-mark");

        Label name = new Label("TripSync");
        name.getStyleClass().add("brand-name");

        Label tagline = new Label("Travel Planner");
        tagline.getStyleClass().add("brand-tagline");

        HBox brand = new HBox(8, mark, name, tagline);
        brand.setAlignment(Pos.BOTTOM_LEFT);
        brand.getStyleClass().add("app-bar-brand");
        brand.setMinWidth(BRAND_WIDTH);
        brand.setPrefWidth(BRAND_WIDTH);
        brand.setMaxWidth(BRAND_WIDTH);
        return brand;
    }

    public void update(String title, boolean backEnabled) {
        screenTitle.setText(title);
        backButton.setVisible(backEnabled);
        backButton.setManaged(backEnabled);
        userMenu.refresh();
    }
}
