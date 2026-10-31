package com.tripsync.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

/**
 * 사용자가 반드시 읽고 답해야 하는 확인 창.
 *
 * <p>토스트와 역할이 다르다. 토스트는 "저장됐습니다"처럼 알리고 지나가면 되는 결과에 쓰고,
 * 이 다이얼로그는 삭제처럼 <b>되돌릴 수 없는 작업</b> 앞에서 흐름을 멈춰 세울 때 쓴다.</p>
 */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * 확인 / 취소를 묻는다.
     *
     * @param title       창 제목
     * @param message     본문. 무엇이 사라지는지 구체적으로 적는다
     * @param confirmText 확인 버튼에 쓸 말. "확인" 대신 "삭제"처럼 행동을 적으면
     *                    사용자가 무엇에 동의하는지 분명해진다
     * @return 사용자가 확인을 눌렀으면 {@code true}
     */
    public static boolean confirm(String title, String message, String confirmText) {
        ButtonType confirmButton = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("취소", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.getButtonTypes().setAll(cancelButton, confirmButton);

        // 기본 Alert 는 긴 문장을 한 줄로 늘려버리므로 줄바꿈되는 라벨을 직접 넣는다
        Label content = new Label(message);
        content.setWrapText(true);
        content.setMaxWidth(380);
        alert.getDialogPane().setContent(content);

        return alert.showAndWait()
                .filter(choice -> choice == confirmButton)
                .isPresent();
    }
}
