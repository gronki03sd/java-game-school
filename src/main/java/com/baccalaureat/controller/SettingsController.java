package com.baccalaureat.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

/**
 * Simple Settings controller for language selection.
 * Foundation for future internationalization features.
 */
public class SettingsController {
    
    @FXML private ComboBox<String> languageComboBox;
    @FXML private Button closeButton;
    
    // Static variable to store selected language (in-memory persistence)
    private static String selectedLanguage = "English";
    
    @FXML
    public void initialize() {
        languageComboBox.setValue(selectedLanguage);
        
        languageComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedLanguage = newVal;
                // TODO: Implement actual language switching logic
                System.out.println("[Settings] Language changed to: " + newVal);
            }
        });
    }
    
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    
    public static String getSelectedLanguage() {
        return selectedLanguage;
    }
}