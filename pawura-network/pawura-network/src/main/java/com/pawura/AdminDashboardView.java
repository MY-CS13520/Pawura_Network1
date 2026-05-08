package com.pawura;

import com.pawura.model.ActivityLog;
import com.pawura.model.User;
import com.pawura.service.AuthenticationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.chart.*;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import javafx.stage.FileChooser;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AdminDashboardView – The primary management interface for administrators.
 * Integrated as a unique tab in the dashboard shell.
 */
public class AdminDashboardView extends VBox {

    private final UserStore userStore = new UserStore();
    private final SightingStore sightingStore = new SightingStore();
    private final ActivityLogStore logStore = new ActivityLogStore();
    private final AuthenticationService authService = new AuthenticationService();

    private LineChart<String, Number> userGrowthChart;
    private BarChart<String, Number> peakUsageChart;
    private AreaChart<String, Number> sightingTrendChart;

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
        Tab reportTab = new Tab("📊 Analytics Report", buildAnalyticsReport());

        tabs.getTabs().addAll(List.of(sightingTab, userTab, logsTab, reportTab));
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().add(tabs);
    }

    private ScrollPane buildAnalyticsReport() {
        VBox container = new VBox(30);
        container.setPadding(new Insets(25));
        container.setAlignment(Pos.TOP_CENTER);

        // Export Actions
        HBox exportActions = new HBox(15);
        exportActions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCsv = new Button("📄 Export CSV");
        btnCsv.getStyleClass().add("secondary-button");
        btnCsv.setOnAction(e -> exportToCsv());

        Button btnPdf = new Button("📕 Export PDF");
        btnPdf.getStyleClass().add("primary-button");
        btnPdf.setOnAction(e -> exportToPdf());

        exportActions.getChildren().addAll(btnCsv, btnPdf);
        container.getChildren().add(exportActions);

        // 1. App Usage Over Time (User Growth)
        container.getChildren().add(createSectionTitle("Community Growth Trend"));
        this.userGrowthChart = buildUserGrowthChart();
        container.getChildren().add(userGrowthChart);

        // 2. Peak Usage (Activity by Hour)
        container.getChildren().add(createSectionTitle("Peak App Usage (Hourly)"));
        this.peakUsageChart = buildPeakUsageChart();
        container.getChildren().add(peakUsageChart);

        // 3. Elephant Sightings Over Time
        container.getChildren().add(createSectionTitle("Elephant Sighting Activity"));
        this.sightingTrendChart = buildSightingTrendChart();
        container.getChildren().add(sightingTrendChart);

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("news-scroll");
        return scroll;
    }

    private Label createSectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    private LineChart<String, Number> buildUserGrowthChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Registration Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Users Joined");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("User Onboarding Over Time");
        lineChart.setPrefHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("New Registrations");
        
        userStore.getRegistrationTrends().forEach((date, count) -> {
            series.getData().add(new XYChart.Data<>(date, count));
        });

        lineChart.getData().add(series);
        return lineChart;
    }

    private BarChart<String, Number> buildPeakUsageChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Hour of Day (0-23)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Activity Volume");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("System Peak Usage Patterns");
        barChart.setPrefHeight(300);
        barChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        
        logStore.getActivityByHour().forEach((hour, count) -> {
            series.getData().add(new XYChart.Data<>(String.format("%02d:00", hour), count));
        });

        barChart.getData().add(series);
        return barChart;
    }

    private AreaChart<String, Number> buildSightingTrendChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Sighting Date");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Report Count");

        AreaChart<String, Number> areaChart = new AreaChart<>(xAxis, yAxis);
        areaChart.setTitle("Elephant Sightings Timeline");
        areaChart.setPrefHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Verified & Pending Reports");

        sightingStore.getSightingTrends().forEach((date, count) -> {
            series.getData().add(new XYChart.Data<>(date, count));
        });

        areaChart.getData().add(series);
        return areaChart;
    }

    private void exportToCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Analytics (CSV)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("pawura_analytics_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv");
        
        File file = fc.showSaveDialog(this.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("Pawura Network Analytics Export - " + LocalDateTime.now());
            writer.println();

            writer.println("COMMUNITY GROWTH TREND");
            writer.println("Date,Registrations");
            userStore.getRegistrationTrends().forEach((date, count) -> writer.println(date + "," + count));
            writer.println();

            writer.println("PEAK USAGE (HOURLY)");
            writer.println("Hour,Activity Count");
            logStore.getActivityByHour().forEach((hour, count) -> writer.println(String.format("%02d:00", hour) + "," + count));
            writer.println();

            writer.println("ELEPHANT SIGHTING TRENDS");
            writer.println("Date,Sighting Count");
            sightingStore.getSightingTrends().forEach((date, count) -> writer.println(date + "," + count));

            logAdminAction("Exported CSV analytics report", "SYSTEM");
            showAlert(Alert.AlertType.INFORMATION, "Export Success", "Analytics data exported to CSV successfully.");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Export Error", "Failed to write CSV: " + e.getMessage());
        }
    }

    private void exportToPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Analytics (PDF)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("pawura_analytics_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        
        File file = fc.showSaveDialog(this.getScene().getWindow());
        if (file == null) return;

        try (FileOutputStream fos = new FileOutputStream(file)) {
            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();

            com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            document.add(new Paragraph("Pawura Network - Administrator Analytics", titleFont));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
            document.add(new Paragraph(" "));

            if (userGrowthChart != null) {
                document.add(new Paragraph("Community Growth Chart", headerFont));
                document.add(nodeToPdfImage(userGrowthChart));
                document.add(new Paragraph(" "));
            }
            addPdfSection(document, "Community Growth Trend Data", headerFont, "Date", "Registrations", userStore.getRegistrationTrends());
            document.add(new Paragraph(" "));
            
            if (peakUsageChart != null) {
                document.add(new Paragraph("Peak Usage Patterns Chart", headerFont));
                document.add(nodeToPdfImage(peakUsageChart));
                document.add(new Paragraph(" "));
            }
            // Format hourly data for the table
            java.util.Map<String, String> hourlyMap = new java.util.LinkedHashMap<>();
            logStore.getActivityByHour().forEach((h, c) -> hourlyMap.put(String.format("%02d:00", h), String.valueOf(c)));
            addPdfSection(document, "Peak Usage Patterns Data", headerFont, "Hour", "Activities", hourlyMap);
            document.add(new Paragraph(" "));

            if (sightingTrendChart != null) {
                document.add(new Paragraph("Elephant Sighting Activity Chart", headerFont));
                document.add(nodeToPdfImage(sightingTrendChart));
                document.add(new Paragraph(" "));
            }
            addPdfSection(document, "Elephant Sighting Trends Data", headerFont, "Date", "Reports", sightingStore.getSightingTrends());

            document.close();
            logAdminAction("Exported PDF analytics report", "SYSTEM");
            showAlert(Alert.AlertType.INFORMATION, "Export Success", "Analytics report exported to PDF successfully.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Error", "Failed to generate PDF: " + e.getMessage());
        }
    }

    private void addPdfSection(Document doc, String title, com.lowagie.text.Font font, String col1, String col2, Map<?, ?> data) throws DocumentException {
        doc.add(new Paragraph(title, font));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.addCell(col1);
        table.addCell(col2);
        data.forEach((k, v) -> {
            table.addCell(k.toString());
            table.addCell(v.toString());
        });
        doc.add(table);
    }

    private com.lowagie.text.Image nodeToPdfImage(javafx.scene.Node node) throws Exception {
        WritableImage wi = node.snapshot(null, null);
        java.awt.image.BufferedImage bi = SwingFXUtils.fromFXImage(wi, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", baos);
        com.lowagie.text.Image img = com.lowagie.text.Image.getInstance(baos.toByteArray());
        img.scaleToFit(520, 300);
        img.setAlignment(com.lowagie.text.Image.ALIGN_CENTER);
        return img;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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
                    Sighting s = getTableRow() != null ? getTableRow().getItem() : null;
                    if (s == null) return;
                    s.setVerified(true);
                    if (sightingStore.update(s)) {
                        logAdminAction("Verified sighting #" + s.getId(), "SIGHTING");
                        table.refresh();
                    }
                });
                btnDelete.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
                btnDelete.setOnAction(e -> {
                    Sighting s = getTableRow() != null ? getTableRow().getItem() : null;
                    if (s == null) return;
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
        nameCol.setCellValueFactory(f -> new SimpleStringProperty(f.getValue() != null ? f.getValue().getFullName() : "—"));

        TableColumn<User, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(f -> new SimpleStringProperty(f.getValue() != null && f.getValue().getRole() != null ? f.getValue().getRole().name() : "—"));

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(f -> new SimpleStringProperty(f.getValue() != null ? (f.getValue().isActive() ? "Active" : "Suspended") : "—"));

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
                    User u = getTableRow() != null ? getTableRow().getItem() : null;
                    if (u != null && roleChoice.getValue() != null && !roleChoice.getValue().equals(u.getRole())) {
                        u.setRole(roleChoice.getValue());
                        logAdminAction("Changed role for '" + u.getUsername() + "' to " + u.getRole(), "USER");
                        if (userStore.update(u)) table.refresh();
                    }
                });

                btnStatus.setOnAction(e -> {
                    User u = getTableRow() != null ? getTableRow().getItem() : null;
                    if (u != null) {
                        u.setActive(!u.isActive());
                        String status = u.isActive() ? "Activated" : "Suspended";
                        logAdminAction(status + " account: " + u.getUsername(), "USER");
                        if (userStore.update(u)) table.refresh();
                    }
                });

                btnDelete.setOnAction(e -> {
                    User u = getTableRow() != null ? getTableRow().getItem() : null;
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
                User u = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || u == null) {
                    setGraphic(null);
                } else {
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