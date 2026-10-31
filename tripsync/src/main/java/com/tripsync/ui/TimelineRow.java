package com.tripsync.ui;

import com.tripsync.model.ScheduleItem;
import com.tripsync.util.Dates;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class TimelineRow extends HBox {

    public TimelineRow(ScheduleItem item,
                       boolean selected,
                       boolean first,
                       boolean last,
                       Runnable onSelect,
                       Runnable onEdit,
                       Runnable onDelete) {

        getStyleClass().add("timeline-row");
        if (selected) {
            getStyleClass().add("timeline-row-selected");
        }
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);

        getChildren().addAll(
                buildRail(item, first, last),
                buildTime(item),
                buildIcon(item),
                buildTexts(item),
                buildMenu(onEdit, onDelete));

        setOnMouseClicked(e -> onSelect.run());
    }

    private Region buildRail(ScheduleItem item, boolean first, boolean last) {
        Region lineAbove = new Region();
        lineAbove.getStyleClass().add("timeline-line");
        lineAbove.setVisible(!first);
        VBox.setVgrow(lineAbove, Priority.ALWAYS);

        Region lineBelow = new Region();
        lineBelow.getStyleClass().add("timeline-line");
        lineBelow.setVisible(!last);
        VBox.setVgrow(lineBelow, Priority.ALWAYS);

        Circle dot = new Circle(4);
        dot.getStyleClass().addAll("timeline-dot", item.category().styleClass() + "-dot");

        VBox rail = new VBox(lineAbove, dot, lineBelow);
        rail.setAlignment(Pos.CENTER);
        rail.setMinWidth(12);
        rail.setPrefWidth(12);
        return rail;
    }

    private Region buildTime(ScheduleItem item) {
        Label time = new Label(item.hasTime() ? Dates.time(item.time()) : "--:--");
        time.getStyleClass().add(item.hasTime() ? "timeline-time" : "timeline-time-empty");
        time.setMinWidth(44);
        time.setPrefWidth(44);
        return time;
    }

    private Region buildIcon(ScheduleItem item) {
        Label glyph = new Label(item.category().icon());
        glyph.getStyleClass().add("timeline-icon-glyph");

        StackPane chip = new StackPane(glyph);
        chip.getStyleClass().addAll("timeline-icon", item.category().styleClass());
        chip.setMinSize(34, 34);
        chip.setPrefSize(34, 34);
        chip.setMaxSize(34, 34);
        return chip;
    }

    private Region buildTexts(ScheduleItem item) {
        Label title = new Label(item.placeName());
        title.getStyleClass().add("timeline-title");

        VBox texts = new VBox(1, title);
        texts.setAlignment(Pos.CENTER_LEFT);

        if (item.hasMemo()) {
            Label subtitle = new Label(item.memo());
            subtitle.getStyleClass().add("timeline-subtitle");
            texts.getChildren().add(subtitle);
        } else {
            Label subtitle = new Label(item.category().label());
            subtitle.getStyleClass().add("timeline-subtitle");
            texts.getChildren().add(subtitle);
        }

        HBox.setHgrow(texts, Priority.ALWAYS);
        return texts;
    }

    private Region buildMenu(Runnable onEdit, Runnable onDelete) {
        MenuItem edit = new MenuItem("수정");
        edit.setOnAction(e -> onEdit.run());

        MenuItem delete = new MenuItem("삭제");
        delete.setOnAction(e -> onDelete.run());

        MenuButton menu = new MenuButton("⋮");
        menu.getStyleClass().add("timeline-menu");
        menu.getItems().addAll(edit, delete);
        return menu;
    }
}
