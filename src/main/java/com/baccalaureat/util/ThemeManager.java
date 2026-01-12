package com.baccalaureat.util;

import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Utility class for applying themes to JavaFX scenes and stages.
 * Manages the gaming theme CSS for the application.
 */
public class ThemeManager {
    
    // Available themes
    public enum Theme {
        GAMING("game-theme.css"),
        LIGHT("styles.css"),
        DARK("theme-dark.css");
        
        private final String cssFile;
        
        Theme(String cssFile) {
            this.cssFile = cssFile;
        }
        
        public String getCssFile() {
            return cssFile;
        }
    }
    
    private static Theme currentTheme = Theme.GAMING;
    
    /**
     * Gets the theme from saved preferences
     * 
     * @return The theme from settings
     */
    public static Theme getSavedTheme() {
        try {
            // Import SettingsController to get saved theme
            String savedTheme = com.baccalaureat.controller.SettingsController.getTheme();
            return switch (savedTheme) {
                case "Dark Theme" -> Theme.DARK;
                case "Light Theme" -> Theme.LIGHT;
                default -> Theme.GAMING; // Clean (Mint Green)
            };
        } catch (Exception e) {
            System.err.println("Could not load saved theme: " + e.getMessage());
            return Theme.GAMING;
        }
    }
    
    /**
     * Applies the gaming theme to a scene
     * 
     * @param scene The scene to apply the theme to
     */
    public static void applyGamingTheme(Scene scene) {
        applyTheme(scene, Theme.GAMING);
    }
    
    /**
     * Applies the saved theme from preferences to a scene
     * 
     * @param scene The scene to apply the theme to
     */
    public static void applySavedTheme(Scene scene) {
        Theme theme = getSavedTheme();
        applyTheme(scene, theme);
    }
    
    /**
     * Applies a specific theme to a scene
     * 
     * @param scene The scene to apply the theme to
     * @param theme The theme to apply
     */
    public static void applyTheme(Scene scene, Theme theme) {
        if (scene == null) {
            System.err.println("Warning: Cannot apply theme to null scene");
            return;
        }
        
        // Clear existing stylesheets
        scene.getStylesheets().clear();
        
        // Apply the selected theme
        try {
            String css = ThemeManager.class.getResource(
                "/com/baccalaureat/" + theme.getCssFile()
            ).toExternalForm();
            scene.getStylesheets().add(css);
            currentTheme = theme;
            System.out.println("Applied theme: " + theme.name());
        } catch (Exception e) {
            System.err.println("Warning: Could not load theme " + theme.name() + ": " + e.getMessage());
            // Fallback to default if available
            if (theme != Theme.LIGHT) {
                applyTheme(scene, Theme.LIGHT);
            }
        }
    }
    
    /**
     * Applies the current theme to a stage's scene
     * 
     * @param stage The stage to apply the theme to
     */
    public static void applyCurrentTheme(Stage stage) {
        if (stage != null && stage.getScene() != null) {
            applyTheme(stage.getScene(), currentTheme);
        }
    }
    
    /**
     * Gets the current active theme
     * 
     * @return The current theme
     */
    public static Theme getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Sets up a stage with the gaming theme and dark window decorations
     * 
     * @param stage The stage to set up
     */
    public static void setupGamingStage(Stage stage) {
        if (stage == null) return;
        
        // Apply gaming theme to scene if available
        if (stage.getScene() != null) {
            applyGamingTheme(stage.getScene());
        }
        
        // Make window resizable but set preferred size
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
    }
    
    /**
     * Switches to the next theme (for theme toggle buttons)
     * 
     * @param scene The scene to apply the new theme to
     */
    public static void toggleTheme(Scene scene) {
        Theme newTheme = switch (currentTheme) {
            case GAMING -> Theme.LIGHT;
            case LIGHT -> Theme.DARK;
            case DARK -> Theme.GAMING;
        };
        applyTheme(scene, newTheme);
    }
    
    /**
     * Gets the CSS file path for the current theme
     * 
     * @return The CSS file path
     */
    public static String getCurrentThemeCSS() {
        try {
            return ThemeManager.class.getResource(
                "/com/baccalaureat/" + currentTheme.getCssFile()
            ).toExternalForm();
        } catch (Exception e) {
            System.err.println("Warning: Could not get theme CSS: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates a full-screen scene with the current theme applied
     * 
     * @param root The root node for the scene
     * @return A scene sized for full screen with theme applied
     */
    public static Scene createFullScreenScene(Parent root) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
        applySavedTheme(scene);
        
        // Also apply saved font size
        try {
            String fontSize = com.baccalaureat.controller.SettingsController.getFontSize();
            String fontSizeClass = switch (fontSize) {
                case "Small" -> "font-size-small";
                case "Large" -> "font-size-large";
                default -> "font-size-medium";
            };
            scene.getRoot().getStyleClass().add(fontSizeClass);
        } catch (Exception e) {
            System.err.println("Could not load saved font size: " + e.getMessage());
        }
        
        return scene;
    }
    
    /**
     * Switches a stage to a new scene in full screen mode with theme applied
     * 
     * @param stage The stage to switch
     * @param root The root node for the new scene
     */
    public static void switchToFullScreenScene(Stage stage, Parent root) {
        if (stage == null || root == null) return;
        
        Scene scene = createFullScreenScene(root);
        stage.setScene(scene);
        stage.setMaximized(true);
    }
}
