package com.pawura.ui;

import com.pawura.model.ElephantSighting;
import com.pawura.model.Location;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.List;

/**
 * MapView – a schematic SVG-style canvas map of Sri Lanka
 * that plots elephant sighting locations as pins.
 *
 * Uses JavaFX Pane with drawn shapes (no external map library needed).
 * Coordinate system: Sri Lanka bounding box
 *   Lat  5.9° – 9.9°  (bottom to top)
 *   Lon 79.7° – 81.9° (left to right)
 */
public class MapView {

    // Sri Lanka bounding box
    private static final double MIN_LAT =  5.9;
    private static final double MAX_LAT =  9.9;
    private static final double MIN_LON = 79.7;
    private static final double MAX_LON = 81.9;

    private static final double MAP_W   = 440;
    private static final double MAP_H   = 560;

    private final List<ElephantSighting> sightings;
    private final BorderPane root;

    public MapView(List<ElephantSighting> sightings) {
        this.sightings = sightings;
        root = buildUI();
    }

    private BorderPane buildUI() {
        BorderPane bp = new BorderPane();
        bp.getStyleClass().add("panel");
        bp.setPadding(new Insets(28, 32, 28, 32));

        Label title = new Label("🗺  Sighting Map — Sri Lanka");
        title.getStyleClass().add("panel-title");

        Pane canvas = buildCanvas();
        StackPane mapWrapper = new StackPane(canvas);
        mapWrapper.setStyle("-fx-background-color: #e8f4f8; -fx-border-color: #aac; " +
                            "-fx-border-width: 1.5; -fx-border-radius: 6;");
        mapWrapper.setMaxWidth(MAP_W + 4);
        mapWrapper.setMaxHeight(MAP_H + 4);

        Label legend = buildLegend();

        VBox content = new VBox(14, title, mapWrapper, legend);
        content.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        bp.setCenter(scroll);
        return bp;
    }

    private Pane buildCanvas() {
        Pane pane = new Pane();
        pane.setPrefSize(MAP_W, MAP_H);
        pane.setStyle("-fx-background-color: #ddeeff;");

        // Draw rough Sri Lanka outline (simplified polygon points)
        drawIslandOutline(pane);

        // Plot district markers for known NP locations
        drawDistrictLabels(pane);

        // Plot sighting pins
        for (ElephantSighting s : sightings) {
            Location loc = s.getLocation();
            if (loc == null) continue;
            double x = lonToX(loc.getLongitude());
            double y = latToY(loc.getLatitude());
            if (x < 0 || x > MAP_W || y < 0 || y > MAP_H) continue;

            // Outer ring
            Circle ring = new Circle(x, y, 10 + Math.min(s.getElephantCount(), 40) * 0.3);
            ring.setFill(Color.rgb(255, 165, 0, 0.25));
            ring.setStroke(Color.DARKORANGE);
            ring.setStrokeWidth(1);

            // Pin dot
            Circle pin = new Circle(x, y, 6);
            pin.setFill(s.isVerified() ? Color.FORESTGREEN : Color.ORANGE);
            pin.setStroke(Color.WHITE);
            pin.setStrokeWidth(1.5);

            // Label
            Label lbl = new Label(loc.getName() != null
                ? loc.getName() : "?");
            lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #333; " +
                         "-fx-background-color: rgba(255,255,255,0.75); " +
                         "-fx-padding: 1 3 1 3; -fx-background-radius: 3;");
            lbl.setLayoutX(x + 9);
            lbl.setLayoutY(y - 8);

            // Tooltip
            javafx.scene.control.Tooltip tt = new javafx.scene.control.Tooltip(
                s.getDisplaySummary());
            javafx.scene.control.Tooltip.install(pin, tt);
            javafx.scene.control.Tooltip.install(ring, tt);

            pane.getChildren().addAll(ring, pin, lbl);
        }
        return pane;
    }

    private void drawIslandOutline(Pane pane) {
        // Approximate key coastal points of Sri Lanka (lat, lon pairs)
        double[][] outline = {
            {9.82, 80.22}, {9.68, 80.02}, {9.83, 80.28}, {9.70, 80.60},
            {9.35, 80.83}, {8.57, 81.23}, {8.09, 81.84}, {7.37, 81.86},
            {6.82, 81.65}, {6.05, 80.24}, {6.02, 80.06}, {5.92, 80.55},
            {6.13, 80.89}, {6.49, 81.05}, {7.47, 80.07}, {7.97, 79.87},
            {8.56, 79.86}, {8.97, 79.88}, {9.56, 80.08}, {9.82, 80.22}
        };

        for (int i = 0; i < outline.length - 1; i++) {
            Line line = new Line(
                lonToX(outline[i][1]),   latToY(outline[i][0]),
                lonToX(outline[i+1][1]), latToY(outline[i+1][0]));
            line.setStroke(Color.rgb(80, 100, 180, 0.6));
            line.setStrokeWidth(1.5);
            pane.getChildren().add(line);
        }
    }

    private void drawDistrictLabels(Pane pane) {
        String[][] districts = {
            {"Colombo",      "6.93", "79.85"},
            {"Kandy",        "7.30", "80.64"},
            {"Polonnaruwa",  "7.94", "81.00"},
            {"Trincomalee",  "8.59", "81.23"},
            {"Hambantota",   "6.12", "81.12"},
        };
        for (String[] d : districts) {
            double x = lonToX(Double.parseDouble(d[2]));
            double y = latToY(Double.parseDouble(d[1]));
            Label lbl = new Label(d[0]);
            lbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #556; -fx-opacity: 0.7;");
            lbl.setLayoutX(x - 20);
            lbl.setLayoutY(y);
            pane.getChildren().add(lbl);
        }
    }

    private Label buildLegend() {
        Label verified   = new Label("● Verified sighting");
        verified.setStyle("-fx-text-fill: forestgreen; -fx-font-size: 12px;");
        Label unverified = new Label("● Unverified sighting");
        unverified.setStyle("-fx-text-fill: darkorange; -fx-font-size: 12px;");
        Label size = new Label("○ Ring size ∝ elephant count");
        size.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        HBox legend = new HBox(20, verified, unverified, size);
        legend.setAlignment(Pos.CENTER_LEFT);
        return new Label("") {{
            setGraphic(legend);
        }};
    }

    // ── Coordinate conversions ────────────────────────────────────────────────

    private double lonToX(double lon) {
        return (lon - MIN_LON) / (MAX_LON - MIN_LON) * MAP_W;
    }

    private double latToY(double lat) {
        // Invert: higher lat → smaller y
        return (1.0 - (lat - MIN_LAT) / (MAX_LAT - MIN_LAT)) * MAP_H;
    }

    public Parent getRoot() { return root; }
}
