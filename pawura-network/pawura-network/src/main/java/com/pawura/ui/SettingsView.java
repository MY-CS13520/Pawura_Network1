package com.pawura.ui;

import com.pawura.model.User;
import com.pawura.UserStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsView {
    private final User user;
    private final Runnable onBack;
    private final VBox root;
    private final UserStore userStore = new UserStore();

    private final TextField latField = new TextField();
    private final TextField lonField = new TextField();
    private final Slider radiusSlider = new Slider(5, 100, 20);
    private final Label radiusValueLabel = new Label();

    public SettingsView(User user, Runnable onBack) {
        this.user = user;
        this.onBack = onBack;
        this.root = buildUI();
        populateData();
    }

    private VBox buildUI() {
        VBox vb = new VBox(20);
        vb.setPadding(new Insets(32));
        vb.getStyleClass().add("panel");

        Label title = new Label("Settings & Notifications");
        title.getStyleClass().add("panel-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);

        grid.add(new Label("Home Latitude:"), 0, 0);
        grid.add(latField, 1, 0);
        grid.add(new Label("Home Longitude:"), 0, 1);
        grid.add(lonField, 1, 1);

        grid.add(new Label("Danger Notification Radius (km):"), 0, 2);
        radiusSlider.setShowTickLabels(true);
        radiusSlider.setShowTickMarks(true);
        radiusSlider.valueProperty().addListener((obs, oldVal, newVal) -> 
            radiusValueLabel.setText(String.format("%.0f km", newVal.doubleValue())));
        grid.add(radiusSlider, 1, 2);
        grid.add(radiusValueLabel, 2, 2);

        Button saveBtn = new Button("Save Settings");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setOnAction(e -> handleSave());

        Button backBtn = new Button("Back");
        backBtn.getStyleClass().add("secondary-button");
        backBtn.setOnAction(e -> onBack.run());

        HBox buttons = new HBox(10, backBtn, saveBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        vb.getChildren().addAll(title, grid, buttons);
        return vb;
    }

    private void populateData() {
        latField.setText(String.valueOf(user.getHomeLat()));
        lonField.setText(String.valueOf(user.getHomeLon()));
        radiusSlider.setValue(user.getNotificationRadius());
        radiusValueLabel.setText(String.format("%.0f km", user.getNotificationRadius()));
    }

    private void handleSave() {
        try {
            user.setHomeLat(Double.parseDouble(latField.getText()));
            user.setHomeLon(Double.parseDouble(lonField.getText()));
            user.setNotificationRadius(radiusSlider.getValue());
            userStore.update(user);
            new Alert(Alert.AlertType.INFORMATION, "Settings saved!").show();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid coordinates format.").show();
        }
    }

    public VBox getRoot() { return root; }
}