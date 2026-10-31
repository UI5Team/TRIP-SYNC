package com.tripsync.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class MonthCalendar extends VBox {

    public enum Mode { DAY, WEEK, MONTH }

    private static final String[] WEEKDAYS = {"일", "월", "화", "수", "목", "금", "토"};

    private final Label monthLabel = new Label();
    private final Button prevButton = new Button("‹");
    private final Button nextButton = new Button("›");
    private final GridPane weekdayRow = new GridPane();
    private final GridPane dayGrid = new GridPane();

    private YearMonth visibleMonth = YearMonth.now();
    private LocalDate selected = LocalDate.now();
    private LocalDate rangeStart;
    private LocalDate rangeEnd;
    private Set<LocalDate> markedDates = Set.of();
    private Mode mode = Mode.MONTH;
    private Consumer<LocalDate> onDateSelected;

    public MonthCalendar() {
        getStyleClass().add("calendar");
        setSpacing(6);

        monthLabel.getStyleClass().add("calendar-month-label");
        prevButton.getStyleClass().add("calendar-nav-button");
        nextButton.getStyleClass().add("calendar-nav-button");

        prevButton.setOnAction(e -> shift(-1));
        nextButton.setOnAction(e -> shift(1));

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        HBox header = new HBox(prevButton, leftSpacer, monthLabel, rightSpacer, nextButton);
        header.setAlignment(Pos.CENTER);
        header.getStyleClass().add("calendar-header");

        applyColumns(weekdayRow);
        applyColumns(dayGrid);
        weekdayRow.getStyleClass().add("calendar-weekday-row");
        dayGrid.getStyleClass().add("calendar-grid");

        for (int i = 0; i < WEEKDAYS.length; i++) {
            Label label = new Label(WEEKDAYS[i]);
            label.getStyleClass().add("calendar-weekday");
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            weekdayRow.add(label, i, 0);
        }

        getChildren().addAll(header, weekdayRow, dayGrid);
    }

    private void applyColumns(GridPane grid) {
        for (int i = 0; i < 7; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 7);
            column.setHalignment(javafx.geometry.HPos.CENTER);
            grid.getColumnConstraints().add(column);
        }
    }

    public void setMode(Mode newMode) {
        mode = newMode;
        render();
    }

    public Mode getMode() {
        return mode;
    }

    public void setSelected(LocalDate date) {
        selected = date;
        if (date != null) {
            visibleMonth = YearMonth.from(date);
        }
        render();
    }

    public void setRange(LocalDate start, LocalDate end) {
        rangeStart = start;
        rangeEnd = end;
        render();
    }

    public void setMarkedDates(Set<LocalDate> dates) {
        markedDates = dates == null ? Set.of() : dates;
        render();
    }

    public void setOnDateSelected(Consumer<LocalDate> listener) {
        onDateSelected = listener;
    }

    private void shift(int amount) {
        if (mode == Mode.MONTH) {
            visibleMonth = visibleMonth.plusMonths(amount);
        } else if (mode == Mode.WEEK) {
            select(selected.plusWeeks(amount));
            return;
        } else {
            select(selected.plusDays(amount));
            return;
        }
        render();
    }

    private void select(LocalDate date) {
        selected = date;
        visibleMonth = YearMonth.from(date);
        render();
        if (onDateSelected != null) {
            onDateSelected.accept(date);
        }
    }

    public final void render() {
        boolean showGrid = mode != Mode.DAY;
        weekdayRow.setVisible(showGrid);
        weekdayRow.setManaged(showGrid);
        dayGrid.setVisible(showGrid);
        dayGrid.setManaged(showGrid);

        monthLabel.setText(mode == Mode.DAY
                ? selected.getYear() + "년 " + selected.getMonthValue() + "월 " + selected.getDayOfMonth() + "일"
                : visibleMonth.getYear() + "년 " + visibleMonth.getMonthValue() + "월");

        dayGrid.getChildren().clear();
        if (!showGrid) {
            return;
        }

        if (mode == Mode.WEEK) {
            LocalDate weekStart = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            for (int i = 0; i < 7; i++) {
                dayGrid.add(buildCell(weekStart.plusDays(i)), i, 0);
            }
            return;
        }

        LocalDate first = visibleMonth.atDay(1);
        LocalDate gridStart = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        for (int week = 0; week < 6; week++) {
            for (int day = 0; day < 7; day++) {
                dayGrid.add(buildCell(gridStart.plusDays((long) week * 7 + day)), day, week);
            }
        }
    }

    private Region buildCell(LocalDate date) {
        Label number = new Label(String.valueOf(date.getDayOfMonth()));
        number.getStyleClass().add("calendar-day-number");

        StackPane bubble = new StackPane(number);
        bubble.getStyleClass().add("calendar-day-bubble");

        VBox cell = new VBox(1, bubble);
        cell.setAlignment(Pos.TOP_CENTER);
        cell.getStyleClass().add("calendar-day");

        if (markedDates.contains(date)) {
            Circle dot = new Circle(2.2);
            dot.getStyleClass().add("calendar-day-dot");
            cell.getChildren().add(dot);
        } else {
            Region placeholder = new Region();
            placeholder.setMinHeight(5);
            placeholder.setPrefHeight(5);
            cell.getChildren().add(placeholder);
        }

        boolean outsideMonth = mode == Mode.MONTH
                && !YearMonth.from(date).equals(visibleMonth);
        boolean outsideRange = rangeStart != null && rangeEnd != null
                && (date.isBefore(rangeStart) || date.isAfter(rangeEnd));

        if (outsideMonth) {
            cell.getStyleClass().add("calendar-day-outside");
        }
        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("calendar-day-today");
        }
        if (date.equals(selected)) {
            cell.getStyleClass().add("calendar-day-selected");
        }

        if (outsideRange) {
            cell.getStyleClass().add("calendar-day-disabled");
        } else {
            cell.setOnMouseClicked(e -> select(date));
        }

        return cell;
    }
}
