package com.tripsync.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 목록이 비어 있을 때 보여주는 안내 영역.
 *
 * <p>설계서 10장은 모임·여행·일정·위시리스트가 비었을 때 모두 "안내 문구 + 다음 행동 버튼"을
 * 보여주라고 정하고 있다. 화면마다 따로 만들면 문구와 간격이 제각각이 되므로
 * 공통 컴포넌트 하나로 통일한다.</p>
 *
 * <p>사용 예:
 * <pre>{@code
 * EmptyState.of("아직 참여 중인 모임이 없습니다",
 *               "모임을 만들거나 초대 코드를 입력해 참여해 보세요.")
 *           .primary("모임 만들기", () -> Router.navigate(Route.GROUP_CREATE))
 *           .build();
 * }</pre>
 */
public final class EmptyState {

    private final String title;
    private final String description;
    private final HBox actions = new HBox(10);

    private EmptyState(String title, String description) {
        this.title = title;
        this.description = description;
        this.actions.setAlignment(Pos.CENTER);
    }

    /** 제목과 설명으로 빈 상태를 만들기 시작한다. */
    public static EmptyState of(String title, String description) {
        return new EmptyState(title, description);
    }

    /** 강조된 주 행동 버튼을 추가한다. */
    public EmptyState primary(String text, Runnable action) {
        return addButton(text, action, "button-primary");
    }

    /** 보조 행동 버튼을 추가한다. */
    public EmptyState secondary(String text, Runnable action) {
        return addButton(text, action, "button-ghost");
    }

    private EmptyState addButton(String text, Runnable action, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setOnAction(e -> action.run());
        actions.getChildren().add(button);
        return this;
    }

    /** 실제 화면에 붙일 노드를 만든다. */
    public Node build() {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("empty-description");
        descLabel.setWrapText(true);

        VBox box = new VBox(8, titleLabel, descLabel);
        box.getStyleClass().add("empty-state");
        box.setAlignment(Pos.CENTER);

        if (!actions.getChildren().isEmpty()) {
            VBox.setMargin(actions, new javafx.geometry.Insets(10, 0, 0, 0));
            box.getChildren().add(actions);
        }
        return box;
    }
}
