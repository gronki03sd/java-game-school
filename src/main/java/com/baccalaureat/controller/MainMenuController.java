package com.baccalaureat.controller;

import java.io.IOException;

import com.baccalaureat.model.GameConfig;
import com.baccalaureat.model.GameSession;
import com.baccalaureat.service.CategoryService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainMenuController {
    @FXML private Button startSoloButton;
    @FXML private Button startMultiplayerButton;
    @FXML private Button howToPlayButton;
    @FXML private Button themeToggleButton;
    @FXML private Button categoryConfigButton;
    @FXML private Button settingsButton;
    @FXML private Label highScoreLabel;
    @FXML private Label gamesPlayedLabel;
    @FXML private Label categoriesCountLabel;

    private boolean darkMode = false;
    private final CategoryService categoryService = new CategoryService();

    @FXML
    private void initialize() {
        updateStatistics();
        updateThemeButton();
    }

    private void updateStatistics() {
        // Update stats
        highScoreLabel.setText(String.valueOf(GameSession.getHighScore()));
        gamesPlayedLabel.setText(String.valueOf(GameSession.getGamesPlayed()));
        
        // Load categories count from database
        updateCategoriesCount();
    }
    
    private void updateCategoriesCount() {
        int enabledCount = categoryService.getEnabledCategories().size();
        categoriesCountLabel.setText(String.valueOf(enabledCount));
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        updateThemeButton();
    }

    private void updateThemeButton() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(darkMode ? "☀️ Mode Clair" : "🌙 Mode Sombre");
        }
    }

    @FXML
    private void handleStartSolo(ActionEvent event) throws IOException {
        navigateToGameConfiguration(GameConfig.GameMode.SOLO);
    }

    @FXML
    private void handleStartMultiplayer(ActionEvent event) throws IOException {
        navigateToGameConfiguration(GameConfig.GameMode.LOCAL);
    }

    @FXML
    private void handleHowToPlay(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Comment Jouer");
        alert.setHeaderText("🎯 Règles du Baccalauréat+");
        alert.setContentText("""
            📝 RÈGLES DU JEU:
            
            1. Une lettre aléatoire est tirée au début de chaque manche
            
            2. Trouvez un mot commençant par cette lettre pour chaque catégorie
               Ex: Si la lettre est "A" → Animal: "Abeille", Pays: "Allemagne"
            
            3. Les mots sont validés automatiquement par notre système intelligent
            
            4. Gagnez des points selon la difficulté et la rareté de vos réponses
            
            💡 ASTUCES:
            • Utilisez des mots moins courants pour plus de points
            • Évitez les répétitions dans la même partie
            • Le système vérifie que vos mots correspondent bien à la catégorie
            
            🏆 MODES DE JEU:
            • Solo: Jouez seul contre le chrono
            • Multijoueur: Jusqu'à 6 joueurs en local
            
            Bonne chance! 🍀
            """);
        alert.showAndWait();
    }

    @FXML
    private void handleThemeToggle(ActionEvent event) {
        darkMode = !darkMode;
        updateThemeButton();
        
        // Apply theme to current scene
        Scene scene = themeToggleButton.getScene();
        if (scene != null) {
            scene.getStylesheets().removeIf(s -> s.contains("theme-light.css") || s.contains("theme-dark.css"));
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
        }
    }

    @FXML
    private void handleCategoryConfig(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/CategoryConfig.fxml"));
            Parent root = loader.load();

            Stage categoryStage = new Stage();
            categoryStage.setTitle("Gestionnaire de Catégories");
            categoryStage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 700, 500);
            
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
            
            categoryStage.setScene(scene);
            categoryStage.showAndWait();
            
            // Refresh categories count after closing dialog
            updateCategoriesCount();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleSettings(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/Settings.fxml"));
            Parent root = loader.load();

            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.setScene(new Scene(root, 400, 300));
            settingsStage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void navigateToGameConfiguration(GameConfig.GameMode mode) {
        try {
            Stage stage = (Stage) startSoloButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/GameConfigurationView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 750);
            
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
            
            // Configure the GameConfigurationController
            Object controller = loader.getController();
            if (controller instanceof GameConfigurationController gcc) {
                gcc.setDarkMode(darkMode);
                gcc.setGameMode(mode);
            }
            
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
