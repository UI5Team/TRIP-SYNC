package com.tripsync.ui;

import java.util.List;
import java.util.Random;

import com.tripsync.model.Category;
import com.tripsync.model.ScheduleItem;
import com.tripsync.util.Dates;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class MapPanel extends StackPane {

    private static final Color WATER = Color.web("#C6DFF5");
    private static final Color LAND = Color.web("#EDF2F7");
    private static final Color BLOCK = Color.web("#E2E9F1");
    private static final Color PARK = Color.web("#D8EBDA");
    private static final Color ROAD = Color.web("#FFFFFF");
    private static final Color ROUTE = Color.web("#2F6FED");
    private static final Color LABEL_BG = Color.web("#FFFFFF");
    private static final Color LABEL_INK = Color.web("#1B2538");
    private static final Color LABEL_SUB = Color.web("#7C8894");

    private final Canvas canvas = new Canvas();
    private final Pane canvasHolder = new Pane(canvas);
    private final Label emptyLabel = new Label("이 날짜에 표시할 장소가 없습니다");

    private List<ScheduleItem> items = List.of();
    private boolean showRoute = true;
    private double zoom = 1.0;

    public MapPanel() {
        getStyleClass().add("map-panel");

        canvas.widthProperty().bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());
        canvas.widthProperty().addListener((obs, old, now) -> draw());
        canvas.heightProperty().addListener((obs, old, now) -> draw());

        emptyLabel.getStyleClass().add("map-empty");
        emptyLabel.setVisible(false);

        getChildren().addAll(canvasHolder, emptyLabel, buildZoomControls(), buildRouteToggle());
        StackPane.setAlignment(emptyLabel, Pos.CENTER);
    }

    public void setItems(List<ScheduleItem> scheduleItems) {
        items = scheduleItems == null ? List.of() : scheduleItems;
        emptyLabel.setVisible(items.isEmpty());
        draw();
    }

    private VBox buildZoomControls() {
        VBox box = new VBox(1, zoomButton("＋", 0.2), zoomButton("－", -0.2), zoomButton("⊙", 0));
        box.getStyleClass().add("map-zoom");
        StackPane.setAlignment(box, Pos.TOP_LEFT);
        StackPane.setMargin(box, new Insets(14));
        box.setMaxSize(34, 104);
        return box;
    }

    private Label zoomButton(String text, double delta) {
        Label button = new Label(text);
        button.getStyleClass().add("map-zoom-button");
        button.setAlignment(Pos.CENTER);
        button.setMinSize(34, 34);
        button.setPrefSize(34, 34);
        button.setOnMouseClicked(e -> {
            zoom = delta == 0 ? 1.0 : Math.max(0.6, Math.min(1.8, zoom + delta));
            draw();
        });
        return button;
    }

    private HBox buildRouteToggle() {
        Label dash = new Label("┄┄");
        dash.getStyleClass().add("map-toggle-dash");

        Label text = new Label("이동 경로 보기");
        text.getStyleClass().add("map-toggle-text");

        Label knob = new Label();
        knob.getStyleClass().addAll("map-toggle-knob", "map-toggle-knob-on");

        StackPane track = new StackPane(knob);
        track.getStyleClass().addAll("map-toggle-track", "map-toggle-track-on");
        track.setMinSize(38, 22);
        track.setPrefSize(38, 22);
        StackPane.setAlignment(knob, Pos.CENTER_RIGHT);

        HBox toggle = new HBox(9, dash, text, track);
        toggle.setAlignment(Pos.CENTER_LEFT);
        toggle.getStyleClass().add("map-toggle");
        toggle.setMaxSize(HBox.USE_PREF_SIZE, HBox.USE_PREF_SIZE);
        StackPane.setAlignment(toggle, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toggle, new Insets(14));

        toggle.setOnMouseClicked(e -> {
            showRoute = !showRoute;
            track.getStyleClass().remove("map-toggle-track-on");
            knob.getStyleClass().remove("map-toggle-knob-on");
            if (showRoute) {
                track.getStyleClass().add("map-toggle-track-on");
                knob.getStyleClass().add("map-toggle-knob-on");
            }
            StackPane.setAlignment(knob, showRoute ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            draw();
        });
        return toggle;
    }

    private void draw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        drawBase(g, w, h);

        if (items.isEmpty()) {
            return;
        }

        double[][] points = layout(w, h);

        if (showRoute && points.length > 1) {
            drawRoute(g, points);
        }
        for (int i = 0; i < points.length; i++) {
            drawPin(g, points[i][0], points[i][1], i + 1, items.get(i));
        }
        for (int i = 0; i < points.length; i++) {
            drawLabel(g, points[i][0], points[i][1], items.get(i), w);
        }
    }

    private void drawBase(GraphicsContext g, double w, double h) {
        g.setFill(LAND);
        g.fillRect(0, 0, w, h);

        Random random = new Random(20261031L);
        double step = 54 * zoom;

        g.setFill(BLOCK);
        for (double y = -step; y < h + step; y += step) {
            for (double x = -step; x < w + step; x += step) {
                if (random.nextInt(10) < 6) {
                    double pad = 5 + random.nextInt(6);
                    g.fillRoundRect(x + pad, y + pad, step - pad * 2, step - pad * 2, 5, 5);
                }
            }
        }

        g.setFill(PARK);
        g.fillRoundRect(w * 0.70, h * 0.60, w * 0.22, h * 0.26, 16, 16);
        g.fillRoundRect(w * 0.06, h * 0.08, w * 0.14, h * 0.16, 14, 14);

        g.setStroke(WATER);
        g.setLineWidth(22 * zoom);
        g.beginPath();
        g.moveTo(-20, h * 0.26);
        g.bezierCurveTo(w * 0.28, h * 0.12, w * 0.34, h * 0.52, w * 0.52, h * 0.44);
        g.bezierCurveTo(w * 0.72, h * 0.35, w * 0.82, h * 0.16, w + 20, h * 0.20);
        g.stroke();

        g.setStroke(ROAD);
        g.setLineWidth(7 * zoom);
        for (double y = h * 0.18; y < h; y += h * 0.22) {
            g.strokeLine(0, y, w, y + 12);
        }
        for (double x = w * 0.14; x < w; x += w * 0.19) {
            g.strokeLine(x, 0, x - 16, h);
        }
    }

    private double[][] layout(double w, double h) {
        int n = items.size();
        double[][] points = new double[n][2];

        double marginX = Math.min(130, w * 0.18);
        double marginY = Math.min(90, h * 0.20);
        double usableW = Math.max(40, w - marginX * 2);
        double usableH = Math.max(40, h - marginY * 2);

        for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0.5 : (double) i / (n - 1);
            Random seeded = new Random(items.get(i).id() * 7919L);
            double wobbleX = (seeded.nextDouble() - 0.5) * usableW * 0.12;
            double wobbleY = (seeded.nextDouble() - 0.5) * usableH * 0.10;

            double x = marginX + usableW * t + wobbleX;
            double y = marginY + usableH * (0.5 + 0.38 * Math.sin(t * Math.PI * 1.35 + 0.4)) + wobbleY;

            points[i][0] = clamp(x, marginX * 0.5, w - marginX * 0.5);
            points[i][1] = clamp(y, marginY * 0.6, h - marginY * 0.6);
        }
        return points;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawRoute(GraphicsContext g, double[][] points) {
        g.setStroke(ROUTE);
        g.setLineWidth(3.4);
        g.setLineDashes(9, 7);
        g.beginPath();
        g.moveTo(points[0][0], points[0][1]);
        for (int i = 1; i < points.length; i++) {
            double midX = (points[i - 1][0] + points[i][0]) / 2;
            g.bezierCurveTo(midX, points[i - 1][1], midX, points[i][1], points[i][0], points[i][1]);
        }
        g.stroke();
        g.setLineDashes(null);
    }

    private void drawPin(GraphicsContext g, double x, double y, int number, ScheduleItem item) {
        double r = 15;
        g.setFill(Color.rgb(20, 40, 80, 0.18));
        g.fillOval(x - r + 1, y - r + 3, r * 2, r * 2);

        g.setFill(pinColor(item.category()));
        g.fillOval(x - r, y - r, r * 2, r * 2);

        g.setStroke(Color.WHITE);
        g.setLineWidth(2.5);
        g.strokeOval(x - r, y - r, r * 2, r * 2);

        g.setFill(Color.WHITE);
        g.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 13));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(javafx.geometry.VPos.CENTER);
        g.fillText(String.valueOf(number), x, y + 0.5);
    }

    private void drawLabel(GraphicsContext g, double x, double y, ScheduleItem item, double canvasWidth) {
        String title = item.placeName();
        String sub = item.hasTime() ? Dates.time(item.time()) : "시간 미정";

        Font titleFont = Font.font("Malgun Gothic", FontWeight.BOLD, 12);
        Font subFont = Font.font("Malgun Gothic", 11);

        double titleWidth = textWidth(title, titleFont);
        double subWidth = textWidth(sub, subFont);
        double boxW = Math.max(titleWidth, subWidth) + 22;
        double boxH = 42;

        double boxX = x + 20;
        if (boxX + boxW > canvasWidth - 8) {
            boxX = x - 20 - boxW;
        }
        double boxY = y - boxH / 2;

        g.setFill(Color.rgb(20, 40, 80, 0.14));
        g.fillRoundRect(boxX + 1, boxY + 2, boxW, boxH, 10, 10);

        g.setFill(LABEL_BG);
        g.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);

        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(javafx.geometry.VPos.CENTER);

        g.setFill(LABEL_INK);
        g.setFont(titleFont);
        g.fillText(title, boxX + 11, boxY + 14);

        g.setFill(LABEL_SUB);
        g.setFont(subFont);
        g.fillText(sub, boxX + 11, boxY + 30);
    }

    private double textWidth(String text, Font font) {
        Text helper = new Text(text);
        helper.setFont(font);
        return helper.getLayoutBounds().getWidth();
    }

    private Color pinColor(Category category) {
        return switch (category) {
            case SIGHT -> Color.web("#E0558F");
            case FOOD -> Color.web("#E08A1E");
            case CAFE -> Color.web("#8B5CE0");
            case STAY -> Color.web("#2EA362");
            case MOVE -> Color.web("#2F6FED");
            case SHOP -> Color.web("#C78B14");
            case ETC -> Color.web("#8491A6");
        };
    }
}
