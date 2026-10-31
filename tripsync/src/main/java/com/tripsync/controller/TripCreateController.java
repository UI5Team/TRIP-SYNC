package com.tripsync.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import com.tripsync.nav.AppContext;
import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Toast;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * 화면 4 — 여행 생성.
 *
 * <p><b>[1단계]</b> 입력 검증(여행명 필수, 종료일 ≥ 시작일)까지 구현한다.
 * 실제 저장은 3단계에서 추가한다.</p>
 */
public class TripCreateController implements RoutedController {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy. M. d.");

    private static final int MAX_TITLE_LENGTH = 40;

    @FXML private Label groupContextLabel;
    @FXML private TextField titleField;
    @FXML private Label titleErrorLabel;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label durationLabel;
    @FXML private Label dateErrorLabel;
    @FXML private Button cancelButton;
    @FXML private Button submitButton;

    /** 어느 모임에 여행을 추가하는지. [3단계에서 저장에 사용] */
    private Long groupId;

    @FXML
    private void initialize() {
        cancelButton.setOnAction(e -> Router.back());
        submitButton.setOnAction(e -> handleSubmit());

        titleField.textProperty().addListener((obs, old, now) -> hideTitleError());

        // 날짜를 고를 때마다 기간을 즉시 계산해 보여준다.
        // 저장을 눌러야 문제를 알려주는 대신, 고르는 순간 결과가 보이게 한다.
        startDatePicker.valueProperty().addListener((obs, old, now) -> onDateChanged());
        endDatePicker.valueProperty().addListener((obs, old, now) -> onDateChanged());
    }

    @Override
    public void onEnter(Object param) {
        groupId = param instanceof Long id ? id : AppContext.currentGroupId().orElse(null);

        groupContextLabel.setText(AppContext.currentGroupName()
                .map(name -> "'" + name + "' 모임에 새 여행을 추가합니다.")
                .orElse("모임에 새 여행을 추가합니다."));

        // 어느 모임인지 모르면 저장할 수 없으므로 아예 막는다
        submitButton.setDisable(groupId == null);

        titleField.clear();
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        durationLabel.setText("");
        hideTitleError();
        hideDateError();
        titleField.requestFocus();
    }

    /** 날짜가 바뀔 때마다 기간을 다시 계산하고 오류를 즉시 반영한다. */
    private void onDateChanged() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            durationLabel.setText("");
            hideDateError();
            return;
        }

        if (end.isBefore(start)) {
            durationLabel.setText("");
            showDateError("종료일은 시작일보다 빠를 수 없습니다.");
            return;
        }

        hideDateError();
        long nights = ChronoUnit.DAYS.between(start, end);
        durationLabel.setText(nights == 0 ? "당일치기" : nights + "박 " + (nights + 1) + "일");
    }

    private void handleSubmit() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        // 설계서 7장 화면 4: 여행명은 비워 둘 수 없다
        if (title.isEmpty()) {
            showTitleError("여행명을 입력해 주세요.");
            return;
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            showTitleError("여행명은 " + MAX_TITLE_LENGTH + "자 이내로 입력해 주세요.");
            return;
        }
        if (start == null || end == null) {
            showDateError("시작일과 종료일을 모두 선택해 주세요.");
            return;
        }
        // 설계서 10장: 종료일이 시작일보다 빠르면 날짜 입력 오류 메시지
        if (end.isBefore(start)) {
            showDateError("종료일은 시작일보다 빠를 수 없습니다.");
            return;
        }

        // [3단계에서 추가] 여기서 저장소에 여행을 저장한다.

        Toast.success("'" + title + "' 여행을 만들었습니다. ("
                + start.format(DATE_FORMAT) + " ~ " + end.format(DATE_FORMAT) + ")");

        // 생성 화면은 히스토리에서 빼고 바로 여행 상세로 넘어간다
        Router.back();
        Router.navigate(Route.TRIP_DETAIL, title);
    }

    private void showTitleError(String message) {
        titleErrorLabel.setText(message);
        titleErrorLabel.setVisible(true);
        titleErrorLabel.setManaged(true);
        if (!titleField.getStyleClass().contains("field-error")) {
            titleField.getStyleClass().add("field-error");
        }
        titleField.requestFocus();
    }

    private void hideTitleError() {
        titleErrorLabel.setVisible(false);
        titleErrorLabel.setManaged(false);
        titleField.getStyleClass().remove("field-error");
    }

    private void showDateError(String message) {
        dateErrorLabel.setText(message);
        dateErrorLabel.setVisible(true);
        dateErrorLabel.setManaged(true);
        markDateError(true);
    }

    private void hideDateError() {
        dateErrorLabel.setVisible(false);
        dateErrorLabel.setManaged(false);
        markDateError(false);
    }

    private void markDateError(boolean hasError) {
        for (DatePicker picker : new DatePicker[]{startDatePicker, endDatePicker}) {
            if (hasError) {
                if (!picker.getStyleClass().contains("field-error")) {
                    picker.getStyleClass().add("field-error");
                }
            } else {
                picker.getStyleClass().remove("field-error");
            }
        }
    }
}
