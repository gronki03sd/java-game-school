package com.baccalaureat.controller;

import java.util.prefs.Preferences;

import com.baccalaureat.util.ThemeManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

/**
 * Settings controller for managing user preferences including:
 * - Theme selection
 * - Display settings (font size)
 * - Audio settings (sound effects, music)
 * - Language selection
 */
public class SettingsController {
    
    @FXML private ComboBox<String> themeComboBox;
    @FXML private ComboBox<String> fontSizeComboBox;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private CheckBox soundEffectsCheckBox;
    @FXML private CheckBox backgroundMusicCheckBox;
    @FXML private Button saveButton;
    @FXML private Button closeButton;
    
    // Preferences for persistent storage
    private static final Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);
    
    // Preference keys
    private static final String THEME_KEY = "theme";
    private static final String FONT_SIZE_KEY = "fontSize";
    private static final String LANGUAGE_KEY = "language";
    private static final String SOUND_EFFECTS_KEY = "soundEffects";
    private static final String BACKGROUND_MUSIC_KEY = "backgroundMusic";
    
    // Default values
    private static final String DEFAULT_THEME = "Clean (Mint Green)";
    private static final String DEFAULT_FONT_SIZE = "Medium";
    private static final String DEFAULT_LANGUAGE = "English";
    private static final boolean DEFAULT_SOUND_EFFECTS = true;
    private static final boolean DEFAULT_BACKGROUND_MUSIC = false;
    
    @FXML
    public void initialize() {
        // Populate theme options
        themeComboBox.getItems().addAll(
            "Clean (Mint Green)",
            "Dark Theme"
        );
        
        // Populate font size options
        fontSizeComboBox.getItems().addAll(
            "Small",
            "Medium",
            "Large"
        );
        
        // Populate language options
        languageComboBox.getItems().addAll(
            "English",
            "Français"
        );
        
        // Load saved preferences
        loadPreferences();
    }
    
    private void loadPreferences() {
        // Load theme
        String savedTheme = prefs.get(THEME_KEY, DEFAULT_THEME);
        themeComboBox.setValue(savedTheme);
        
        // Load font size
        String savedFontSize = prefs.get(FONT_SIZE_KEY, DEFAULT_FONT_SIZE);
        fontSizeComboBox.setValue(savedFontSize);
        
        // Load language
        String savedLanguage = prefs.get(LANGUAGE_KEY, DEFAULT_LANGUAGE);
        languageComboBox.setValue(savedLanguage);
        
        // Load audio settings
        boolean soundEffects = prefs.getBoolean(SOUND_EFFECTS_KEY, DEFAULT_SOUND_EFFECTS);
        soundEffectsCheckBox.setSelected(soundEffects);
        
        boolean backgroundMusic = prefs.getBoolean(BACKGROUND_MUSIC_KEY, DEFAULT_BACKGROUND_MUSIC);
        backgroundMusicCheckBox.setSelected(backgroundMusic);
    }
    
    @FXML
    private void handleSave() {
        // Save theme
        String selectedTheme = themeComboBox.getValue();
        if (selectedTheme != null) {
            prefs.put(THEME_KEY, selectedTheme);
            applyThemeToAllWindows(selectedTheme);
        }
        
        // Save font size
        String selectedFontSize = fontSizeComboBox.getValue();
        if (selectedFontSize != null) {
            prefs.put(FONT_SIZE_KEY, selectedFontSize);
            applyFontSizeToAllWindows(selectedFontSize);
        }
        
        // Save language
        String selectedLanguage = languageComboBox.getValue();
        if (selectedLanguage != null) {
            prefs.put(LANGUAGE_KEY, selectedLanguage);
            System.out.println("[Settings] Language set to: " + selectedLanguage);
        }
        
        // Save audio settings
        boolean soundEnabled = soundEffectsCheckBox.isSelected();
        boolean musicEnabled = backgroundMusicCheckBox.isSelected();
        prefs.putBoolean(SOUND_EFFECTS_KEY, soundEnabled);
        prefs.putBoolean(BACKGROUND_MUSIC_KEY, musicEnabled);
        
        System.out.println("[Settings] Preferences saved successfully");
        System.out.println("  Theme: " + selectedTheme);
        System.out.println("  Font Size: " + selectedFontSize);
        System.out.println("  Language: " + selectedLanguage);
        System.out.println("  Sound Effects: " + soundEnabled);
        System.out.println("  Background Music: " + musicEnabled);
        
        // Show confirmation
        System.out.println("[Settings] Settings applied successfully!");
        
        // Close the settings window
        handleClose();
    }
    
    private void applyThemeToAllWindows(String themeName) {
        ThemeManager.Theme theme = switch (themeName) {
            case "Dark Theme" -> ThemeManager.Theme.DARK;
            case "Light Theme" -> ThemeManager.Theme.LIGHT;
            default -> ThemeManager.Theme.GAMING; // Clean (Mint Green)
        };
        
        // Apply to current settings window
        Stage settingsStage = (Stage) saveButton.getScene().getWindow();
        if (settingsStage != null && settingsStage.getScene() != null) {
            ThemeManager.applyTheme(settingsStage.getScene(), theme);
        }
        
        // Apply to main window
        Stage mainStage = (Stage) Stage.getWindows().stream()
            .filter(window -> window instanceof Stage && !window.equals(settingsStage))
            .findFirst()
            .orElse(null);
        
        if (mainStage != null && mainStage.getScene() != null) {
            ThemeManager.applyTheme(mainStage.getScene(), theme);
            // Also update font size on main window
            applyFontSizeToScene(mainStage.getScene(), getFontSize());
        }
    }
    
    private void applyFontSizeToAllWindows(String fontSize) {
        // Apply to all open windows
        Stage.getWindows().stream()
            .filter(window -> window instanceof Stage)
            .map(window -> (Stage) window)
            .filter(stage -> stage.getScene() != null)
            .forEach(stage -> applyFontSizeToScene(stage.getScene(), fontSize));
    }
    
    private void applyFontSizeToScene(javafx.scene.Scene scene, String fontSize) {
        // Remove existing font size classes
        scene.getRoot().getStyleClass().removeAll("font-size-small", "font-size-medium", "font-size-large");
        
        // Add new font size class
        String fontSizeClass = switch (fontSize) {
            case "Small" -> "font-size-small";
            case "Large" -> "font-size-large";
            default -> "font-size-medium";
        };
        scene.getRoot().getStyleClass().add(fontSizeClass);
    }
    
    private void applyTheme(String themeName) {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        if (stage != null && stage.getScene() != null) {
            ThemeManager.Theme theme = switch (themeName) {
                case "Dark Theme" -> ThemeManager.Theme.DARK;
                case "Light Theme" -> ThemeManager.Theme.LIGHT;
                default -> ThemeManager.Theme.GAMING; // Clean (Mint Green)
            };
            ThemeManager.applyTheme(stage.getScene(), theme);
        }
    }
    
    private void applyFontSize(String fontSize) {
        // TODO: Implement font size scaling
        // This would involve adding CSS classes for different font sizes
        System.out.println("[Settings] Font size changed to: " + fontSize);
    }
    
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    
    // Static getters for accessing settings from other controllers
    public static String getTheme() {
        return prefs.get(THEME_KEY, DEFAULT_THEME);
    }
    
    public static String getFontSize() {
        return prefs.get(FONT_SIZE_KEY, DEFAULT_FONT_SIZE);
    }
    
    public static String getLanguage() {
        return prefs.get(LANGUAGE_KEY, DEFAULT_LANGUAGE);
    }
    
    public static boolean isSoundEffectsEnabled() {
        return prefs.getBoolean(SOUND_EFFECTS_KEY, DEFAULT_SOUND_EFFECTS);
    }
    
    public static boolean isBackgroundMusicEnabled() {
        return prefs.getBoolean(BACKGROUND_MUSIC_KEY, DEFAULT_BACKGROUND_MUSIC);
    }
}