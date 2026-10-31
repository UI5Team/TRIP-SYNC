package com.tripsync;

import java.net.URL;

import com.tripsync.nav.AppBar;
import com.tripsync.nav.Route;
import com.tripsync.nav.Router;
import com.tripsync.ui.Toast;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * TripSync 진입점.
 *
 * <p>완전 오프라인 JavaFX 데스크톱 앱이다. WebView, 외부 지도 API, WebSocket, 원격 서버를
 * 일절 사용하지 않고 창 하나 안에서 모든 기능이 끝난다.</p>
 *
 * <p>창 구조:
 * <pre>
 * StackPane (root)
 *  ├─ BorderPane (shell)
 *  │   ├─ top    : AppBar  — 항상 보이는 상단 바
 *  │   └─ center : 현재 화면 — Router 가 FXML 을 갈아끼운다
 *  └─ StackPane (toastLayer) — 저장 성공 메시지 등이 잠깐 떠오르는 층
 * </pre>
 */
public class TripSyncApp extends Application {

    private static final String STYLESHEET = "/com/tripsync/css/app.css";

    /** 설계서 8장: 버튼이 잘리거나 입력란이 겹치지 않는 최소 창 크기 */
    private static final double MIN_WIDTH = 1040;
    private static final double MIN_HEIGHT = 680;

    @Override
    public void start(Stage stage) {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");

        AppBar appBar = new AppBar();
        shell.setTop(appBar);

        StackPane toastLayer = new StackPane();
        StackPane root = new StackPane(shell, toastLayer);

        Router.init(shell, appBar);
        Toast.init(toastLayer);

        Scene scene = new Scene(root, 1280, 820);
        applyStylesheet(scene);

        // 첫 화면: 시작 화면 겸 모임 목록
        Router.navigate(Route.GROUP_LIST);

        stage.setTitle("TripSync — 함께 만드는 여행 계획");
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setScene(scene);
        stage.show();
    }

    private void applyStylesheet(Scene scene) {
        URL css = getClass().getResource(STYLESHEET);
        if (css == null) {
            System.err.println("[경고] 스타일시트를 찾지 못했습니다: " + STYLESHEET);
            return;
        }
        scene.getStylesheets().add(css.toExternalForm());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
