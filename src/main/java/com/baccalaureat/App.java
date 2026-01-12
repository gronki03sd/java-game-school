package com.baccalaureat;

import com.baccalaureat.controller.SettingsController;
import com.baccalaureat.util.ThemeManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/baccalaureat/MainMenu.fxml"));
        
        // Get screen dimensions
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        
        // Create scene with screen size
        Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
        
        // Apply saved theme from settings
        applySavedTheme(scene);
        
        // Apply saved font size
        applySavedFontSize(scene);
        
        // Configure stage
        primaryStage.setTitle("Baccalauréat+ Game");
        primaryStage.setScene(scene);
        
        // Set minimum window size for when user restores from maximized
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        
        // Make window resizable (user can restore/resize if they want)
        primaryStage.setResizable(true);
        
        // Start maximized (full screen but with window controls)
        primaryStage.setMaximized(true);
        
        primaryStage.show();
    }
    
    private void applySavedTheme(Scene scene) {
        String savedTheme = SettingsController.getTheme();
        ThemeManager.Theme theme = switch (savedTheme) {
            case "Dark Theme" -> ThemeManager.Theme.DARK;
            case "Light Theme" -> ThemeManager.Theme.LIGHT;
            default -> ThemeManager.Theme.GAMING; // Clean (Mint Green)
        };
        ThemeManager.applyTheme(scene, theme);
    }
    
    private void applySavedFontSize(Scene scene) {
        String fontSize = SettingsController.getFontSize();
        String fontSizeClass = switch (fontSize) {
            case "Small" -> "font-size-small";
            case "Large" -> "font-size-large";
            default -> "font-size-medium";
        };
        scene.getRoot().getStyleClass().add(fontSizeClass);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
