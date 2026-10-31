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

/**
 * TripSync 진입점.
 *
 * <p>완전 오프라인 JavaFX 데스크톱 앱이다. WebView, 외부 지도 API, WebSocket, 원격 서버를
 * 일절 사용하지 않고 창 하나 안에서 모든 기능이 끝난다. 데이터는 사용자 PC 의
 * SQLite 파일 하나에만 저장된다.</p>
 *
 * <p>창 구조:
 * <pre>
 * StackPane (root)
 *  ├─ BorderPane (shell)
 *  │   ├─ left   : Sidebar — 내비게이션과 내 모임 목록
 *  │   └─ center : BorderPane
 *  │       ├─ top    : AppBar  — 뒤로 버튼과 화면 이름
 *  │       └─ center : 현재 화면 — Router 가 FXML 을 갈아끼운다
 *  └─ StackPane (toastLayer) — 저장 성공 메시지가 잠깐 떠오르는 층
 * </pre>
 */
public class TripSyncApp extends Application {

    private static final String STYLESHEET = "/com/tripsync/css/app.css";

    /** 설계서 8장: 버튼이 잘리거나 입력란이 겹치지 않는 최소 창 크기 */
    private static final double MIN_WIDTH = 1100;
    private static final double MIN_HEIGHT = 700;

    @Override
    public void start(Stage stage) {
        // 화면을 만들기 전에 저장소를 준비한다. 여기서 실패하면 어떤 화면도 의미가 없다.
        // init() 이 아니라 start() 에서 하는 이유: 실패를 Alert 창으로 알리려면
        // JavaFX 툴킷이 올라와 있어야 하기 때문이다.
        try {
            Database.init();
            UserRepository.ensureDefaultUser();
        } catch (DataAccessException e) {
            showFatalError(e);
            Platform.exit();
            return;
        }

        BorderPane content = new BorderPane();

        AppBar appBar = new AppBar();
        content.setTop(appBar);

        Sidebar sidebar = new Sidebar();

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setLeft(sidebar);
        shell.setCenter(content);

        StackPane toastLayer = new StackPane();
        StackPane root = new StackPane(shell, toastLayer);

        Router.init(content, appBar);
        Router.setNavigationListener(sidebar::refresh);
        Toast.init(toastLayer);

        Scene scene = new Scene(root, 1320, 860);
        applyStylesheet(scene);

        // 첫 화면: 시작 화면 겸 모임 목록
        Router.navigate(Route.GROUP_LIST);

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

    /**
     * DB 준비 실패처럼 앱을 켤 수 없는 오류를 사용자에게 알린다.
     * 콘솔 스택 트레이스만 남기면 사용자는 창이 안 뜨는 이유를 알 수 없다.
     */
    static void showFatalError(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("TripSync 를 시작할 수 없습니다");
        alert.setHeaderText("데이터 저장소를 준비하지 못했습니다.");

        Label detail = new Label(
                error.getMessage() + "\n\n"
                        + "저장 위치에 쓰기 권한이 있는지 확인한 뒤 다시 실행해 주세요.");
        detail.setWrapText(true);
        detail.setMaxWidth(420);
        alert.getDialogPane().setContent(detail);

        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
