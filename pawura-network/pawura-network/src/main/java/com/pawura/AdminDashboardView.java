package com.pawura;

import com.pawura.model.ActivityLog;
import com.pawura.model.User;
import com.pawura.service.AuthenticationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.util.List;
import java.time.LocalDateTime;

/**
 * AdminDashboardView – The primary management interface for administrators.
 * Integrated as a unique tab in the dashboard shell.
 */
public class AdminDashboardView extends VBox {

    private final UserStore userStore = new UserStore();
    private final SightingStore sightingStore = new SightingStore();
    private final ActivityLogStore logStore = new ActivityLogStore();
    private final AuthenticationService authService = new AuthenticationService();

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

        tabs.getTabs().addAll(List.of(sightingTab, userTab, logsTab));
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().add(tabs);
    }

    private void logAdminAction(String action, String targetType) {
        authService.getCurrentUser().ifPresent(admin -> {
            ActivityLog log = new ActivityLog();
            log.setAdminId(admin.getId());
            log.setAction(action);
            log.setTargetType(targetType);
            logStore.save(log);
        });
    }

    private VBox buildSightingTable() {
        TableView<Sighting> table = new TableView<>();
        table.getStyleClass().add("styled-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Sighting, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<Sighting, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(f -> new SimpleStringProperty(
            f.getValue().getSightedAt() != null ? f.getValue().getSightedAt().toString() : "—"));

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
                    if (sightingStore.update(s)) {
                        logAdminAction("Verified sighting #" + s.getId(), "SIGHTING");
                        table.refresh();
                    }
                });
                btnDelete.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
                btnDelete.setOnAction(e -> {
                    Sighting s = getTableView().getItems().get(getIndex());
                    if (sightingStore.delete(s.getId())) {
                        logAdminAction("Deleted sighting report #" + s.getId(), "SIGHTING");
                        table.getItems().remove(s);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        table.getColumns().addAll(List.of(idCol, dateCol, countCol, statusCol, actionCol));
        table.setItems(FXCollections.observableArrayList(sightingStore.findAll()));

        VBox container = new VBox(10, new Label("Verify community reports before they enter the prediction model:"), table);
        container.setPadding(new Insets(15));
        VBox.setVgrow(table, Priority.ALWAYS);
        return container;
    }

    private VBox buildUserTable() {
        TableView<User> table = new TableView<>();
        table.getStyleClass().add("styled-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No users found in the system."));

        TableColumn<User, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getFullName()));

        TableColumn<User, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getRole().name()));

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().isActive() ? "Active" : "Suspended"));

        TableColumn<User, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final ChoiceBox<User.Role> roleChoice = new ChoiceBox<>(FXCollections.observableArrayList(User.Role.values()));
            private final Button btnStatus = new Button();
            private final Button btnDelete = new Button("Remove");
            private final HBox pane = new HBox(10, roleChoice, btnStatus, btnDelete);

            {
                pane.setAlignment(Pos.CENTER_LEFT);
                btnDelete.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 10;");
                btnStatus.getStyleClass().add("secondary-button");
                btnStatus.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
                roleChoice.setStyle("-fx-font-size: 11px;");
                roleChoice.setPrefWidth(100);

                roleChoice.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u != null && roleChoice.getValue() != null && u.getRole() != roleChoice.getValue()) {
                        u.setRole(roleChoice.getValue());
                        logAdminAction("Changed role for '" + u.getUsername() + "' to " + u.getRole(), "USER");
                        if (userStore.update(u)) table.refresh();
                    }
                });

                btnStatus.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u != null) {
                        u.setActive(!u.isActive());
                        String status = u.isActive() ? "Activated" : "Suspended";
                        logAdminAction(status + " account: " + u.getUsername(), "USER");
                        if (userStore.update(u)) table.refresh();
                    }
                });

                btnDelete.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u != null) {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Confirm Deletion");
                        confirm.setHeaderText("Remove User: " + u.getUsername());
                        confirm.setContentText("Are you sure you want to permanently delete this user? This action cannot be undone.");
                        
                        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                            if (userStore.delete(u.getId())) {
                                logAdminAction("Permanently removed user: " + u.getUsername(), "USER");
                                table.getItems().remove(u);
                            }
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User u = getTableView().getItems().get(getIndex());
                    roleChoice.setValue(u.getRole());
                    btnStatus.setText(u.isActive() ? "Suspend" : "Activate");
                    setGraphic(pane);
                }
            }
        });

        table.getColumns().addAll(List.of(nameCol, userCol, roleCol, statusCol, actionCol));
        table.setItems(FXCollections.observableArrayList(userStore.findAll()));

        VBox container = new VBox(10, new Label("Manage community access and user roles:"), table);
        container.setPadding(new Insets(15));
        VBox.setVgrow(table, Priority.ALWAYS);
        return container;
    }

    private VBox buildLogsView() {
        ListView<String> logList = new ListView<>();
        logList.getStyleClass().add("styled-list");
        
        logStore.findAll().forEach(log -> logList.getItems().add(log.toString()));
        
        VBox container = new VBox(10, new Label("Recent system events:"), logList);
        container.setPadding(new Insets(15));
        VBox.setVgrow(logList, Priority.ALWAYS);
        return container;
    }
}