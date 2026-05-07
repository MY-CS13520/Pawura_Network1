package com.pawura.ui;

import com.pawura.database.DatabaseManager;
import com.pawura.model.*;
import com.pawura.service.SightingService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SightingFormView – form to report a new elephant sighting.
 * Validates input before persisting via SightingService.
 */
public class SightingFormView {

    private final User                currentUser;
    private final SightingService     sightingService;
    private final Runnable            onSaved;
    private final BorderPane          root;

    // ── Form Fields ───────────────────────────────────────────────────────────
    private ComboBox<String>    locationCombo;
    private List<Location>      locationCache = new ArrayList<>();
    private Spinner<Integer>    countSpinner;
    private ComboBox<ElephantSighting.HerdSize>   herdCombo;
    private ComboBox<ElephantSighting.Behaviour>  behaviourCombo;
    private TextArea            notesArea;
    private Label               errorLabel;

    public SightingFormView(User user, SightingService service, Runnable onSaved) {
        this.currentUser    = user;
        this.sightingService = service;
        this.onSaved        = onSaved;
        root = buildUI();
        loadLocations();
    }

    // ── UI Builder ────────────────────────────────────────────────────────────

    private BorderPane buildUI() {
        BorderPane bp = new BorderPane();
        bp.getStyleClass().add("panel");
        bp.setPadding(new Insets(0));

        // Header
        Label title = new Label("🔭  Report New Sighting");
        title.getStyleClass().add("panel-title");
        HBox header = new HBox(title);
        header.setPadding(new Insets(28, 32, 8, 32));

        // Form grid
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(16, 32, 16, 32));
        ColumnConstraints c1 = new ColumnConstraints(140);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        // Location
        grid.add(fieldLabel("Location *"), 0, 0);
        locationCombo = new ComboBox<>();
        locationCombo.setMaxWidth(Double.MAX_VALUE);
        locationCombo.getStyleClass().add("form-field");
        locationCombo.setPromptText("Select location…");
        grid.add(locationCombo, 1, 0);

        // Count
        grid.add(fieldLabel("Elephant Count *"), 0, 1);
        countSpinner = new Spinner<>(1, 500, 1);
        countSpinner.setEditable(true);
        countSpinner.getStyleClass().add("form-field");
        countSpinner.setMaxWidth(120);
        grid.add(countSpinner, 1, 1);

        // Herd size
        grid.add(fieldLabel("Herd Size"), 0, 2);
        herdCombo = new ComboBox<>(
            FXCollections.observableArrayList(ElephantSighting.HerdSize.values()));
        herdCombo.setMaxWidth(Double.MAX_VALUE);
        herdCombo.getStyleClass().add("form-field");
        grid.add(herdCombo, 1, 2);

        // Behaviour
        grid.add(fieldLabel("Behaviour"), 0, 3);
        behaviourCombo = new ComboBox<>(
            FXCollections.observableArrayList(ElephantSighting.Behaviour.values()));
        behaviourCombo.setMaxWidth(Double.MAX_VALUE);
        behaviourCombo.getStyleClass().add("form-field");
        grid.add(behaviourCombo, 1, 3);

        // Notes
        grid.add(fieldLabel("Notes"), 0, 4);
        notesArea = new TextArea();
        notesArea.setPromptText("Optional observations…");
        notesArea.setPrefRowCount(4);
        notesArea.setWrapText(true);
        notesArea.getStyleClass().add("form-field");
        grid.add(notesArea, 1, 4);

        // Error label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        grid.add(errorLabel, 1, 5);

        // Buttons
        Button saveBtn   = new Button("✔  Save Sighting");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setOnAction(e -> handleSave());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("secondary-button");
        cancelBtn.setOnAction(e -> onSaved.run());

        HBox btnBar = new HBox(12, cancelBtn, saveBtn);
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setPadding(new Insets(8, 32, 28, 32));

        VBox body = new VBox(header, grid, btnBar);
        VBox.setVgrow(grid, Priority.ALWAYS);

        bp.setCenter(body);
        return bp;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    // ── Data Loading ──────────────────────────────────────────────────────────

    private void loadLocations() {
        try (ResultSet rs = DatabaseManager.getInstance().getConnection()
                .createStatement().executeQuery(
                    "SELECT id, name, district, latitude, longitude FROM locations ORDER BY name")) {
            while (rs.next()) {
                Location loc = new Location();
                loc.setId(rs.getInt("id"));
                loc.setName(rs.getString("name"));
                loc.setDistrict(rs.getString("district"));
                try { loc.setLatitude(rs.getDouble("latitude")); }  catch (Exception ignored) {}
                try { loc.setLongitude(rs.getDouble("longitude")); } catch (Exception ignored) {}
                locationCache.add(loc);
                locationCombo.getItems().add(loc.getName() + " (" + loc.getDistrict() + ")");
            }
        } catch (SQLException e) {
            errorLabel.setText("Could not load locations: " + e.getMessage());
            errorLabel.setVisible(true);
        }
    }

    // ── Save Handler ──────────────────────────────────────────────────────────

    private void handleSave() {
        errorLabel.setVisible(false);

        int locIdx = locationCombo.getSelectionModel().getSelectedIndex();
        if (locIdx < 0) {
            showError("Please select a location.");
            return;
        }

        ElephantSighting s = new ElephantSighting();
        s.setLocation(locationCache.get(locIdx));
        s.setReportedBy(currentUser);
        s.setSightedAt(LocalDateTime.now());
        s.setElephantCount(countSpinner.getValue());
        s.setHerdSize(herdCombo.getValue());
        s.setBehaviour(behaviourCombo.getValue());
        s.setNotes(notesArea.getText().trim());
        s.setVerified(false);

        if (sightingService.save(s)) {
            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                "Sighting saved successfully! (ID: " + s.getId() + ")");
            ok.setHeaderText(null);
            ok.showAndWait();
            onSaved.run();
        } else {
            showError("Failed to save sighting. Please try again.");
        }
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
    }

    public Parent getRoot() { return root; }
}
