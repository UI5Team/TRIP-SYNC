package com.tripsync.ui;

import java.util.ArrayList;
import java.util.List;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.GroupRepository;
import com.tripsync.data.SessionStore;
import com.tripsync.data.TripRepository;
import com.tripsync.data.UserRepository;
import com.tripsync.model.Group;
import com.tripsync.model.ScheduleItem;
import com.tripsync.model.Trip;
import com.tripsync.nav.Route;
import com.tripsync.nav.Router;
import com.tripsync.util.Dates;

import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class SearchBox extends HBox {

    private static final int MAX_RESULTS = 8;

    private final TextField field = new TextField();
    private final ContextMenu results = new ContextMenu();

    public SearchBox() {
        getStyleClass().add("search-box");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setMaxWidth(520);
        HBox.setHgrow(this, Priority.ALWAYS);

        Label icon = new Label("🔍");
        icon.getStyleClass().add("search-icon");

        field.getStyleClass().add("search-field");
        field.setPromptText("장소, 여행지, 키워드 등을 검색해보세요...");
        HBox.setHgrow(field, Priority.ALWAYS);

        results.getStyleClass().add("search-results");
        results.setAutoHide(true);

        field.textProperty().addListener((obs, old, now) -> update(now));
        field.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                results.hide();
            }
        });

        getChildren().addAll(icon, field);
    }

    private void update(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        results.getItems().clear();

        if (query.length() < 1) {
            results.hide();
            return;
        }

        List<Hit> hits = search(query);
        if (hits.isEmpty()) {
            results.getItems().add(messageItem("‘" + query + "’ 에 대한 결과가 없습니다"));
        } else {
            for (Hit hit : hits) {
                results.getItems().add(resultItem(hit));
            }
        }

        if (!results.isShowing()) {
            results.show(field, javafx.geometry.Side.BOTTOM, 0, 6);
        }
    }

    private record Hit(String kind, String title, String meta, Runnable open) {
    }

    private List<Hit> search(String query) {
        String needle = query.toLowerCase();
        List<Hit> hits = new ArrayList<>();

        try {
            List<Group> groups = GroupRepository.findAllForUser(UserRepository.currentUserId());

            for (Group group : groups) {
                if (hits.size() >= MAX_RESULTS) {
                    break;
                }
                if (group.name().toLowerCase().contains(needle)
                        || group.inviteCode().toLowerCase().contains(needle)) {
                    hits.add(new Hit("모임", group.name(),
                            "참여자 " + group.memberCount() + "명 · " + group.inviteCode(),
                            () -> Router.navigate(Route.GROUP_DETAIL, group.id())));
                }
            }

            for (Group group : groups) {
                for (Trip trip : TripRepository.findAllByGroup(group.id())) {
                    if (hits.size() >= MAX_RESULTS) {
                        break;
                    }
                    if (trip.title().toLowerCase().contains(needle)) {
                        hits.add(new Hit("여행", trip.title(),
                                group.name() + " · " + Dates.period(trip.startDate(), trip.endDate()),
                                () -> Router.navigate(Route.TRIP_DETAIL, trip.id())));
                    }
                    for (ScheduleItem item : SessionStore.schedulesOf(trip.id())) {
                        if (hits.size() >= MAX_RESULTS) {
                            break;
                        }
                        if (item.placeName().toLowerCase().contains(needle)) {
                            hits.add(new Hit(item.category().label(), item.placeName(),
                                    trip.title() + " · " + Dates.medium(item.date()),
                                    () -> Router.navigate(Route.TRIP_DETAIL, trip.id())));
                        }
                    }
                }
            }

        } catch (DataAccessException | IllegalStateException e) {
            return List.of();
        }

        return hits;
    }

    private CustomMenuItem resultItem(Hit hit) {
        Label kind = new Label(hit.kind());
        kind.getStyleClass().add("search-result-kind");

        Label title = new Label(hit.title());
        title.getStyleClass().add("search-result-title");

        Label meta = new Label(hit.meta());
        meta.getStyleClass().add("search-result-meta");

        VBox texts = new VBox(1, title, meta);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, texts, spacer, kind);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("search-result");
        row.setPrefWidth(440);

        CustomMenuItem item = new CustomMenuItem(row);
        item.setHideOnClick(true);
        item.setOnAction(e -> {
            field.clear();
            results.hide();
            hit.open().run();
        });
        return item;
    }

    private CustomMenuItem messageItem(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("search-result-empty");
        label.setPrefWidth(440);

        CustomMenuItem item = new CustomMenuItem(label);
        item.setHideOnClick(true);
        return item;
    }
}
