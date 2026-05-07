package com.pawura;

import com.pawura.model.User;
import com.pawura.model.Administrator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * AdminDashboardView – The primary management interface for administrators.
 */
public class AdminDashboardView extends VBox {

    private final UserStore userStore = new UserStore();
    private final SightingStore sightingStore = new SightingStore();

    public AdminDashboardView() {
        setSpacing(25);
        setPadding(new Insets(30));
        getStyleClass().add("content-area");

        buildHeader();
        buildStatsRow();
        buildMainContent();
    }

    private void buildHeader() {
        Label title = new Label("Administrator Control Center");
        title.getStyleClass().add("panel-title");
        
        Label subtitle = new Label("System health and community moderation oversight.");
        subtitle.getStyleClass().add("panel-subtitle");

        VBox header = new VBox(5, title, subtitle);
        getChildren().add(header);
    }

    private void buildStatsRow() {
        HBox stats = new HBox(20);
        stats.setAlignment(Pos.CENTER_LEFT);

        stats.getChildren().addAll(
            createStatCard("👥", "Active Users", String.valueOf(userStore.countActiveUsers())),
            createStatCard("🚨", "Pending Moderation", String.valueOf(sightingStore.countUnverified())),
            createStatCard("📈", "System Health", "Optimal")
        );

        getChildren().add(stats);
    }

    private VBox createStatCard(String emoji, String label, String value) {
        Label iconLbl = new Label(emoji);
        iconLbl.getStyleClass().add("stat-emoji");

        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("stat-value");

        Label descLbl = new Label(label);
        descLbl.getStyleClass().add("stat-label");

        VBox card = new VBox(5, iconLbl, valLbl, descLbl);
        card.getStyleClass().add("stat-card");
        card.setMinWidth(220);
        return card;
    }

    private void buildMainContent() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab sightingTab = new Tab("🚨 Moderation Queue", buildSightingTable());
        Tab userTab = new Tab("👥 User Management", buildUserTable());
        Tab logsTab = new Tab("📜 System Activity", buildLogsView());

        tabs.getTabs().addAll(sightingTab, userTab, logsTab);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().add(tabs);
    }

    private VBox buildSightingTable() {
        TableView<Sighting> table = new TableView<>();
        table.getStyleClass().add("styled-table");

        TableColumn<Sighting, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<Sighting, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getSightedAt().toString()));

        TableColumn<Sighting, Integer> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(new PropertyValueFactory<>("elephantCount"));

        TableColumn<Sighting, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().isVerified() ? "✅ Verified" : "⏳ Pending"));

        TableColumn<Sighting, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnApprove = new Button("Approve");
            private final Button btnDelete = new Button("🗑");
            private final HBox pane = new HBox(5, btnApprove, btnDelete);
            {
                btnApprove.getStyleClass().add("primary-button");
                btnApprove.setStyle("-fx-font-size: 10px; -fx-padding: 4 8;");
                btnApprove.setOnAction(e -> {
                    Sighting s = getTableView().getItems().get(getIndex());
                    s.setVerified(true);
                    if (sightingStore.update(s)) table.refresh();
                });
                btnDelete.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
                btnDelete.setOnAction(e -> {
                    Sighting s = getTableView().getItems().get(getIndex());
                    if (sightingStore.delete(s.getId())) table.getItems().remove(s);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(idCol, dateCol, countCol, statusCol, actionCol);
        table.setItems(FXCollections.observableArrayList(sightingStore.findAll()));

        VBox container = new VBox(10, new Label("Verify community reports before they enter the prediction model:"), table);
        container.setPadding(new Insets(15));
        VBox.setVgrow(table, Priority.ALWAYS);
        return container;
    }

    private VBox buildUserTable() {
        TableView<User> table = new TableView<>();
        table.getStyleClass().add("styled-table");

        TableColumn<User, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getFullName()));

        TableColumn<User, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getRole().name()));

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().isActive() ? "Active" : "Deactivated"));

        table.getColumns().addAll(nameCol, userCol, roleCol, statusCol);
        table.setItems(FXCollections.observableArrayList(userStore.findAll()));

        VBox container = new VBox(10, new Label("Manage community access and user roles:"), table);
        container.setPadding(new Insets(15));
        VBox.setVgrow(table, Priority.ALWAYS);
        return container;
    }

    private VBox buildLogsView() {
        ListView<String> logList = new ListView<>();
        logList.getStyleClass().add("styled-list");
        
        // Mock/System logs since the DB table for logs isn't present yet
        logList.getItems().addAll(
            "[" + java.time.LocalDateTime.now().minusHours(2) + "] SYSTEM: Database integrity check passed.",
            "[" + java.time.LocalDateTime.now().minusMinutes(45) + "] AUTH: User 'ranger1' logged in.",
            "[" + java.time.LocalDateTime.now().minusMinutes(12) + "] DATA: New sighting reported in Kaudulla NP.",
            "[" + java.time.LocalDateTime.now().minusMinutes(5) + "] ADMIN: Statistics recalculated successfully."
        );

        VBox container = new VBox(10, new Label("Recent system-wide events and administrative actions:"), logList);
        container.setPadding(new Insets(15));
        VBox.setVgrow(logList, Priority.ALWAYS);
        return container;
    }
}