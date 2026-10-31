package com.tripsync.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tripsync.data.SessionStore;
import com.tripsync.model.Category;
import com.tripsync.model.WishlistItem;
import com.tripsync.nav.AppContext;
import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Dialogs;
import com.tripsync.ui.EmptyState;
import com.tripsync.ui.Toast;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class WishlistController implements RoutedController {

    private static final int MAX_PLACE_LENGTH = 50;
    private static final double CARD_WIDTH = 210;

    @FXML private Label tripContextLabel;
    @FXML private TextField placeNameField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField noteField;
    @FXML private Button addButton;
    @FXML private Label addErrorLabel;
    @FXML private HBox tabBar;
    @FXML private Button sampleButton;
    @FXML private FlowPane wishGrid;

    private Long tripId;
    private String tripTitle = "여행";
    private Category activeTab;

    @FXML
    private void initialize() {
        List<String> labels = new ArrayList<>();
        for (Category category : Category.values()) {
            labels.add(category.label());
        }
        categoryComboBox.setItems(FXCollections.observableArrayList(labels));
        categoryComboBox.setValue(Category.SIGHT.label());

        addButton.setOnAction(e -> handleAdd());
        placeNameField.setOnAction(e -> handleAdd());
        noteField.setOnAction(e -> handleAdd());
        placeNameField.textProperty().addListener((obs, old, now) -> hideAddError());

        sampleButton.setOnAction(e -> {
            if (tripId == null) {
                return;
            }
            SessionStore.fillSampleWishes(tripId);
            Toast.success("샘플 후보 장소를 채웠습니다.");
            render();
        });
    }

    @Override
    public void onEnter(Object param) {
        tripId = param instanceof Long id ? id : AppContext.currentTripId().orElse(null);
        tripTitle = AppContext.currentTripTitle().orElse("여행");
        activeTab = null;

        boolean ready = tripId != null;
        addButton.setDisable(!ready);
        sampleButton.setDisable(!ready);
        placeNameField.setDisable(!ready);
        noteField.setDisable(!ready);
        categoryComboBox.setDisable(!ready);

        tripContextLabel.setText(ready
                ? "'" + tripTitle + "' 여행의 후보 장소입니다. 모임원이 함께 보는 목록입니다."
                : "여행을 먼저 선택해 주세요.");

        hideAddError();
        render();
    }

    private void render() {
        renderTabs();
        renderGrid();
    }

    private Map<Category, Integer> counts() {
        Map<Category, Integer> counts = new LinkedHashMap<>();
        if (tripId == null) {
            return counts;
        }
        for (WishlistItem item : SessionStore.wishesOf(tripId)) {
            counts.merge(item.category(), 1, Integer::sum);
        }
        return counts;
    }

    private void renderTabs() {
        tabBar.getChildren().clear();
        if (tripId == null) {
            return;
        }

        int total = SessionStore.wishesOf(tripId).size();
        tabBar.getChildren().add(tab("전체 (" + total + ")", null));

        for (Map.Entry<Category, Integer> entry : counts().entrySet()) {
            tabBar.getChildren().add(tab(
                    entry.getKey().label() + " (" + entry.getValue() + ")",
                    entry.getKey()));
        }
    }

    private Node tab(String text, Category category) {
        Label label = new Label(text);
        label.getStyleClass().add("wish-tab-label");

        StackPane chip = new StackPane(label);
        chip.getStyleClass().add("wish-tab");
        if (category == activeTab) {
            chip.getStyleClass().add("wish-tab-active");
        }
        chip.setOnMouseClicked(e -> {
            activeTab = category;
            render();
        });
        return chip;
    }

    private void renderGrid() {
        wishGrid.getChildren().clear();

        if (tripId == null) {
            wishGrid.getChildren().add(EmptyState
                    .of("여행이 선택되지 않았습니다", "모임에서 여행을 연 뒤 위시리스트를 사용할 수 있습니다.")
                    .primary("모임 목록으로", () -> Router.resetTo(Route.GROUP_LIST, null))
                    .build());
            return;
        }

        List<WishlistItem> items = new ArrayList<>();
        for (WishlistItem item : SessionStore.wishesOf(tripId)) {
            if (activeTab == null || item.category() == activeTab) {
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            wishGrid.getChildren().add(EmptyState
                    .of(activeTab == null ? "후보 장소가 없습니다" : "이 분류에 후보가 없습니다",
                            "위 입력란에 가고 싶은 곳의 이름을 적고 추가해 보세요.")
                    .primary("장소 입력하기", () -> placeNameField.requestFocus())
                    .build());
            return;
        }

        for (WishlistItem item : items) {
            wishGrid.getChildren().add(card(item));
        }
    }

    private Node card(WishlistItem item) {
        Label name = new Label(item.placeName());
        name.getStyleClass().add("wish-card-title");
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMaxWidth(CARD_WIDTH - 32);

        Label badge = new Label(item.category().label());
        badge.getStyleClass().addAll("category-badge", item.category().styleClass());

        Label note = new Label(item.hasNote() ? item.note() : "메모 없음");
        note.getStyleClass().add("wish-card-note");
        note.setWrapText(true);
        note.setMaxWidth(CARD_WIDTH - 32);

        Button addToSchedule = new Button("일정에 추가");
        addToSchedule.getStyleClass().add("wish-card-button");
        addToSchedule.setMaxWidth(Double.MAX_VALUE);
        addToSchedule.setOnAction(e -> Router.navigate(Route.SCHEDULE_EDIT,
                new ScheduleEditController.Request(tripId, null, null, item)));

        Button remove = new Button("삭제");
        remove.getStyleClass().add("wish-card-delete");
        remove.setOnAction(e -> handleDelete(item));

        HBox actions = new HBox(7, addToSchedule, remove);
        HBox.setHgrow(addToSchedule, Priority.ALWAYS);
        actions.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(7, name, badge, note, spacer, actions);
        card.getStyleClass().add("wish-card");
        card.setPrefWidth(CARD_WIDTH);
        card.setMinWidth(CARD_WIDTH);
        card.setMinHeight(168);
        return card;
    }

    private void handleAdd() {
        if (tripId == null) {
            Toast.warn("여행을 먼저 선택해 주세요.");
            return;
        }

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
        if (SessionStore.hasWish(tripId, placeName)) {
            showAddError("이미 목록에 있는 장소입니다.");
            return;
        }

        Category category = Category.fromLabel(categoryComboBox.getValue());
        SessionStore.addWish(tripId, placeName, category, note);

        placeNameField.clear();
        noteField.clear();
        hideAddError();
        placeNameField.requestFocus();

        activeTab = null;
        render();
        Toast.success("'" + placeName + "' 을(를) 후보에 추가했습니다.");
    }

    private void handleDelete(WishlistItem item) {
        if (!Dialogs.confirm("후보 장소 삭제",
                "'" + item.placeName() + "' 을(를) 위시리스트에서 삭제합니다.",
                "이미 일정에 등록한 항목은 그대로 남습니다.",
                "삭제")) {
            return;
        }
        SessionStore.deleteWish(tripId, item.id());
        Toast.success("후보 장소를 삭제했습니다.");
        render();
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
}
