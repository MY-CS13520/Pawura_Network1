package com.pawura.ui;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.pawura.model.ElephantSighting;
import com.pawura.model.Location;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * MapView – renders an OpenStreetMap tile-based map of Sri Lanka
 * and overlays elephant sighting pins.
 *
 * Tiles are fetched via HttpURLConnection (not JavaFX Image URL loading)
 * to avoid platform-level URL access restrictions.
 *
 * No API key required. OSM tiles are free and open.
 * Attribution: © OpenStreetMap contributors  (shown on map, required by ODbL)
 */
public class MapView {

    // ── OSM tile servers (round-robin across a/b/c subdomains) ───────────────
    private static final String[] TILE_SERVERS = {
        "https://a.tile.openstreetmap.org/%d/%d/%d.png",
        "https://b.tile.openstreetmap.org/%d/%d/%d.png",
        "https://c.tile.openstreetmap.org/%d/%d/%d.png"
    };
    // OSM policy: identify your app in User-Agent
    private static final String USER_AGENT = "PawuraElephantTracker/1.0 (your@email.com)";

    // ── Viewport ──────────────────────────────────────────────────────────────
    private static final int    TILE_SIZE  = 256;
    private static final int    ZOOM       = 8;
    private static final double CENTER_LAT = 7.85;
    private static final double CENTER_LON = 80.70;
    private static final int    TILES_W    = 4;
    private static final int    TILES_H    = 5;
    private static final double MAP_W      = TILE_SIZE * TILES_W;
    private static final double MAP_H      = TILE_SIZE * TILES_H;

    private final List<ElephantSighting> sightings;
    private final BorderPane root;
    private final double originTileX;
    private final double originTileY;

    private final ExecutorService tileLoader = Executors.newFixedThreadPool(6);
    private int serverIndex = 0; // round-robin counter

    public MapView(List<ElephantSighting> sightings) {
        this.sightings   = sightings;
        double cx        = lonToTileX(CENTER_LON, ZOOM);
        double cy        = latToTileY(CENTER_LAT, ZOOM);
        this.originTileX = cx - TILES_W / 2.0;
        this.originTileY = cy - TILES_H / 2.0;
        this.root        = buildUI();
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private BorderPane buildUI() {
        BorderPane bp = new BorderPane();
        bp.getStyleClass().add("panel");
        bp.setPadding(new Insets(28, 32, 28, 32));

        Label title = new Label("🗺  Sighting Map — Sri Lanka");
        title.getStyleClass().add("panel-title");

        Canvas tileCanvas = new Canvas(MAP_W, MAP_H);
        Canvas pinCanvas  = new Canvas(MAP_W, MAP_H);

        // Draw a loading placeholder immediately
        GraphicsContext gc = tileCanvas.getGraphicsContext2D();
        gc.setFill(Color.rgb(230, 238, 245));
        gc.fillRect(0, 0, MAP_W, MAP_H);
        gc.setFill(Color.SLATEGRAY);
        gc.setFont(Font.font("Arial", 13));
        gc.fillText("Loading map…", MAP_W / 2 - 45, MAP_H / 2);

        Label attribution = new Label("© OpenStreetMap contributors");
        attribution.setStyle(
            "-fx-font-size: 9px; -fx-text-fill: #333;" +
            "-fx-background-color: rgba(255,255,255,0.78);" +
            "-fx-padding: 2 6 2 6;");
        StackPane.setAlignment(attribution, Pos.BOTTOM_RIGHT);

        StackPane mapWrapper = new StackPane(tileCanvas, pinCanvas, attribution);
        mapWrapper.setStyle("-fx-border-color: #aac; -fx-border-width: 1.5; -fx-border-radius: 6;");
        mapWrapper.setMaxWidth(MAP_W + 4);
        mapWrapper.setMaxHeight(MAP_H + 4);

        HBox legend = buildLegend();
        VBox content = new VBox(14, title, mapWrapper, legend);
        content.setAlignment(Pos.TOP_LEFT);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        bp.setCenter(scroll);

        loadTiles(tileCanvas, () -> drawPins(pinCanvas));
        return bp;
    }

    // ── Tile fetching (via HttpURLConnection, no JavaFX URL restrictions) ─────

    private void loadTiles(Canvas canvas, Runnable onComplete) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        int startTileX = (int) Math.floor(originTileX);
        int startTileY = (int) Math.floor(originTileY);
        int maxTile    = (1 << ZOOM);

        // Count valid tiles
        int total = 0;
        for (int dy = 0; dy <= TILES_H; dy++)
            for (int dx = 0; dx <= TILES_W; dx++) {
                int tx = startTileX + dx, ty = startTileY + dy;
                if (tx >= 0 && ty >= 0 && tx < maxTile && ty < maxTile) total++;
            }

        AtomicInteger remaining = new AtomicInteger(total);

        for (int dy = 0; dy <= TILES_H; dy++) {
            for (int dx = 0; dx <= TILES_W; dx++) {
                int tileX = startTileX + dx;
                int tileY = startTileY + dy;
                if (tileX < 0 || tileY < 0 || tileX >= maxTile || tileY >= maxTile) continue;

                double drawX = (tileX - originTileX) * TILE_SIZE;
                double drawY = (tileY - originTileY) * TILE_SIZE;

                // Spread requests across a/b/c subdomains
                String urlStr = String.format(TILE_SERVERS[serverIndex % 3], ZOOM, tileX, tileY);
                serverIndex++;

                tileLoader.submit(() -> {
                    Image img = fetchTile(urlStr);
                    Platform.runLater(() -> {
                        if (img != null && !img.isError()) {
                            gc.drawImage(img, drawX, drawY, TILE_SIZE, TILE_SIZE);
                        } else {
                            // Grey placeholder for failed tiles
                            gc.setFill(Color.rgb(205, 215, 225));
                            gc.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                            gc.setStroke(Color.rgb(180, 190, 200));
                            gc.strokeRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                        }
                        if (remaining.decrementAndGet() <= 0) onComplete.run();
                    });
                });
            }
        }
    }

    /**
     * Fetches a tile PNG via HttpURLConnection and returns it as a JavaFX Image.
     * This bypasses any JavaFX-level URL/network blocking.
     */
    private Image fetchTile(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "image/png,image/*");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.connect();

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    return new Image(is);
                }
            }
        } catch (Exception e) {
            System.err.println("Tile fetch failed: " + urlStr + " — " + e.getMessage());
        }
        return null;
    }

    // ── Pin drawing ───────────────────────────────────────────────────────────

    private void drawPins(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFont(Font.font("Arial", 10));

        for (ElephantSighting s : sightings) {
            Location loc = s.getLocation();
            if (loc == null) continue;

            double x = geoToCanvasX(loc.getLongitude());
            double y = geoToCanvasY(loc.getLatitude());
            if (x < 0 || x > MAP_W || y < 0 || y > MAP_H) continue;

            // Outer ring — size proportional to herd count
            double ringR = 10 + Math.min(s.getElephantCount(), 40) * 0.3;
            gc.setFill(Color.rgb(255, 165, 0, 0.25));
            gc.setStroke(Color.DARKORANGE);
            gc.setLineWidth(1.0);
            gc.fillOval(x - ringR, y - ringR, ringR * 2, ringR * 2);
            gc.strokeOval(x - ringR, y - ringR, ringR * 2, ringR * 2);

            // Pin dot
            double pinR = 6;
            gc.setFill(s.isVerified() ? Color.FORESTGREEN : Color.ORANGE);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.fillOval(x - pinR, y - pinR, pinR * 2, pinR * 2);
            gc.strokeOval(x - pinR, y - pinR, pinR * 2, pinR * 2);

            // Label
            String name = (loc.getName() != null) ? loc.getName() : "?";
            double labelW = name.length() * 6.5 + 8;
            gc.setFill(Color.rgb(255, 255, 255, 0.82));
            gc.fillRoundRect(x + 9, y - 10, labelW, 14, 4, 4);
            gc.setFill(Color.rgb(40, 40, 40));
            gc.fillText(name, x + 13, y + 1);
        }
    }

    // ── Legend ────────────────────────────────────────────────────────────────

    private HBox buildLegend() {
        Label verified   = new Label("● Verified sighting");
        verified.setStyle("-fx-text-fill: forestgreen; -fx-font-size: 12px;");
        Label unverified = new Label("● Unverified sighting");
        unverified.setStyle("-fx-text-fill: darkorange; -fx-font-size: 12px;");
        Label size = new Label("○ Ring size ∝ elephant count");
        size.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        HBox legend = new HBox(20, verified, unverified, size);
        legend.setAlignment(Pos.CENTER_LEFT);
        return legend;
    }

    // ── Coordinate math ───────────────────────────────────────────────────────

    private static double lonToTileX(double lon, int z) {
        return (lon + 180.0) / 360.0 * (1 << z);
    }

    private static double latToTileY(double lat, int z) {
        double r = Math.toRadians(lat);
        return (1.0 - Math.log(Math.tan(r) + 1.0 / Math.cos(r)) / Math.PI) / 2.0 * (1 << z);
    }

    private double geoToCanvasX(double lon) {
        return (lonToTileX(lon, ZOOM) - originTileX) * TILE_SIZE;
    }

    private double geoToCanvasY(double lat) {
        return (latToTileY(lat, ZOOM) - originTileY) * TILE_SIZE;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Call on window close to stop background threads. */
    public void shutdown() {
        tileLoader.shutdownNow();
    }

    public Parent getRoot() { return root; }
}
