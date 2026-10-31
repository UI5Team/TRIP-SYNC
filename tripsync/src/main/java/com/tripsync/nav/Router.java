package com.tripsync.nav;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public final class Router {

    private static BorderPane shell;

    private static AppBar appBar;

    private static final Deque<Entry> history = new ArrayDeque<>();

    private static Entry current;

    private static Runnable navigationListener;

    private Router() {

    }

    public static void init(BorderPane appShell, AppBar bar) {
        shell = appShell;
        appBar = bar;
        history.clear();
        current = null;
    }

    public static void setNavigationListener(Runnable listener) {
        navigationListener = listener;
    }

    public static Route currentRoute() {
        return current == null ? null : current.route();
    }

    public static void navigate(Route route) {
        navigate(route, null);
    }

    public static void navigate(Route route, Object param) {
        requireInitialized();
        if (current != null) {
            history.push(current);
        }
        show(new Entry(route, param));
    }

    public static void back() {
        requireInitialized();
        if (history.isEmpty()) {
            current = null;
            show(new Entry(Route.GROUP_LIST, null));
            return;
        }
        show(history.pop());
    }

    public static void reload() {
        requireInitialized();
        if (current != null) {
            show(current);
        }
    }

    public static void resetTo(Route route, Object param) {
        requireInitialized();
        history.clear();
        current = null;
        show(new Entry(route, param));
    }

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

            if (navigationListener != null) {
                navigationListener.run();
            }

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

    private record Entry(Route route, Object param) {
    }
}
