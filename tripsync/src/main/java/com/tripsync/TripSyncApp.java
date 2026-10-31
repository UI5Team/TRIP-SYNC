package com.tripsync;

import java.net.URL;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.Database;
import com.tripsync.data.UserRepository;
import com.tripsync.nav.AppBar;
import com.tripsync.nav.Route;
import com.tripsync.nav.Router;
import com.tripsync.ui.Sidebar;
import com.tripsync.ui.Toast;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class TripSyncApp extends Application {

    private static final String STYLESHEET = "/com/tripsync/css/app.css";

    private static final double MIN_WIDTH = 1160;
    private static final double MIN_HEIGHT = 720;

    @Override
    public void start(Stage stage) {
        try {
            Database.init();
            UserRepository.ensureDefaultUser();
        } catch (DataAccessException e) {
            showFatalError(e);
            Platform.exit();
            return;
        }

        AppBar appBar = new AppBar();
        Sidebar sidebar = new Sidebar();

        BorderPane body = new BorderPane();
        body.getStyleClass().add("app-body");
        body.setLeft(sidebar);

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setTop(appBar);
        shell.setCenter(body);

        StackPane toastLayer = new StackPane();
        StackPane root = new StackPane(shell, toastLayer);

        Router.init(body, appBar);
        Router.setNavigationListener(sidebar::refresh);
        Toast.init(toastLayer);

        Router.navigate(Route.GROUP_LIST);

        Scene scene = new Scene(root, 1360, 880);
        applyStylesheet(scene);

        stage.setTitle("TripSync — 함께 만드는 여행 계획");
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        Database.close();
    }

    private void applyStylesheet(Scene scene) {
        URL css = getClass().getResource(STYLESHEET);
        if (css == null) {
            System.err.println("[경고] 스타일시트를 찾지 못했습니다: " + STYLESHEET);
            return;
        }
        scene.getStylesheets().add(css.toExternalForm());
    }

    private void showFatalError(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("TripSync 를 시작할 수 없습니다");
        alert.setHeaderText("데이터 저장소를 준비하지 못했습니다.");

        Label detail = new Label(error.getMessage()
                + "\n\n저장 위치에 쓰기 권한이 있는지 확인한 뒤 다시 실행해 주세요.");
        detail.setWrapText(true);
        detail.setMaxWidth(420);
        alert.getDialogPane().setContent(detail);

        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
