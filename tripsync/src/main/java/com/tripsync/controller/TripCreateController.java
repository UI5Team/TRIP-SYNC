package com.tripsync.controller;

import java.time.LocalDate;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.TripRepository;
import com.tripsync.model.Trip;
import com.tripsync.nav.AppContext;
import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Toast;
import com.tripsync.util.Dates;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * 화면 4 — 여행 생성.
 *
 * <p>설계서 7장: 여행명·시작일·종료일이 모두 필수이고, 종료일은 시작일보다 빠를 수 없다.
 * 날짜를 고르는 즉시 "3박 4일" 이 계산돼 보이므로, 저장을 눌러야 잘못을 알게 되는 일이 없다.</p>
 */
public class TripCreateController implements RoutedController {

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

    /** 어느 모임에 여행을 추가하는지 */
    private Long groupId;

    @FXML
    private void initialize() {
        cancelButton.setOnAction(e -> Router.back());
        submitButton.setOnAction(e -> handleSubmit());

        titleField.textProperty().addListener((obs, old, now) -> hideTitleError());
        titleField.setOnAction(e -> handleSubmit());

        // 날짜를 고를 때마다 기간을 즉시 계산해 보여준다
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
        if (groupId == null) {
            groupContextLabel.setText("모임을 먼저 선택해 주세요. 모임 목록에서 모임을 연 뒤 여행을 만들 수 있습니다.");
            submitButton.setDisable(true);
        } else {
            submitButton.setDisable(false);
        }

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
        durationLabel.setText(Dates.duration(start, end));
    }

    private void handleSubmit() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (groupId == null) {
            Toast.warn("모임을 먼저 선택해 주세요.");
            return;
        }
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

        try {
            Trip created = TripRepository.create(groupId, title, start, end);

            Toast.success("'" + created.title() + "' 여행을 만들었습니다. "
                    + Dates.period(start, end));

            // 생성 화면은 히스토리에서 빼고 바로 여행 상세로 넘어간다
            Router.back();
            Router.navigate(Route.TRIP_DETAIL, created.id());

        } catch (DataAccessException e) {
            // 설계서 10장: 저장 실패 → 입력값을 유지하고 재시도 안내
            showDateError(e.getMessage() + " 입력한 내용은 그대로 두었으니 다시 시도해 주세요.");
        }
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
