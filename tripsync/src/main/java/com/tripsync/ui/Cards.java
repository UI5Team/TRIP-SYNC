package com.tripsync.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 목록에 반복해서 나오는 카드를 만드는 헬퍼.
 *
 * <p>모임 카드와 여행 카드는 "제목 + 보조 정보 + 오른쪽 꼬리표" 라는 같은 구조를 쓴다.
 * 화면마다 VBox/HBox 를 직접 조립하면 간격과 글자 크기가 조금씩 어긋나므로
 * 여기서 한 번만 정의한다.</p>
 */
public final class Cards {

    private Cards() {
    }

    /**
     * 클릭하면 다음 화면으로 넘어가는 카드.
     *
     * @param title    카드 제목 (모임명, 여행명)
     * @param meta     제목 아래 보조 설명 (기간, 일정 개수 등)
     * @param trailing 오른쪽 끝에 붙일 꼬리표 텍스트. 필요 없으면 null
     * @param onOpen   카드를 클릭했을 때 실행할 동작
     */
    public static Node clickable(String title, String meta, String trailing, Runnable onOpen) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("card-meta");

        VBox texts = new VBox(4, titleLabel, metaLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(14, texts, spacer);
        row.setAlignment(Pos.CENTER_LEFT);

        if (trailing != null && !trailing.isBlank()) {
            Label tag = new Label(trailing);
            tag.getStyleClass().add("invite-code");
            row.getChildren().add(tag);
        }

        Label chevron = new Label("›");
        chevron.getStyleClass().add("card-meta");
        row.getChildren().add(chevron);

        row.getStyleClass().addAll("card", "card-clickable");
        row.setOnMouseClicked(e -> onOpen.run());
        return row;
    }
}
