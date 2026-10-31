package com.tripsync.controller;

import java.time.LocalDate;
import java.time.LocalTime;

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

/**
 * 화면 6 — 일정 추가 / 수정.
 *
 * <p>추가와 수정은 입력 항목이 같아 화면 하나로 처리하고, 제목과 저장 동작만 바꾼다.
 * 설계서 7장에 따라 날짜와 장소명이 필수, 시작 시간과 메모는 선택이다.</p>
 *
 * <p><b>[1단계]</b> 입력 검증과 복귀 흐름까지 구현한다. 실제 저장은 4단계에서 추가한다.</p>
 */
public class ScheduleEditController implements RoutedController {

    private static final int MAX_PLACE_LENGTH = 50;

    /** 설계서 11장: 장소 분류는 선택 목록으로 제공할 수 있다 */
    private static final String[] CATEGORIES = {
            "관광지", "식당", "카페", "숙소", "쇼핑", "이동", "기타"
    };

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

    @FXML
    private void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList(CATEGORIES));
        timeComboBox.setItems(FXCollections.observableArrayList(buildTimeOptions()));

        cancelButton.setOnAction(e -> Router.back());
        saveButton.setOnAction(e -> handleSave());

        placeNameField.textProperty().addListener((obs, old, now) -> hidePlaceError());
        datePicker.valueProperty().addListener((obs, old, now) -> hideDateError());
    }

    @Override
    public void onEnter(Object param) {
        headingLabel.setText("일정 추가");

        // 여행 상세에서 선택 중이던 날짜를 그대로 받아 미리 채워 둔다
        datePicker.setValue(param instanceof LocalDate date ? date : LocalDate.now());

        timeComboBox.setValue(null);
        timeComboBox.getEditor().clear();
        placeNameField.clear();
        categoryComboBox.setValue(null);
        memoArea.clear();

        hideDateError();
        hidePlaceError();
        placeNameField.requestFocus();
    }

    /** 30분 간격 시간 후보. 직접 입력도 가능하므로 어디까지나 보조 목록이다. */
    private static java.util.List<String> buildTimeOptions() {
        java.util.List<String> options = new java.util.ArrayList<>();
        for (int hour = 6; hour <= 23; hour++) {
            options.add(String.format("%02d:00", hour));
            options.add(String.format("%02d:30", hour));
        }
        return options;
    }

    private void handleSave() {
        LocalDate date = datePicker.getValue();
        String placeName = placeNameField.getText() == null ? "" : placeNameField.getText().trim();

        // 설계서 7장 화면 6: 필수값이 없으면 저장하지 않고 해당 입력란에 안내를 표시
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

        // 시간은 선택 입력이지만, 적었다면 형식이 맞아야 한다
        String rawTime = timeComboBox.getEditor().getText();
        LocalTime time = null;
        if (rawTime != null && !rawTime.isBlank()) {
            time = Dates.parseTime(rawTime);
            if (time == null) {
                showDateError("시간은 HH:mm 형식으로 입력해 주세요. (예: 14:30)");
                return;
            }
        }

        // [4단계에서 추가] 여기서 저장소에 일정을 저장한다.

        String when = time == null ? "시간 미정" : Dates.time(time);
        Toast.success("일정을 저장했습니다. " + placeName + " · " + when);

        // 설계서: 저장/수정 완료 후 여행 상세 화면으로 돌아간다
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
