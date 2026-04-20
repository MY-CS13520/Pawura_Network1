package com.pawura;

import com.pawura.app.PawuraApp;

/**
 * Entry point for the Pawura Network application.
 * Delegates to PawuraApp (JavaFX launcher) to avoid module-path issues
 * when packaging as a fat JAR.
 */
public class Main {
    public static void main(String[] args) {
        PawuraApp.launch(PawuraApp.class, args);
    }
}
