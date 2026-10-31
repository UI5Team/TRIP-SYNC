package com.tripsync.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.EmptyState;
import com.tripsync.ui.Toast;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 화면 7 — 공동 위시리스트.
 *
 * <p>가고 싶은 곳을 장소명 텍스트로 모아두고, 고른 후보를 날짜·시간과 함께 일정으로
 * 넘긴다. 설계서에 따라 지도 검색, 투표, 좋아요 수는 만들지 않는다.</p>
 *
 * <p><b>[1단계]</b> 추가·삭제·선택이 화면에서 동작하도록 메모리 리스트로 구현한다.
 * 5단계에서 저장소 연동과 실제 일정 등록으로 교체한다.</p>
 */
public class WishlistController implements RoutedController {

    private static final int MAX_PLACE_LENGTH = 50;

    @FXML private Label tripContextLabel;
    @FXML private TextField placeNameField;
    @FXML private TextField noteField;
    @FXML private Button addButton;
    @FXML private Label addErrorLabel;

    @FXML private Label wishCountLabel;
    @FXML private VBox wishListBox;

    @FXML private Label selectedPlaceLabel;
    @FXML private DatePicker registerDatePicker;
    @FXML private ComboBox<String> registerTimeComboBox;
    @FXML private Label registerErrorLabel;
    @FXML private Button registerButton;

    private String tripTitle = "여행";

    /** [1단계] 메모리 보관 — 5단계에서 저장소로 옮긴다 */
    private final List<WishItem> wishes = new ArrayList<>();
    private WishItem selected;

    private record WishItem(String placeName, String note) {
    }

    @FXML
    private void initialize() {
        registerTimeComboBox.setItems(FXCollections.observableArrayList(buildTimeOptions()));

        addButton.setOnAction(e -> handleAdd());
        placeNameField.setOnAction(e -> handleAdd());
        placeNameField.textProperty().addListener((obs, old, now) -> hideAddError());

        registerButton.setOnAction(e -> handleRegister());

        updateSelection(null);
    }

    @Override
    public void onEnter(Object param) {
        if (param instanceof String title && !title.isBlank()) {
            tripTitle = title;
        }
        tripContextLabel.setText("'" + tripTitle + "' 여행의 후보 장소입니다. 모임원이 함께 보는 목록입니다.");
        renderWishes();
    }

    private static List<String> buildTimeOptions() {
        List<String> options = new ArrayList<>();
        for (int hour = 6; hour <= 23; hour++) {
            options.add(String.format("%02d:00", hour));
            options.add(String.format("%02d:30", hour));
        }
        return options;
    }

    private void renderWishes() {
        wishListBox.getChildren().clear();
        wishCountLabel.setText(wishes.isEmpty() ? "" : wishes.size() + "곳");

        if (wishes.isEmpty()) {
            wishListBox.getChildren().add(EmptyState
                    .of("후보 장소가 없습니다",
                            "위 입력란에 가고 싶은 곳의 이름을 적고 추가해 보세요.")
                    .primary("장소 입력하기", () -> placeNameField.requestFocus())
                    .build());
            return;
        }

        for (WishItem wish : wishes) {
            wishListBox.getChildren().add(buildWishRow(wish));
        }
    }

    private Node buildWishRow(WishItem wish) {
        Label name = new Label(wish.placeName());
        name.getStyleClass().add("card-title");

        VBox texts = new VBox(3, name);
        if (!wish.note().isBlank()) {
            Label note = new Label(wish.note());
            note.getStyleClass().add("card-meta");
            note.setWrapText(true);
            texts.getChildren().add(note);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button selectButton = new Button(wish.equals(selected) ? "선택됨" : "선택");
        selectButton.getStyleClass().add(wish.equals(selected) ? "button-secondary" : "button-ghost");
        selectButton.setOnAction(e -> {
            updateSelection(wish);
            renderWishes();
        });

        Button deleteButton = new Button("삭제");
        deleteButton.getStyleClass().add("button-danger");
        deleteButton.setOnAction(e -> handleDelete(wish));

        HBox row = new HBox(10, texts, spacer, selectButton, deleteButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("card");
        return row;
    }

    private void handleAdd() {
        String placeName = placeNameField.getText() == null ? "" : placeNameField.getText().trim();
        String note = noteField.getText() == null ? "" : noteField.getText().trim();

        if (placeName.isEmpty()) {
            showAddError("장소명을 입력해 주세요.");
            return;
        }
        if (placeName.length() > MAX_PLACE_LENGTH) {
            showAddError("장소명은 " + MAX_PLACE_LENGTH + "자 이내로 입력해 주세요.");
            return;
        }
        boolean duplicate = wishes.stream()
                .anyMatch(w -> w.placeName().equalsIgnoreCase(placeName));
        if (duplicate) {
            showAddError("이미 목록에 있는 장소입니다.");
            return;
        }

        // [5단계에서 추가] 저장소에 위시리스트 항목을 저장한다.
        wishes.add(new WishItem(placeName, note));

        placeNameField.clear();
        noteField.clear();
        hideAddError();
        placeNameField.requestFocus();

        renderWishes();
        Toast.success("'" + placeName + "'을(를) 후보에 추가했습니다.");
    }

    private void handleDelete(WishItem wish) {
        wishes.remove(wish);
        if (wish.equals(selected)) {
            updateSelection(null);
        }
        renderWishes();
        Toast.success("'" + wish.placeName() + "'을(를) 목록에서 삭제했습니다.");
    }

    private void updateSelection(WishItem wish) {
        selected = wish;
        selectedPlaceLabel.setText(wish == null ? "선택된 장소 없음" : wish.placeName());
        registerButton.setDisable(wish == null);
        hideRegisterError();
    }

    /** 설계서: '일정에 등록'을 누르면 날짜와 시간 선택을 거쳐 일정 항목으로 생성한다. */
    private void handleRegister() {
        if (selected == null) {
            showRegisterError("먼저 등록할 장소를 선택해 주세요.");
            return;
        }
        LocalDate date = registerDatePicker.getValue();
        if (date == null) {
            showRegisterError("등록할 날짜를 선택해 주세요.");
            return;
        }

        // [5단계에서 추가] 여기서 위시리스트 항목을 일정으로 저장한다.

        String rawTime = registerTimeComboBox.getEditor().getText();
        String when = (rawTime == null || rawTime.isBlank()) ? "시간 미정" : rawTime.trim();

        Toast.success("'" + selected.placeName() + "'을(를) " + date + " " + when + " 일정으로 등록했습니다.");
        Router.back();
    }

    private void showAddError(String message) {
        addErrorLabel.setText(message);
        addErrorLabel.setVisible(true);
        addErrorLabel.setManaged(true);
        if (!placeNameField.getStyleClass().contains("field-error")) {
            placeNameField.getStyleClass().add("field-error");
        }
        placeNameField.requestFocus();
    }

    private void hideAddError() {
        addErrorLabel.setVisible(false);
        addErrorLabel.setManaged(false);
        placeNameField.getStyleClass().remove("field-error");
    }

    private void showRegisterError(String message) {
        registerErrorLabel.setText(message);
        registerErrorLabel.setVisible(true);
        registerErrorLabel.setManaged(true);
    }

    private void hideRegisterError() {
        registerErrorLabel.setVisible(false);
        registerErrorLabel.setManaged(false);
    }
}
