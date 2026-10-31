package com.tripsync.controller;

import java.util.List;

import com.tripsync.nav.Route;
import com.tripsync.nav.RoutedController;
import com.tripsync.nav.Router;
import com.tripsync.ui.Cards;
import com.tripsync.ui.EmptyState;
import com.tripsync.ui.Toast;
import com.tripsync.util.InviteCode;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

/**
 * 화면 3 — 모임 상세 및 여행 목록.
 *
 * <p><b>[1단계]</b> 넘겨받은 모임 이름을 표시하고 샘플 여행 목록을 보여준다.
 * 3단계에서 저장소 조회로 교체한다.</p>
 */
public class GroupDetailController implements RoutedController {

    @FXML private Label groupNameLabel;
    @FXML private Label inviteCodeLabel;
    @FXML private Button copyCodeButton;
    @FXML private Label memberLabel;
    @FXML private Label tripCountLabel;
    @FXML private Button createTripButton;
    @FXML private VBox tripListBox;

    private String groupName = "모임";

    /** [1단계] 임시 샘플 — 3단계에서 저장소 조회로 대체 */
    private record SampleTrip(String title, String period, int scheduleCount) {
    }

    private static final List<SampleTrip> SAMPLE_TRIPS = List.of(
            new SampleTrip("제주 3박 4일", "2026. 11. 14. ~ 2026. 11. 17.", 7),
            new SampleTrip("부산 주말 여행", "2026. 12. 5. ~ 2026. 12. 6.", 3)
    );

    @FXML
    private void initialize() {
        createTripButton.setOnAction(e -> Router.navigate(Route.TRIP_CREATE, groupName));
        copyCodeButton.setOnAction(e -> copyInviteCode());
    }

    @Override
    public void onEnter(Object param) {
        // 이전 화면이 모임 이름 또는 초대 코드를 넘겨준다
        if (param instanceof String text && !text.isBlank()) {
            groupName = text;
        }
        groupNameLabel.setText(groupName);
        inviteCodeLabel.setText(InviteCode.isValidFormat(groupName)
                ? InviteCode.normalize(groupName)   // 코드로 참여해 들어온 경우
                : "TS4K7Q");                        // [1단계] 샘플 코드

        memberLabel.setText("참여자 3명 · 이 코드를 아는 사람이 모임에 참여할 수 있습니다.");

        renderTrips();
    }

    private void renderTrips() {
        tripListBox.getChildren().clear();

        List<SampleTrip> trips = SAMPLE_TRIPS;
        tripCountLabel.setText(trips.isEmpty() ? "" : trips.size() + "개");

        // 설계서 10장: 모임에 여행이 없을 때의 안내
        if (trips.isEmpty()) {
            tripListBox.getChildren().add(EmptyState
                    .of("아직 여행이 없습니다",
                            "첫 여행을 만들고 날짜별 일정을 채워 보세요.")
                    .primary("새 여행 만들기", () -> Router.navigate(Route.TRIP_CREATE, groupName))
                    .build());
            return;
        }

        for (SampleTrip trip : trips) {
            String meta = trip.period() + " · 일정 " + trip.scheduleCount() + "개";
            tripListBox.getChildren().add(Cards.clickable(
                    trip.title(),
                    meta,
                    null,
                    () -> Router.navigate(Route.TRIP_DETAIL, trip.title())));
        }
    }

    /** 초대 코드를 클립보드에 복사한다. 네트워크를 쓰지 않는 OS 기본 기능이다. */
    private void copyInviteCode() {
        ClipboardContent content = new ClipboardContent();
        content.putString(inviteCodeLabel.getText());
        Clipboard.getSystemClipboard().setContent(content);
        Toast.success("초대 코드를 복사했습니다.");
    }
}
