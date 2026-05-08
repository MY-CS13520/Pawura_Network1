package com.pawura.ui;

import com.pawura.AdminDashboardView;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.pawura.app.PawuraApp;
import com.pawura.model.ElephantSighting;
import com.pawura.model.Location;
import com.pawura.model.NewsArticle;
import com.pawura.model.Prediction;
import com.pawura.model.User;
import com.pawura.service.AuthenticationService;
import com.pawura.service.NewsService;
import com.pawura.service.PredictionService;
import com.pawura.service.SightingService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * DashboardView – main window after login.
 * Left sidebar navigation; right content area swaps between panels:
 *   Overview  |  Sightings  |  Map  |  Predictions  |  News  |  (Admin)
 */
public class DashboardView {

    private final Stage                stage;
    private final User                 user;
    private final AuthenticationService authService;

    private final SightingService  sightingService  = new SightingService();
    private final PredictionService predictionService = new PredictionService();
    private final NewsService      newsService      = new NewsService();

    private BorderPane root;
    private StackPane  contentArea;

    private final Set<Integer> notifiedDangerIds = new HashSet<>();
    private final ObservableList<String> notifications = FXCollections.observableArrayList();

    private double xOffset = 0;
    private double yOffset = 0;

    public DashboardView(Stage stage, User user, AuthenticationService authService) {
        this.stage       = stage;
        this.user        = user;
        this.authService = authService;
        root = buildUI();
        startDangerPolling();
    }

    // ── Root Layout ───────────────────────────────────────────────────────────

    private BorderPane buildUI() {
        BorderPane bp = new BorderPane();
        bp.getStyleClass().add("root");
        bp.setTop(createTitleBar());
        bp.setLeft(buildSidebar());
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");
        bp.setCenter(contentArea);

        stage.maximizedProperty().addListener((obs, wasMax, isMax) -> {
        if (isMax) {
            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getVisualBounds();
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        }
    });
        bp.prefWidthProperty().bind(stage.widthProperty());
        bp.prefHeightProperty().bind(stage.heightProperty());
        contentArea.prefWidthProperty().bind(stage.widthProperty());
        contentArea.prefHeightProperty().bind(stage.heightProperty());

        // Dragging logic for the dashboard
        bp.getTop().setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        bp.getTop().setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        showOverview();
        return bp;
    }

    /**
     * Background polling to check for nearby danger sightings every 30 seconds.
     */
    private void startDangerPolling() {
        Timeline dangerCheck = new Timeline(new KeyFrame(javafx.util.Duration.seconds(30), e -> {
            if (user.getHomeLat() == 0 && user.getHomeLon() == 0) return;

            List<ElephantSighting> recentDangers = sightingService.findRecentDangerSightings(LocalDateTime.now().minusHours(1));
            for (ElephantSighting s : recentDangers) {
                if (!notifiedDangerIds.contains(s.getId())) {
                    double dist = user.getHomeLocation().distanceTo(s.getLocation());
                    if (dist <= user.getNotificationRadius()) {
                        notifiedDangerIds.add(s.getId());
                        String msg = String.format("🚨 Near your home: Elephant spotted at %s (%.1f km away)", 
                            s.getLocation().getName(), dist);
                        Platform.runLater(() -> {
                            notifications.add(0, msg);
                            if (notifications.size() > 10) notifications.remove(10);
                        });
                        showDangerNotification(s, dist);
                    }
                }
            }
        }));
        dangerCheck.setCycleCount(Timeline.INDEFINITE);
        dangerCheck.play();
    }

    private void showDangerNotification(ElephantSighting s, double distance) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("🚨 HIGH DANGER ALERT");
            alert.setHeaderText("Elephant Spotted Nearby!");
            alert.setContentText(String.format(
                "A high-danger sighting was reported %.1f km from your home location at %s.\n\nCaption: %s\n\nPlease stay safe!",
                distance, s.getLocation().getName(), s.getCaption()));
            alert.show();
        });
    }

    private HBox createTitleBar() {
        Button bellBtn = new Button("🔔");
        bellBtn.getStyleClass().add("window-control-btn");
        bellBtn.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Notifications", 
            notifications.isEmpty() ? "No new notifications." : String.join("\n\n", notifications)));

        Button minBtn = new Button("—");
        minBtn.getStyleClass().add("window-control-btn");
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button sizeBtn = new Button("▢");
        sizeBtn.getStyleClass().add("window-control-btn");
        sizeBtn.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().addAll("window-control-btn", "close-btn");
        closeBtn.setOnAction(e -> stage.close());

        HBox controls = new HBox(bellBtn, minBtn, sizeBtn, closeBtn);
        controls.setAlignment(Pos.TOP_RIGHT);
        controls.getStyleClass().add("window-header");
        return controls;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        Label logo = new Label("🐘 Pawura");
        logo.getStyleClass().add("sidebar-logo");

        Label userLabel = new Label(user.getFullName() != null
            ? user.getFullName() : user.getUsername());
        userLabel.getStyleClass().add("sidebar-user");
        userLabel.setWrapText(true);

        Label roleLabel = new Label(user.getRole().name());
        roleLabel.getStyleClass().add("sidebar-role");

        VBox userBox = new VBox(4, userLabel, roleLabel);
        userBox.setPadding(new Insets(0, 12, 16, 12));

        Separator sep = new Separator();

        Button btnOverview    = navButton("🏠  Overview",      this::showOverview);
        Button btnSightings   = navButton("🔭  Sightings",     this::showSightings);
        Button btnMap         = navButton("🗺  Map View",      this::showMap);
        Button btnPredictions = navButton("📊  Predictions",   this::showPredictions);
        Button btnNews        = navButton("📰  News",          this::showNews);
        Button btnSettings    = navButton("⚙  Settings",      this::showSettings);

        VBox nav = new VBox(4, btnOverview, btnSightings, btnMap,
                            btnPredictions, btnNews, btnSettings);
        nav.setPadding(new Insets(8, 8, 8, 8));

        // Admin-only button
        if (user.isAdmin()) {
            Button btnAdmin = navButton("⚙  Admin Panel", this::showAdmin);
            btnAdmin.getStyleClass().add("nav-admin");
            nav.getChildren().add(btnAdmin);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("⬅  Logout");
        btnLogout.getStyleClass().add("logout-button");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setOnAction(e -> handleLogout());

        VBox sidebar = new VBox(12, logo, userBox, sep, nav, spacer, btnLogout);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(20, 8, 16, 8));
        sidebar.setPrefWidth(200);
        return sidebar;
    }

    private Button navButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    // ── Overview Panel ────────────────────────────────────────────────────────

    private void showOverview() {
        long sightingCount = sightingService.findAll().size();
        long newsCount     = newsService.findAll().size();
        long predCount     = predictionService.findAll().size();

        VBox panel = new VBox(20);
        panel.setPadding(new Insets(32));
        panel.getStyleClass().add("panel");

        Label heading = new Label("Dashboard Overview");
        heading.getStyleClass().add("panel-title");

        Label welcome = new Label("Welcome back, " +
            (user.getFullName() != null ? user.getFullName() : user.getUsername()) + "!");
        welcome.getStyleClass().add("panel-subtitle");

        HBox stats = new HBox(25,
            statCard("🐘", String.valueOf(sightingCount), "Total Sightings"),
            statCard("📍", String.valueOf(predCount),     "Predictions"),
            statCard("📰", String.valueOf(newsCount),     "News Articles")
        );
        stats.setAlignment(Pos.CENTER);

        Label feedTitle = new Label("Public Sighting Feed");
        feedTitle.getStyleClass().add("section-title");

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 12;");
        refreshBtn.setOnAction(e -> showOverview());

        HBox feedHeader = new HBox(10, feedTitle, refreshBtn);
        feedHeader.setAlignment(Pos.CENTER_LEFT);

        // Notifications warning section
        VBox notificationArea = new VBox(10);
        if (!notifications.isEmpty()) {
            for (String note : notifications) {
                HBox bar = new HBox(12);
                bar.getStyleClass().add("notification-bar");
                bar.setAlignment(Pos.CENTER_LEFT);
                
                Label lbl = new Label(note);
                lbl.getStyleClass().add("notification-text");
                lbl.setWrapText(true);
                HBox.setHgrow(lbl, Priority.ALWAYS);
                
                Button dismiss = new Button("✕");
                dismiss.getStyleClass().add("window-control-btn");
                dismiss.setStyle("-fx-text-fill: -color-accent; -fx-font-weight: bold;");
                dismiss.setOnAction(e -> {
                    notifications.remove(note);
                    showOverview();
                });
                
                bar.getChildren().addAll(lbl, dismiss);
                notificationArea.getChildren().add(bar);
            }
        }

        FlowPane feedContainer = new FlowPane();
        feedContainer.setHgap(20);
        feedContainer.setVgap(20);
        feedContainer.setPadding(new Insets(10));
        feedContainer.setAlignment(Pos.TOP_CENTER);
        
        List<ElephantSighting> feedItems = sightingService.findAll().stream()
                .filter(ElephantSighting::isVisible)
                .toList();

        for (ElephantSighting s : feedItems) {
            feedContainer.getChildren().add(createFeedTile(s));
        }

        ScrollPane scroll = new ScrollPane(feedContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);
        scroll.getStyleClass().add("news-scroll");

        panel.getChildren().addAll(heading, welcome);
        if (!notificationArea.getChildren().isEmpty()) {
            panel.getChildren().add(notificationArea);
        }
        panel.getChildren().addAll(stats, feedHeader, scroll);
        setContent(panel);
    }

    private javafx.scene.layout.Pane createFeedTile(ElephantSighting s) {
        VBox tile = new VBox(10);
        tile.getStyleClass().addAll("news-tile", "sighting-card");
        tile.setPrefWidth(350); // Consistent card width, slightly larger than news tile (280)
        tile.setMinWidth(350);
        tile.setMaxWidth(350);
        tile.setPadding(new Insets(15));

        // Header with User and Danger Badge
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label userLbl = new Label("👤 " + (s.getReportedBy() != null ? s.getReportedBy().getFullName() : "Anonymous"));
        userLbl.getStyleClass().add("news-tile-meta");
        header.getChildren().add(userLbl);

        // Content
        Label caption = new Label(s.getCaption() != null && !s.getCaption().isEmpty() ? s.getCaption() : "New sighting reported.");
        caption.getStyleClass().add("news-tile-title");
        caption.setWrapText(true);

        tile.getChildren().addAll(header, caption);

        // Image
        if (s.getImagePath() != null && !s.getImagePath().isEmpty()) {
            try {
                // "Convert" image size to card dimensions (320x200 inside 350px card)
                Image img = new Image(s.getImagePath(), 320, 200, false, true, true);
                ImageView iv = new ImageView(img);
                iv.setSmooth(true);
                Rectangle clip = new Rectangle();
                clip.setWidth(320);
                clip.setHeight(200);
                clip.setArcWidth(24);
                clip.setArcHeight(24);
                iv.setClip(clip);
                tile.getChildren().add(iv);
            } catch (Exception ignored) {}
        }

        // Footer info
        Label details = new Label(String.format("📍 %s | 🐘 %d | %s", 
            s.getLocation() != null ? s.getLocation().getName() : "Unknown", 
            s.getElephantCount(), 
            s.getBehaviour() != null ? s.getBehaviour().name() : "N/A"));
        details.getStyleClass().add("stat-label");

        // Danger Toggle Button (Facebook Like style)
        Button dangerBtn = new Button("🚨 Alert");
        dangerBtn.getStyleClass().add("nav-button");
        if (s.isDanger()) {
            dangerBtn.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
        } else {
            dangerBtn.setStyle("-fx-text-fill: #6b7280;");
        }

        dangerBtn.setOnAction(e -> {
            boolean newStatus = !s.isDanger();
            if (sightingService.updateDanger(s.getId(), newStatus)) {
                showOverview(); // Refresh the feed to show updated state
            }
        });
        
        HBox footer = new HBox(15, details, dangerBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        // Admin Moderation Button
        if (user.isAdmin()) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button takeDownBtn = new Button("Take Down");
            takeDownBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 10px;");
            takeDownBtn.setOnAction(e -> {
                if (sightingService.delete(s.getId())) {
                    showOverview(); // Refresh feed
                }
            });
            footer.getChildren().addAll(spacer, takeDownBtn);
        }

        tile.getChildren().add(footer);
        return wrapWithShimmer(tile, 350, 450, 14);
    }

    private javafx.scene.layout.Pane statCard(String emoji, String value, String label) {
        Label emojiLbl = new Label(emoji);
        emojiLbl.getStyleClass().add("stat-emoji");
        Label valLbl   = new Label(value);
        valLbl.getStyleClass().add("stat-value");
        Label lblLbl   = new Label(label);
        lblLbl.getStyleClass().add("stat-label");
        VBox card = new VBox(4, emojiLbl, valLbl, lblLbl);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(16, 28, 16, 28));
        return wrapWithShimmer(card, 220, 160, 16);
    }

    // ── Sightings Panel ───────────────────────────────────────────────────────

    private void showSightings() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(32));
        panel.getStyleClass().add("panel");

        Label heading = new Label("Elephant Sightings");
        heading.getStyleClass().add("panel-title");

        Button addBtn = new Button("+ Report New Sighting");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> openSightingForm());

        HBox topBar = new HBox(addBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);

        TableView<ElephantSighting> table = buildSightingsTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        panel.getChildren().addAll(heading, topBar, table);
        setContent(panel);
    }

    private TableView<ElephantSighting> buildSightingsTable() {
        TableView<ElephantSighting> tv = new TableView<>();
        tv.getStyleClass().add("styled-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<ElephantSighting, String> locCol = new TableColumn<>("Location");
        locCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getLocation() != null
                    ? d.getValue().getLocation().getName() : "—"));

        TableColumn<ElephantSighting, String> countCol = new TableColumn<>("🐘 Count");
        countCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                String.valueOf(d.getValue().getElephantCount())));
        countCol.setMaxWidth(90);

        TableColumn<ElephantSighting, String> behCol = new TableColumn<>("Behaviour");
        behCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getBehaviour() != null
                    ? d.getValue().getBehaviour().name() : "—"));

        TableColumn<ElephantSighting, String> dateCol = new TableColumn<>("Sighted At");
        dateCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().getSightedAt().toLocalDate().toString()));

        TableColumn<ElephantSighting, String> verCol = new TableColumn<>("Verified");
        verCol.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().isVerified() ? "✔" : "⏳"));
        verCol.setMaxWidth(80);

        tv.getColumns().addAll(List.of(locCol, countCol, behCol, dateCol, verCol));
        tv.setItems(FXCollections.observableArrayList(sightingService.findAll()));

        // Detail on click
        tv.setOnMouseClicked(e -> {
            ElephantSighting sel = tv.getSelectionModel().getSelectedItem();
            if (sel != null) showDetail("Sighting Detail", sel.getDetailText());
        });

        return tv;
    }

    // ── Map Panel ─────────────────────────────────────────────────────────────

    private void showMap() {
        MapView mapView = new MapView(sightingService.findAll());
        setContent(mapView.getRoot());
    }

    // ── Predictions Panel ─────────────────────────────────────────────────────

    private void showPredictions() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(32));
        panel.getStyleClass().add("panel");

        Label heading = new Label("Movement Predictions");
        heading.getStyleClass().add("panel-title");

        Button genBtn = new Button("⚡ Generate Prediction");
        genBtn.getStyleClass().add("primary-button");
        genBtn.setOnAction(e -> generatePrediction());

        HBox topBar = new HBox(genBtn);
        topBar.setAlignment(Pos.CENTER_RIGHT);

        ListView<String> list = new ListView<>();
        list.getStyleClass().add("styled-list");
        predictionService.findAll().stream()
            .map(Prediction::getDisplaySummary)
            .forEach(s -> list.getItems().add(s));
        VBox.setVgrow(list, Priority.ALWAYS);

        panel.getChildren().addAll(heading, topBar, list);
        setContent(panel);
    }

    private void generatePrediction() {
        List<Location> history = sightingService.findAll().stream()
            .filter(s -> s.getLocation() != null)
            .map(ElephantSighting::getLocation)
            .limit(5)
            .toList();

        Prediction p = predictionService.predict(history);
        predictionService.save(p);
        showAlert(Alert.AlertType.INFORMATION, "Prediction Generated",
            p.getDisplaySummary() + "\n\n" + p.getDetailText());
        showPredictions();
    }

    // ── News Panel ────────────────────────────────────────────────────────────

    private void showNews() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(32));
        panel.getStyleClass().add("panel");

        Label heading = new Label("News & Updates");
        heading.getStyleClass().add("panel-title");

        FlowPane grid = new FlowPane();
        grid.getStyleClass().add("news-grid");
        grid.setHgap(24);
        grid.setVgap(24);
        grid.setPadding(new Insets(10, 0, 10, 0));

        for (NewsArticle article : newsService.findAll()) {
            grid.getChildren().add(createNewsTile(article));
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("news-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(heading, scroll);
        setContent(panel);
    }

    private javafx.scene.layout.Pane createNewsTile(NewsArticle article) {
        VBox tile = new VBox();
        tile.getStyleClass().add("news-tile");
        tile.setPrefWidth(280);
        tile.setMinWidth(280);
        tile.setMaxWidth(280);

        // Image Section
        ImageView iv = new ImageView();
        iv.setFitWidth(280);
        iv.setFitHeight(160);
        iv.setPreserveRatio(false);
        
        if (article.getImageUrl() != null && !article.getImageUrl().isEmpty()) {
            try {
                iv.setImage(new Image(article.getImageUrl(), true));
            } catch (Exception ignored) {
                // Fallback or placeholder logic could go here
            }
        }

        // Rounded corners for the image top
        Rectangle clip = new Rectangle(280, 160);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        iv.setClip(clip);

        VBox info = new VBox(8);
        info.setPadding(new Insets(16));
        
        Label title = new Label(article.getTitle());
        title.getStyleClass().add("news-tile-title");
        title.setWrapText(true);
        title.setMinHeight(45);

        Label meta = new Label(article.getCategory() + " • " + article.getSource());
        meta.getStyleClass().add("news-tile-meta");

        info.getChildren().addAll(meta, title);
        tile.getChildren().addAll(iv, info);
        tile.setOnMouseClicked(e -> showDetail("Article", article.getDetailText()));
        
        return wrapWithShimmer(tile, 280, 310, 14);
    }

    // ── Admin Panel ───────────────────────────────────────────────────────────

    private void showAdmin() {
        setContent(new AdminDashboardView());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void openSightingForm() {
        SightingFormView form = new SightingFormView(user, sightingService,
            this::showOverview);
        setContent(form.getRoot());
    }

    private void showSettings() {
        setContent(new SettingsView(user, this::showOverview).getRoot());
    }

    private void handleLogout() {
        authService.logout();
        LoginView login = new LoginView(stage);
        Scene scene = new Scene(login.getRoot(),
            PawuraApp.LOGIN_W, PawuraApp.LOGIN_H);
        scene.getStylesheets().add(
            getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
    }

    private StackPane wrapWithShimmer(VBox tile, double w, double h, double radius) {
        StackPane container = new StackPane(tile);
        container.setMaxSize(w, h);
        
        // The Glint Line
        Region glint = new Region();
        glint.getStyleClass().add("glint-line");
        glint.setManaged(false); // Do not affect layout
        glint.setMouseTransparent(true);
        
        // "Overflow: Hidden" Implementation
        Rectangle clip = new Rectangle(w, h);
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        container.setClip(clip);
        
        container.getChildren().add(glint);
        
        // Sweep Animation
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(4), glint);
        tt.setFromX(-w * 1.5);
        tt.setFromY(-h * 1.2);
        tt.setToX(w * 1.5);
        tt.setToY(h * 1.2);
        tt.setCycleCount(javafx.animation.TranslateTransition.INDEFINITE);
        tt.setDelay(javafx.util.Duration.seconds(Math.random() * 3)); // Random offset for natural look
        tt.play();

        // Subtle Scale-Up Animation on Hover
        javafx.animation.ScaleTransition hoverScale = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(250), container);
        container.setOnMouseEntered(e -> {
            hoverScale.stop();
            hoverScale.setToX(1.03);
            hoverScale.setToY(1.03);
            hoverScale.play();
        });
        container.setOnMouseExited(e -> {
            hoverScale.stop();
            hoverScale.setToX(1.0);
            hoverScale.setToY(1.0);
            hoverScale.play();
        });
        
        return container;
    }

    private void setContent(javafx.scene.Node node) {
        node.setOpacity(0);
        contentArea.getChildren().setAll(node);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void showDetail(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        TextArea ta = new TextArea(text);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(560, 260);
        alert.getDialogPane().setContent(ta);
        alert.getDialogPane().setPrefWidth(680);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public Parent getRoot() { return root; }
}
