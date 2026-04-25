package com.pawura.ui;

import com.pawura.app.PawuraApp;
import com.pawura.model.User;
import com.pawura.service.AuthenticationService;
import com.pawura.util.ValidationUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/**
 * SignUpView – user registration screen with username, email, password, and OTP verification.
 */
public class SignUpView {

    private final Stage stage;
    private final VBox root;
    private final AuthenticationService authService = new AuthenticationService();

    private final TextField usernameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final TextField otpField = new TextField();

    private final Button sendOtpButton = new Button("Send OTP");
    private final Button verifyAndRegisterButton = new Button("Verify & Register");
    private final Label errorLabel = new Label();

    private VBox otpSection;

    private double xOffset = 0;
    private double yOffset = 0;

    public SignUpView(Stage stage) {
        this.stage = stage;
        root = buildUI();
    }

    private VBox buildUI() {
        Label emoji = new Label("🐘");
        emoji.getStyleClass().add("login-emoji");

        Label title = new Label("Pawura Network");
        title.getStyleClass().add("login-title");

        Label sub = new Label("Create Your Account");
        sub.getStyleClass().add("login-subtitle");
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setWrapText(true);

        VBox header = new VBox(2, emoji, title, sub);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 15, 0));

        // Username
        Label userLbl = new Label("Username");
        userLbl.getStyleClass().add("field-label");
        usernameField.setPromptText("Enter your username");
        usernameField.getStyleClass().add("form-field");

        // Email
        Label emailLbl = new Label("Email");
        emailLbl.getStyleClass().add("field-label");
        emailField.setPromptText("Enter your email address");
        emailField.getStyleClass().add("form-field");

        // Password
        Label passLbl = new Label("Password");
        passLbl.getStyleClass().add("field-label");
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("form-field");

        // Confirm Password
        Label confirmPassLbl = new Label("Confirm Password");
        confirmPassLbl.getStyleClass().add("field-label");
        confirmPasswordField.setPromptText("Confirm your password");
        confirmPasswordField.getStyleClass().add("form-field");

        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);

        sendOtpButton.getStyleClass().add("primary-button");
        sendOtpButton.setMaxWidth(Double.MAX_VALUE);
        sendOtpButton.setOnAction(e -> handleSendOtp());

        // OTP Section (initially hidden)
        Label otpLbl = new Label("OTP");
        otpLbl.getStyleClass().add("field-label");
        otpField.setPromptText("Enter 6-digit OTP");
        otpField.getStyleClass().add("form-field");

        verifyAndRegisterButton.getStyleClass().add("primary-button");
        verifyAndRegisterButton.setMaxWidth(Double.MAX_VALUE);
        verifyAndRegisterButton.setOnAction(e -> handleVerifyAndRegister());

        otpSection = new VBox(8, otpLbl, otpField, verifyAndRegisterButton);
        otpSection.setVisible(false);
        otpSection.setManaged(false); // Don't take up space when invisible
        otpSection.setAlignment(Pos.CENTER);

        Button backToLoginButton = new Button("Back to Login");
        backToLoginButton.getStyleClass().add("secondary-button");
        backToLoginButton.setMaxWidth(Double.MAX_VALUE);
        backToLoginButton.setOnAction(e -> showLoginView());
        VBox.setMargin(backToLoginButton, new Insets(10, 0, 0, 0));

        VBox form = new VBox(6, userLbl, usernameField, emailLbl, emailField,
                             passLbl, passwordField, confirmPassLbl, confirmPasswordField,
                             errorLabel, sendOtpButton, otpSection, backToLoginButton);
        form.setPadding(new Insets(5, 32, 5, 32));
        form.setAlignment(Pos.CENTER);

        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        VBox card = new VBox(header, scrollPane);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420);
        card.setPadding(new Insets(25, 8, 20, 8));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox cardContainer = new VBox(card);
        cardContainer.setAlignment(Pos.CENTER);
        VBox.setVgrow(cardContainer, Priority.ALWAYS);

        VBox outer = new VBox(createTitleBar(), cardContainer);
        outer.getStyleClass().add("login-bg");

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

    private void handleSendOtp() {
        errorLabel.setVisible(false);
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("All fields are required.");
            return;
        }
        if (!ValidationUtils.isValidUsername(username)) {
            showError("Username must be 3-30 alphanumeric characters.");
            return;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            showError("Please enter a valid email address.");
            return;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        sendOtpButton.setDisable(true);
        sendOtpButton.setText("Sending OTP...");

        String otp = authService.generateRandomOtp();
        if (authService.sendOtpEmail(email, otp)) {
            showInfo("OTP sent to " + email + ". Please check your inbox.");
            otpSection.setVisible(true);
            otpSection.setManaged(true);
            sendOtpButton.setVisible(false); // Hide send OTP button
            sendOtpButton.setManaged(false);
            otpField.requestFocus();
        } else {
            showError("Failed to send OTP. Please check your email and try again.");
            sendOtpButton.setDisable(false);
            sendOtpButton.setText("Send OTP");
        }
    }

    private void handleVerifyAndRegister() {
        errorLabel.setVisible(false);
        String email = emailField.getText().trim();
        String otp = otpField.getText().trim();

        if (otp.isEmpty()) {
            showError("Please enter the OTP.");
            return;
        }

        verifyAndRegisterButton.setDisable(true);
        verifyAndRegisterButton.setText("Verifying...");

        if (authService.verifyOtp(email, otp)) {
            // OTP verified, now register the user
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String fullName = ""; // You might want to add a full name field later
            User.Role role = User.Role.VIEWER; // Default role for new registrations

            if (authService.register(username, password, email, fullName, role)) {
                showInfo("Registration successful! You can now log in.");
                showLoginView();
            } else {
                showError("Registration failed. Username or email might already be in use.");
                verifyAndRegisterButton.setDisable(false);
                verifyAndRegisterButton.setText("Verify & Register");
            }
        } else {
            showError("Invalid OTP. Please try again.");
            verifyAndRegisterButton.setDisable(false);
            verifyAndRegisterButton.setText("Verify & Register");
        }
    }

    private void showLoginView() {
        LoginView login = new LoginView(stage);
        Scene scene = new Scene(login.getRoot(), PawuraApp.LOGIN_W, PawuraApp.LOGIN_H);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
    }

    private void showInfo(String msg) {
        errorLabel.setText(msg); // Using errorLabel for info too, could be a separate infoLabel
        errorLabel.getStyleClass().remove("error-label");
        errorLabel.getStyleClass().add("info-label"); // Assuming you have an info-label style
        errorLabel.setVisible(true);
    }

    public Parent getRoot() { return root; }
}