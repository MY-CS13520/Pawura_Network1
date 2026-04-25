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

    private double xOffset = 0;
    private double yOffset = 0;

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
        usernameField.setOnAction(e -> passwordField.requestFocus()); // Move focus to password field

        Button signUpButton = new Button("Don't have an account? Sign Up");
        signUpButton.getStyleClass().add("secondary-button");
        signUpButton.setMaxWidth(Double.MAX_VALUE);
        signUpButton.setOnAction(e -> showSignUpView());
        signUpButton.setPadding(new Insets(10, 0, 10, 0)); // Add some padding for better look

        VBox form = new VBox(8, userLbl, usernameField, passLbl, passwordField,
                             errorLabel, loginButton, signUpButton); // Add sign-up button
        form.setPadding(new Insets(0, 32, 0, 32));
        form.setAlignment(Pos.CENTER);
        VBox.setMargin(signUpButton, new Insets(10, 0, 0, 0)); // Margin above sign-up button

        Label hint = new Label(
            "Demo logins:  admin | ranger1 | viewer1 | demo    password: password123");
        hint.getStyleClass().add("hint-label");
        hint.setWrapText(true);
        hint.setTextAlignment(TextAlignment.CENTER);
        hint.setPadding(new Insets(18, 24, 4, 24));

        VBox card = new VBox(header, form, hint);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.setPadding(new Insets(44, 8, 32, 8));

        // Wrapper to center the card while the TitleBar stays at the top
        VBox cardContainer = new VBox(card);
        cardContainer.setAlignment(Pos.CENTER);
        VBox.setVgrow(cardContainer, Priority.ALWAYS);

        VBox outer = new VBox(createTitleBar(), cardContainer);
        outer.getStyleClass().add("login-bg");

        // Enable dragging of the entire window
        outer.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        outer.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        return outer;
    }

    private HBox createTitleBar() {
        Button minBtn = new Button("—");
        minBtn.getStyleClass().add("window-control-btn");
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().addAll("window-control-btn", "close-btn");
        closeBtn.setOnAction(e -> stage.close());

        HBox controls = new HBox(minBtn, closeBtn);
        controls.setAlignment(Pos.TOP_RIGHT);
        controls.getStyleClass().add("window-header");
        return controls;
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

    /**
     * Switches the view to the SignUpView.
     */
    private void showSignUpView() {
        SignUpView signUpView = new SignUpView(stage);
        Scene scene = new Scene(signUpView.getRoot(), PawuraApp.LOGIN_W, PawuraApp.LOGIN_H);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
    }
    public Parent getRoot() { return root; }
}
