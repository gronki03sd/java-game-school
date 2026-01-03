package com.baccalaureat.controller;

import java.io.IOException;

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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class MainMenuController {
    @FXML private Button startSoloButton;
    @FXML private Button startMultiplayerButton;
    @FXML private Button howToPlayButton;
    @FXML private Button themeToggleButton;
    @FXML private Button categoryConfigButton;
    @FXML private Label highScoreLabel;
    @FXML private Label gamesPlayedLabel;
    @FXML private Label categoriesCountLabel;
    @FXML private ToggleButton easyBtn;
    @FXML private ToggleButton mediumBtn;
    @FXML private ToggleButton hardBtn;

    private ToggleGroup difficultyGroup;
    private boolean darkMode = false;
    private final CategoryService categoryService = new CategoryService();

    @FXML
    private void initialize() {
        // Setup toggle group for difficulty
        difficultyGroup = new ToggleGroup();
        easyBtn.setToggleGroup(difficultyGroup);
        mediumBtn.setToggleGroup(difficultyGroup);
        hardBtn.setToggleGroup(difficultyGroup);

        // Set default selection based on current difficulty
        switch (GameSession.getSelectedDifficulty()) {
            case EASY -> easyBtn.setSelected(true);
            case MEDIUM -> mediumBtn.setSelected(true);
            case HARD -> hardBtn.setSelected(true);
        }
        updateSelectedStyle();

        // Update stats
        highScoreLabel.setText(String.valueOf(GameSession.getHighScore()));
        gamesPlayedLabel.setText(String.valueOf(GameSession.getGamesPlayed()));
        
        // Load categories count from database
        updateCategoriesCount();
        
        // Set initial theme
        applyTheme(false);
    }
    
    private void updateCategoriesCount() {
        int enabledCount = categoryService.getEnabledCategories().size();
        categoriesCountLabel.setText(String.valueOf(enabledCount));
    }
    @FXML
    private void handleThemeToggle(ActionEvent event) {
        darkMode = !darkMode;
        applyTheme(darkMode);
        themeToggleButton.setText(darkMode ? "☀️ Mode Clair" : "🌙 Mode Sombre");
    }

    private void applyTheme(boolean dark) {
        Scene scene = startSoloButton.getScene();
        if (scene == null) return;
        scene.getStylesheets().removeIf(s -> s.contains("theme-light.css") || s.contains("theme-dark.css"));
        if (dark) {
            scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
        }
    }

    @FXML
    private void handleDifficultyChange(ActionEvent event) {
        ToggleButton source = (ToggleButton) event.getSource();
        if (source.isSelected()) {
            String difficulty = (String) source.getUserData();
            GameSession.setDifficulty(GameSession.Difficulty.valueOf(difficulty));
            updateSelectedStyle();
        } else {
            // Prevent deselection - reselect the button
            source.setSelected(true);
        }
    }

    private void updateSelectedStyle() {
        easyBtn.getStyleClass().remove("difficulty-button-selected");
        mediumBtn.getStyleClass().remove("difficulty-button-selected");
        hardBtn.getStyleClass().remove("difficulty-button-selected");

        if (easyBtn.isSelected()) easyBtn.getStyleClass().add("difficulty-button-selected");
        if (mediumBtn.isSelected()) mediumBtn.getStyleClass().add("difficulty-button-selected");
        if (hardBtn.isSelected()) hardBtn.getStyleClass().add("difficulty-button-selected");
    }

    @FXML
    private void handleStartSolo(ActionEvent event) throws IOException {
        Stage stage = (Stage) startSoloButton.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/GameView.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 750);
        // Pass theme
        if (darkMode) {
            scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
        }
        // Set darkMode in GameController
        Object controller = loader.getController();
        if (controller instanceof com.baccalaureat.controller.GameController gc) {
            gc.setDarkMode(darkMode);
        }
        stage.setScene(scene);
        stage.show();
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
            
            3. Validez vos réponses avant la fin du temps!
            
            📊 POINTS:
            • Réponse valide: +2 points
            • Réponse unique: +1 point bonus
            
            🎮 DIFFICULTÉS:
            • Facile: 90s, 4 catégories, 3 manches
            • Normal: 60s, 6 catégories, 5 manches
            • Difficile: 45s, 8 catégories, 7 manches
            
            Bonne chance! 🍀
            """);
        alert.showAndWait();
    }

    @FXML
    private void handleStartMultiplayer(ActionEvent event) throws IOException {
        Stage stage = (Stage) startMultiplayerButton.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MultiplayerLobby.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 750);
        // Pass theme
        if (darkMode) {
            scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
        }
        // Set darkMode in MultiplayerLobbyController
        Object controller = loader.getController();
        if (controller instanceof com.baccalaureat.controller.MultiplayerLobbyController mlc) {
            mlc.setDarkMode(darkMode);
        }
        stage.setScene(scene);
        stage.show();
    }
    
    @FXML
    private void handleCategoryConfig() {
        System.out.println("Category Config button clicked!"); // Debug log
        try {
            // Open category management window
            Stage categoryStage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/CategoryConfig.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 800, 600);
            
            // Apply current theme
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
            
            categoryStage.setTitle("Gestion des Catégories");
            categoryStage.setScene(scene);
            categoryStage.initOwner(startSoloButton.getScene().getWindow());
            categoryStage.showAndWait();
            
            // Refresh categories count after configuration changes
            updateCategoriesCount();
        } catch (Exception e) {
            System.err.println("Error opening category configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
