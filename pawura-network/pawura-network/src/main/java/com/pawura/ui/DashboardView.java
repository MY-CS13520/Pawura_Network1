package com.pawura.ui;

import com.pawura.app.PawuraApp;
import com.pawura.model.*;
import com.pawura.service.AuthenticationService;
import com.pawura.service.NewsService;
import com.pawura.service.PredictionService;
import com.pawura.service.SightingService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.List;

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

    public DashboardView(Stage stage, User user, AuthenticationService authService) {
        this.stage       = stage;
        this.user        = user;
        this.authService = authService;
        root = buildUI();
    }

    // ── Root Layout ───────────────────────────────────────────────────────────

    private BorderPane buildUI() {
        BorderPane bp = new BorderPane();
        bp.setLeft(buildSidebar());
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");
        bp.setCenter(contentArea);
        showOverview();
        return bp;
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

        VBox nav = new VBox(4, btnOverview, btnSightings, btnMap,
                            btnPredictions, btnNews);
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

        HBox stats = new HBox(20,
            statCard("🐘", String.valueOf(sightingCount), "Total Sightings"),
            statCard("📍", String.valueOf(predCount),     "Predictions"),
            statCard("📰", String.valueOf(newsCount),     "News Articles")
        );
        stats.setAlignment(Pos.CENTER_LEFT);

        Label recentTitle = new Label("Recent Sightings");
        recentTitle.getStyleClass().add("section-title");

        ListView<String> recentList = new ListView<>();
        recentList.getStyleClass().add("styled-list");
        List<ElephantSighting> recent = sightingService.findAll();
        recent.stream().limit(5)
              .map(ElephantSighting::getDisplaySummary)
              .forEach(s -> recentList.getItems().add(s));
        recentList.setPrefHeight(180);

        panel.getChildren().addAll(heading, welcome, stats, recentTitle, recentList);
        setContent(panel);
    }

    private VBox statCard(String emoji, String value, String label) {
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
        return card;
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
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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

        tv.getColumns().addAll(locCol, countCol, behCol, dateCol, verCol);
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

        ListView<NewsArticle> list = new ListView<>();
        list.getStyleClass().add("styled-list");
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(NewsArticle item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplaySummary());
            }
        });
        list.setItems(FXCollections.observableArrayList(newsService.findAll()));
        list.setOnMouseClicked(e -> {
            NewsArticle sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) showDetail("Article", sel.getDetailText());
        });
        VBox.setVgrow(list, Priority.ALWAYS);

        panel.getChildren().addAll(heading, list);
        setContent(panel);
    }

    // ── Admin Panel ───────────────────────────────────────────────────────────

    private void showAdmin() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(32));
        panel.getStyleClass().add("panel");

        Label heading = new Label("⚙ Admin Panel");
        heading.getStyleClass().add("panel-title");

        Label info = new Label(
            "Logged in as: " + user.getUsername() +
            "\nRole: "        + user.getRole() +
            "\nEmail: "       + (user.getEmail() != null ? user.getEmail() : "—"));
        info.getStyleClass().add("detail-text");

        Label dbLabel = new Label("Database: pawura.db (SQLite)");
        dbLabel.getStyleClass().add("panel-subtitle");

        panel.getChildren().addAll(heading, info, new Separator(), dbLabel);
        setContent(panel);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void openSightingForm() {
        SightingFormView form = new SightingFormView(stage, user, sightingService,
            () -> showSightings());
        Scene scene = new Scene(form.getRoot(), PawuraApp.WINDOW_W, PawuraApp.WINDOW_H);
        scene.getStylesheets().add(
            getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
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

    private void setContent(javafx.scene.Node node) {
        contentArea.getChildren().setAll(node);
    }

    private void showDetail(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        TextArea ta = new TextArea(text);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(520, 340);
        alert.getDialogPane().setContent(ta);
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
