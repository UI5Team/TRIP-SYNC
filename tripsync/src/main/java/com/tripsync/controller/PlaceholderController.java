package com.tripsync.controller;

import java.util.List;

import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PlaceholderController implements RoutedController {

    public record Spec(String icon, String title, String subtitle, List<String> planned) {
    }

    private static final Spec FRIENDS = new Spec("👥", "친구",
            "모임원을 친구로 등록하고 함께 여행을 계획하는 화면입니다.",
            List.of("모임 참여자 목록 보기",
                    "친구별 일정 레이어 켜고 끄기",
                    "함께 간 여행 기록 모아 보기"));

    private static final Spec SETTINGS = new Spec("⚙", "설정",
            "앱 동작과 데이터 저장 방식을 바꾸는 화면입니다.",
            List.of("기본 프로필 선택",
                    "주 시작 요일과 시간 표기 형식",
                    "로컬 데이터 내보내기 · 초기화"));

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label iconLabel;
    @FXML private Label statusLabel;
    @FXML private Label statusDetailLabel;
    @FXML private VBox plannedBox;
    @FXML private Button homeButton;

    @FXML
    private void initialize() {
        homeButton.setOnAction(e -> Router.resetTo(Route.GROUP_LIST, null));
    }

    @Override
    public void onEnter(Object param) {
        Spec spec = "친구".equals(param) ? FRIENDS : SETTINGS;

        iconLabel.setText(spec.icon());
        titleLabel.setText(spec.title());
        subtitleLabel.setText(spec.subtitle());
        statusLabel.setText("중간 발표 이후 구현 예정");
        statusDetailLabel.setText("이번 발표 범위는 레이아웃과 화면 이동 흐름입니다. "
                + "이 화면은 자리와 진입 경로만 잡아 둔 상태입니다.");

        plannedBox.getChildren().clear();
        for (String item : spec.planned()) {
            plannedBox.getChildren().add(plannedRow(item));
        }
    }

    private HBox plannedRow(String text) {
        Label bullet = new Label("○");
        bullet.getStyleClass().add("placeholder-bullet");

        Label label = new Label(text);
        label.getStyleClass().add("placeholder-item");
        label.setWrapText(true);

        HBox row = new HBox(9, bullet, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
