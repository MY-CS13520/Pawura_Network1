package com.pawura.app;

import com.pawura.database.DatabaseManager;
import com.pawura.ui.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * PawuraApp – the JavaFX Application subclass.
 * Bootstraps the database, then shows the Login screen.
 */
public class PawuraApp extends Application {

    public static final String APP_TITLE   = "Pawura Network 🐘";
    public static final double WINDOW_W    = 1100;
    public static final double WINDOW_H    = 720;
    public static final double LOGIN_W     = 480;
    public static final double LOGIN_H     = 580;

    @Override
    public void start(Stage primaryStage) {
        // Initialise the database (creates tables if they don't exist)
        DatabaseManager.getInstance().initialise();

        // Build Login screen
        LoginView loginView = new LoginView(primaryStage);
        Scene scene = new Scene(loginView.getRoot(), LOGIN_W, LOGIN_H);
        scene.getStylesheets().add(
            getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    @Override
    public void stop() {
        DatabaseManager.getInstance().close();
    }
}
