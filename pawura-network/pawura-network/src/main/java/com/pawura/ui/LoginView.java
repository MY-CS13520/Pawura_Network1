package com.pawura.ui;

import com.pawura.app.PawuraApp;
import com.pawura.model.User;
import com.pawura.service.AuthenticationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * LoginView – sign-in screen with username / password fields.
 * On successful authentication launches DashboardView.
 */
public class LoginView {

    private final Stage  stage;
    private final VBox   root;
    private final AuthenticationService authService = new AuthenticationService();

    private final TextField     usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button        loginButton   = new Button("Sign In");
    private final Label         errorLabel    = new Label();

    public LoginView(Stage stage) {
        this.stage = stage;
        root = buildUI();
    }

    private VBox buildUI() {
        Label emoji = new Label("🐘");
        emoji.getStyleClass().add("login-emoji");

        Label title = new Label("Pawura Network");
        title.getStyleClass().add("login-title");

        Label sub = new Label("Elephant Movement Tracking System — Sri Lanka");
        sub.getStyleClass().add("login-subtitle");
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setWrapText(true);

        VBox header = new VBox(4, emoji, title, sub);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 28, 0));

        Label userLbl = new Label("Username");
        userLbl.getStyleClass().add("field-label");
        usernameField.setPromptText("Enter your username");
        usernameField.getStyleClass().add("form-field");

        Label passLbl = new Label("Password");
        passLbl.getStyleClass().add("field-label");
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("form-field");

        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> passwordField.requestFocus());

        VBox form = new VBox(8, userLbl, usernameField, passLbl, passwordField,
                             errorLabel, loginButton);
        form.setPadding(new Insets(0, 32, 0, 32));

        Label hint = new Label(
            "Demo logins:  admin  |  ranger1  |  viewer1     password: password123");
        hint.getStyleClass().add("hint-label");
        hint.setWrapText(true);
        hint.setTextAlignment(TextAlignment.CENTER);
        hint.setPadding(new Insets(18, 24, 4, 24));

        VBox card = new VBox(header, form, hint);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.setPadding(new Insets(44, 8, 32, 8));

        VBox outer = new VBox(card);
        outer.setAlignment(Pos.CENTER);
        outer.getStyleClass().add("login-bg");
        VBox.setVgrow(outer, Priority.ALWAYS);
        return outer;
    }

    private void handleLogin() {
        errorLabel.setVisible(false);
        loginButton.setDisable(true);
        loginButton.setText("Signing in…");

        Optional<User> result = authService.login(
            usernameField.getText().trim(), passwordField.getText());

        loginButton.setDisable(false);
        loginButton.setText("Sign In");

        if (result.isPresent()) {
            DashboardView dash = new DashboardView(stage, result.get(), authService);
            Scene scene = new Scene(dash.getRoot(), PawuraApp.WINDOW_W, PawuraApp.WINDOW_H);
            scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();
        } else {
            errorLabel.setText("⚠  Invalid username or password. Please try again.");
            errorLabel.setVisible(true);
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    public Parent getRoot() { return root; }
}
