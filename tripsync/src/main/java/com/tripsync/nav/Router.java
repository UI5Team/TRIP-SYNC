package com.tripsync.nav;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * 화면 이동을 한 곳에서 관리하는 라우터.
 *
 * <p>각 컨트롤러는 다른 화면의 컨트롤러를 직접 알 필요 없이
 * {@code Router.navigate(Route.TRIP_DETAIL, tripId)} 한 줄로 이동한다.
 * 화면 사이의 의존성이 라우터 하나로 모이므로 화면을 추가·교체하기 쉽다.</p>
 *
 * <p>앱 전체에 창이 하나뿐이고 화면 전환도 하나의 흐름이라, 인스턴스를 들고 다니는 대신
 * static 메서드로 제공한다.</p>
 */
public final class Router {

    /** 화면이 갈아끼워지는 자리(앱 셸의 center) */
    private static BorderPane shell;

    /** 상단 바. 화면 이동마다 제목과 뒤로 버튼 상태를 갱신한다. */
    private static AppBar appBar;

    /** 뒤로 가기용 히스토리 */
    private static final Deque<Entry> history = new ArrayDeque<>();

    /** 현재 보여지고 있는 화면 */
    private static Entry current;

    private Router() {
        // 유틸리티 클래스 — 인스턴스를 만들지 않는다
    }

    /** 라우터가 사용할 셸과 상단 바를 등록한다. 앱 시작 시 한 번만 호출. */
    public static void init(BorderPane appShell, AppBar bar) {
        shell = appShell;
        appBar = bar;
        history.clear();
        current = null;
    }

    /** 파라미터 없이 화면 이동 */
    public static void navigate(Route route) {
        navigate(route, null);
    }

    /**
     * 화면 이동.
     *
     * @param route 이동할 화면
     * @param param 다음 화면에 넘길 값 (없으면 null)
     */
    public static void navigate(Route route, Object param) {
        requireInitialized();
        if (current != null) {
            history.push(current);
        }
        show(new Entry(route, param));
    }

    /**
     * 뒤로 가기. 히스토리가 비어 있으면 모임 목록으로 돌아간다.
     */
    public static void back() {
        requireInitialized();
        if (history.isEmpty()) {
            current = null;
            show(new Entry(Route.GROUP_LIST, null));
            return;
        }
        show(history.pop());
    }

    /**
     * 현재 화면을 파라미터 그대로 다시 불러온다.
     *
     * <p>설계서의 "저장 후 다시 조회" 흐름에 쓴다. 저장이 끝난 뒤 이 메서드를 호출하면
     * 화면이 저장소에서 최신 데이터를 다시 읽어 그린다. 히스토리는 늘어나지 않는다.</p>
     */
    public static void reload() {
        requireInitialized();
        if (current != null) {
            show(current);
        }
    }

    /** 히스토리를 모두 비우고 지정한 화면을 시작점으로 삼는다. */
    public static void resetTo(Route route, Object param) {
        requireInitialized();
        history.clear();
        current = null;
        show(new Entry(route, param));
    }

    /** 실제로 FXML 을 읽어 화면에 붙이는 부분 */
    private static void show(Entry entry) {
        URL location = Router.class.getResource(entry.route.fxmlPath());
        if (location == null) {
            throw new IllegalStateException(
                    "FXML 파일을 찾을 수 없습니다: " + entry.route.fxmlPath()
                            + " (src/main/resources 아래 경로를 확인하세요)");
        }

        try {
            FXMLLoader loader = new FXMLLoader(location);
            Node view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof RoutedController routed) {
                routed.onEnter(entry.param);
            }

            shell.setCenter(view);
            current = entry;
            appBar.update(entry.route.title(), entry.route.isBackEnabled());

        } catch (IOException e) {
            throw new IllegalStateException(
                    "화면을 불러오지 못했습니다: " + entry.route.fxmlPath(), e);
        }
    }

    private static void requireInitialized() {
        if (shell == null || appBar == null) {
            throw new IllegalStateException("Router.init() 이 먼저 호출되어야 합니다.");
        }
    }

    /** 화면 하나와 그 화면에 넘긴 파라미터를 묶어둔 기록 */
    private record Entry(Route route, Object param) {
    }
}
