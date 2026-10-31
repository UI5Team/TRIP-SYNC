package com.tripsync.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.tripsync.data.SessionStore;
import com.tripsync.model.Category;
import com.tripsync.model.ScheduleItem;
import com.tripsync.nav.AppContext;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Toast;
import com.tripsync.util.Dates;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ScheduleEditController implements RoutedController {

    public record Request(long tripId, LocalDate date, Long scheduleId) {
    }

    private static final int MAX_PLACE_LENGTH = 50;

    @FXML private Label headingLabel;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeComboBox;
    @FXML private Label dateErrorLabel;
    @FXML private TextField placeNameField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Label placeErrorLabel;
    @FXML private TextArea memoArea;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    private Long tripId;
    private Long editingId;

    @FXML
    private void initialize() {
        List<String> categoryLabels = new ArrayList<>();
        for (Category category : Category.values()) {
            categoryLabels.add(category.label());
        }
        categoryComboBox.setItems(FXCollections.observableArrayList(categoryLabels));
        timeComboBox.setItems(FXCollections.observableArrayList(buildTimeOptions()));

        cancelButton.setOnAction(e -> Router.back());
        saveButton.setOnAction(e -> handleSave());

        placeNameField.textProperty().addListener((obs, old, now) -> hidePlaceError());
        datePicker.valueProperty().addListener((obs, old, now) -> hideDateError());
    }

    @Override
    public void onEnter(Object param) {
        Request request = param instanceof Request value ? value : null;

        tripId = request != null ? request.tripId() : AppContext.currentTripId().orElse(null);
        editingId = request != null ? request.scheduleId() : null;

        LocalDate defaultDate = request != null && request.date() != null
                ? request.date()
                : LocalDate.now();

        Optional<ScheduleItem> existing = (tripId != null && editingId != null)
                ? SessionStore.scheduleById(tripId, editingId)
                : Optional.empty();

        if (existing.isPresent()) {
            ScheduleItem item = existing.get();
            headingLabel.setText("일정 수정");
            datePicker.setValue(item.date());
            timeComboBox.getEditor().setText(item.hasTime() ? Dates.time(item.time()) : "");
            placeNameField.setText(item.placeName());
            categoryComboBox.setValue(item.category().label());
            memoArea.setText(item.memo() == null ? "" : item.memo());
        } else {
            editingId = null;
            headingLabel.setText("일정 추가");
            datePicker.setValue(defaultDate);
            timeComboBox.setValue(null);
            timeComboBox.getEditor().clear();
            placeNameField.clear();
            categoryComboBox.setValue(Category.SIGHT.label());
            memoArea.clear();
        }

        saveButton.setDisable(tripId == null);
        hideDateError();
        hidePlaceError();
        placeNameField.requestFocus();
    }

    private static List<String> buildTimeOptions() {
        List<String> options = new ArrayList<>();
        for (int hour = 6; hour <= 23; hour++) {
            options.add(String.format("%02d:00", hour));
            options.add(String.format("%02d:30", hour));
        }
        return options;
    }

    private void handleSave() {
        LocalDate date = datePicker.getValue();
        String placeName = placeNameField.getText() == null ? "" : placeNameField.getText().trim();

        if (tripId == null) {
            Toast.warn("여행을 먼저 선택해 주세요.");
            return;
        }
        if (date == null) {
            showDateError("날짜를 선택해 주세요.");
            return;
        }
        if (placeName.isEmpty()) {
            showPlaceError("장소명을 입력해 주세요.");
            return;
        }
        if (placeName.length() > MAX_PLACE_LENGTH) {
            showPlaceError("장소명은 " + MAX_PLACE_LENGTH + "자 이내로 입력해 주세요.");
            return;
        }

        String rawTime = timeComboBox.getEditor().getText();
        LocalTime time = null;
        if (rawTime != null && !rawTime.isBlank()) {
            time = Dates.parseTime(rawTime);
            if (time == null) {
                showDateError("시간은 HH:mm 형식으로 입력해 주세요. (예: 14:30)");
                return;
            }
        }

        Category category = Category.fromLabel(categoryComboBox.getValue());
        String memo = memoArea.getText() == null ? "" : memoArea.getText().trim();

        if (editingId == null) {
            SessionStore.addSchedule(new ScheduleItem(
                    ScheduleItem.UNSAVED, tripId, date, time, placeName, category, memo));
            Toast.success("일정을 추가했습니다. " + placeName);
        } else {
            SessionStore.updateSchedule(new ScheduleItem(
                    editingId, tripId, date, time, placeName, category, memo));
            Toast.success("일정을 수정했습니다. " + placeName);
        }

        Router.back();
    }

    private void showDateError(String message) {
        dateErrorLabel.setText(message);
        dateErrorLabel.setVisible(true);
        dateErrorLabel.setManaged(true);
    }

    private void hideDateError() {
        dateErrorLabel.setVisible(false);
        dateErrorLabel.setManaged(false);
    }

    private void showPlaceError(String message) {
        placeErrorLabel.setText(message);
        placeErrorLabel.setVisible(true);
        placeErrorLabel.setManaged(true);
        if (!placeNameField.getStyleClass().contains("field-error")) {
            placeNameField.getStyleClass().add("field-error");
        }
        placeNameField.requestFocus();
    }

    private void hidePlaceError() {
        placeErrorLabel.setVisible(false);
        placeErrorLabel.setManaged(false);
        placeNameField.getStyleClass().remove("field-error");
    }
}
