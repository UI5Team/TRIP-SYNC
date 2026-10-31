package com.tripsync.nav;

import com.tripsync.ui.SearchBox;
import com.tripsync.ui.Toast;
import com.tripsync.ui.UserMenu;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

public class AppBar extends HBox {

    public static final double BRAND_WIDTH = 236;

    private final Button backButton;
    private final Label screenTitle;
    private final UserMenu userMenu;

    public AppBar() {
        getStyleClass().add("app-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);

        backButton = new Button("←");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> Router.back());
        backButton.setTooltip(new Tooltip("이전 화면으로"));

        screenTitle = new Label();
        screenTitle.getStyleClass().add("screen-title");

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        SearchBox searchBox = new SearchBox();

        Label offlineBadge = new Label("● 오프라인");
        offlineBadge.getStyleClass().add("offline-badge");
        offlineBadge.setTooltip(new Tooltip(
                "인터넷 연결 없이 이 PC 안에만 저장됩니다. 외부 서버로 전송하지 않습니다."));

        userMenu = new UserMenu();

        getChildren().addAll(
                buildBrand(),
                backButton,
                screenTitle,
                leftSpacer,
                searchBox,
                rightSpacer,
                offlineBadge,
                buildNotificationButton(),
                buildSettingsButton(),
                userMenu);
    }

    private Region buildBrand() {
        Label mark = new Label("✈");
        mark.getStyleClass().add("brand-mark");

        Label name = new Label("TripSync");
        name.getStyleClass().add("brand-name");

        HBox brand = new HBox(8, mark, name);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("app-bar-brand");
        brand.setMinWidth(BRAND_WIDTH);
        brand.setPrefWidth(BRAND_WIDTH);
        brand.setMaxWidth(BRAND_WIDTH);
        brand.setOnMouseClicked(e -> Router.resetTo(Route.GROUP_LIST, null));
        return brand;
    }

    private Region buildNotificationButton() {
        Label glyph = new Label("🔔");
        glyph.getStyleClass().add("icon-button-glyph");

        Circle badge = new Circle(3.5);
        badge.getStyleClass().add("notification-dot");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new javafx.geometry.Insets(7, 8, 0, 0));

        StackPane button = new StackPane(glyph, badge);
        button.getStyleClass().add("icon-button");
        button.setMinSize(36, 36);
        button.setPrefSize(36, 36);
        button.setMaxSize(36, 36);
        Tooltip.install(button, new Tooltip("알림"));
        button.setOnMouseClicked(e -> Toast.success("새로운 알림이 없습니다."));
        return button;
    }

    private Region buildSettingsButton() {
        Label glyph = new Label("⚙");
        glyph.getStyleClass().add("icon-button-glyph");

        StackPane button = new StackPane(glyph);
        button.getStyleClass().add("icon-button");
        button.setMinSize(36, 36);
        button.setPrefSize(36, 36);
        button.setMaxSize(36, 36);
        Tooltip.install(button, new Tooltip("설정"));
        button.setOnMouseClicked(e -> Router.navigate(Route.SETTINGS, "설정"));
        return button;
    }

    public void update(String title, boolean backEnabled) {
        screenTitle.setText(title);
        backButton.setVisible(backEnabled);
        backButton.setManaged(backEnabled);
        userMenu.refresh();
    }
}
