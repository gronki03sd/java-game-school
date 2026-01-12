package com.baccalaureat.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Utility class for creating and showing styled JavaFX dialogs.
 * This ensures consistent styling across all alerts and dialogs in the application.
 */
public class DialogHelper {

    // Path to the main CSS file (will use current theme)
    private static final String MAIN_CSS = "/com/baccalaureat/styles.css";
    
    /**
     * Shows an information alert with custom styling
     * 
     * @param title The title of the alert
     * @param header The header text
     * @param content The content text
     */
    public static void showInformation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleDialog(alert);
        alert.showAndWait();
    }
    
    /**
     * Shows an error alert with custom styling
     * 
     * @param title The title of the alert
     * @param header The header text
     * @param content The content text
     */
    public static void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleDialog(alert);
        alert.showAndWait();
    }
    
    /**
     * Shows a warning alert with custom styling
     * 
     * @param title The title of the alert
     * @param header The header text
     * @param content The content text
     */
    public static void showWarning(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleDialog(alert);
        alert.showAndWait();
    }
    
    /**
     * Shows a confirmation dialog with custom styling
     * 
     * @param title The title of the confirmation
     * @param header The header text
     * @param content The content text
     * @return true if the user clicked OK, false otherwise
     */
    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleDialog(alert);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Shows a confirmation dialog with custom button text
     * 
     * @param title The title of the confirmation
     * @param header The header text
     * @param content The content text
     * @param buttonType The custom button type
     * @return the selected button type
     */
    public static Optional<ButtonType> showConfirmation(String title, String header, String content, ButtonType... buttonTypes) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(buttonTypes);
        styleDialog(alert);
        
        return alert.showAndWait();
    }
    
    /**
     * Creates and returns a styled alert without showing it
     * Useful when you need to customize the alert further before showing
     * 
     * @param alertType The type of alert
     * @param title The title
     * @param header The header text
     * @param content The content text
     * @return The styled alert
     */
    public static Alert createStyledAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        styleDialog(alert);
        return alert;
    }
    
    /**
     * Applies custom styling to an alert dialog
     * - Loads the main CSS file
     * - Applies a custom style class to the DialogPane
     * - Sets the icon for the stage (if needed)
     * 
     * @param alert The alert to style
     */
    private static void styleDialog(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        
        // Load the current theme CSS file
        try {
            String css = ThemeManager.getCurrentThemeCSS();
            if (css != null) {
                dialogPane.getStylesheets().add(css);
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load CSS for dialog: " + e.getMessage());
        }
        
        // Add custom style class for additional styling
        dialogPane.getStyleClass().add("custom-dialog");
        
        // Style the dialog based on alert type
        switch (alert.getAlertType()) {
            case ERROR:
                dialogPane.getStyleClass().add("error-dialog");
                break;
            case WARNING:
                dialogPane.getStyleClass().add("warning-dialog");
                break;
            case INFORMATION:
                dialogPane.getStyleClass().add("info-dialog");
                break;
            case CONFIRMATION:
                dialogPane.getStyleClass().add("confirmation-dialog");
                break;
            default:
                break;
        }
        
        // Set minimum width for better appearance
        dialogPane.setMinWidth(400);
        
        // Apply styling to stage if available
        if (alert.getDialogPane().getScene() != null && 
            alert.getDialogPane().getScene().getWindow() != null) {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            // You can set window icon here if needed
            // stage.getIcons().add(new Image(DialogHelper.class.getResourceAsStream("/icon.png")));
        }
    }
}
