package com.baccalaureat.controller;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.GameConfig;
import com.baccalaureat.model.GameSession;
import com.baccalaureat.service.CategoryService;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Game Configuration Screen.
 * Replaces the old difficulty selection system with customizable game settings.
 */
public class GameConfigurationController {
    
    @FXML private Label modeLabel;
    @FXML private Button manageCategoriesButton;
    @FXML private Label categoryCountLabel;
    @FXML private FlowPane categoryChipsContainer;
    
    @FXML private Slider roundsSlider;
    @FXML private Label roundsValueLabel;
    @FXML private ComboBox<String> durationComboBox;
    
    @FXML private VBox playerConfigSection;
    @FXML private Label playerSectionTitle;
    @FXML private VBox soloPlayerConfig;
    @FXML private TextField soloNicknameField;
    @FXML private VBox localPlayerConfig;
    @FXML private Slider playersCountSlider;
    @FXML private Label playersCountLabel;
    @FXML private VBox nicknamesContainer;
    @FXML private VBox distantPlayerConfig;
    
    @FXML private Button backButton;
    @FXML private Label validationMessageLabel;
    @FXML private Button startGameButton;
    
    private final CategoryService categoryService = new CategoryService();
    private GameConfig gameConfig = new GameConfig();
    private boolean darkMode = false;
    private final Map<String, TextField> nicknameFields = new HashMap<>();
    
    @FXML
    private void initialize() {
        setupRoundsSlider();
        setupDurationComboBox();
        setupPlayersCountSlider();
        setupValidation();
        updateCategoryDisplay();
        updatePlayerConfiguration();
    }
    
    public void setGameMode(GameConfig.GameMode mode) {
        gameConfig.setMode(mode);
        modeLabel.setText(mode.getDisplayName());
        updatePlayerConfiguration();
    }
    
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }
    
    private void setupRoundsSlider() {
        roundsSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int rounds = newVal.intValue();
            gameConfig.setNumberOfRounds(rounds);
            roundsValueLabel.setText(String.valueOf(rounds));
            validateConfiguration();
        });
    }
    
    private void setupDurationComboBox() {
        durationComboBox.setValue("2 minutes (120s)");
        durationComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Extract the number from strings like "1 minute (60s)" or "1m 30s (90s)" etc
                String valueStr = newVal;
                try {
                    // Extract seconds from parentheses like "(120s)"
                    int startIdx = valueStr.lastIndexOf('(');
                    int endIdx = valueStr.lastIndexOf('s');
                    if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
                        String secondsStr = valueStr.substring(startIdx + 1, endIdx);
                        int seconds = Integer.parseInt(secondsStr);
                        gameConfig.setRoundDurationSeconds(seconds);
                        validateConfiguration();
                    } else {
                        // Fallback to 120 seconds if parsing fails
                        gameConfig.setRoundDurationSeconds(120);
                    }
                } catch (NumberFormatException e) {
                    // Fallback to 120 seconds if parsing fails
                    gameConfig.setRoundDurationSeconds(120);
                }
            }
        });
    }
    
    private void setupPlayersCountSlider() {
        playersCountSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int playerCount = newVal.intValue();
            playersCountLabel.setText(String.valueOf(playerCount));
            updateNicknameFields(playerCount);
            validateConfiguration();
        });
    }
    
    private void setupValidation() {
        // For Solo mode, auto-set default player name
        // For other modes, listen to nickname field changes
        soloNicknameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (gameConfig.getMode() != GameConfig.GameMode.SOLO) {
                List<String> nicknames = new ArrayList<>();
                if (newVal != null && !newVal.trim().isEmpty()) {
                    nicknames.add(newVal.trim());
                }
                gameConfig.setPlayerNicknames(nicknames);
            }
            validateConfiguration();
        });
    }
    
    private void updatePlayerConfiguration() {
        // Hide all player config sections
        soloPlayerConfig.setVisible(false);
        soloPlayerConfig.setManaged(false);
        localPlayerConfig.setVisible(false);
        localPlayerConfig.setManaged(false);
        distantPlayerConfig.setVisible(false);
        distantPlayerConfig.setManaged(false);
        
        switch (gameConfig.getMode()) {
            case SOLO -> {
                // Solo mode: use default player name, no UI needed
                List<String> defaultPlayer = List.of("Player");
                gameConfig.setPlayerNicknames(defaultPlayer);
                playerSectionTitle.setText("👤 Solo Player");
                // Keep solo config hidden for cleaner UI
            }
            case LOCAL -> {
                playerSectionTitle.setText("👥 Configuration Joueurs");
                localPlayerConfig.setVisible(true);
                localPlayerConfig.setManaged(true);
                updateNicknameFields((int) playersCountSlider.getValue());
            }
            case DISTANT -> {
                playerSectionTitle.setText("🌐 Mode Distant");
                distantPlayerConfig.setVisible(true);
                distantPlayerConfig.setManaged(true);
            }
        }
        
        validateConfiguration();
    }
    
    private void updateNicknameFields(int playerCount) {
        nicknamesContainer.getChildren().clear();
        nicknameFields.clear();
        
        for (int i = 1; i <= playerCount; i++) {
            Label label = new Label("Joueur " + i + ":");
            label.getStyleClass().add("config-label");
            
            TextField field = new TextField();
            field.setPromptText("Nom du joueur " + i);
            field.getStyleClass().add("config-input");
            field.setPrefWidth(200);
            
            String fieldKey = "player" + i;
            nicknameFields.put(fieldKey, field);
            
            // Listen to changes
            field.textProperty().addListener((obs, oldVal, newVal) -> {
                updatePlayerNicknames();
                validateConfiguration();
            });
            
            VBox fieldBox = new VBox(5, label, field);
            fieldBox.setPadding(new Insets(5));
            nicknamesContainer.getChildren().add(fieldBox);
        }
    }
    
    private void updatePlayerNicknames() {
        List<String> nicknames = new ArrayList<>();
        for (TextField field : nicknameFields.values()) {
            String text = field.getText();
            nicknames.add(text != null ? text.trim() : "");
        }
        gameConfig.setPlayerNicknames(nicknames);
    }
    
    private void updateCategoryDisplay() {
        List<Category> categories = categoryService.getEnabledCategories();
        gameConfig.setSelectedCategories(categories);
        
        categoryCountLabel.setText(categories.size() + " catégories sélectionnées");
        
        categoryChipsContainer.getChildren().clear();
        for (Category category : categories) {
            Label chip = new Label(category.getIcon() + " " + category.displayName());
            chip.getStyleClass().add("category-chip");
            categoryChipsContainer.getChildren().add(chip);
        }
        
        validateConfiguration();
    }
    
    private void validateConfiguration() {
        boolean isValid = gameConfig.isValid();
        startGameButton.setDisable(!isValid);
        
        if (!isValid) {
            String message = gameConfig.getValidationMessage();
            validationMessageLabel.setText(message);
            validationMessageLabel.setVisible(true);
        } else {
            validationMessageLabel.setVisible(false);
        }
    }
    
    @FXML
    private void handleManageCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/CategoryConfig.fxml"));
            Parent root = loader.load();
            
            Stage categoryStage = new Stage();
            categoryStage.setTitle("Gestionnaire de Catégories");
            categoryStage.initModality(Modality.APPLICATION_MODAL);
            categoryStage.setScene(new Scene(root, 700, 500));
            
            if (darkMode) {
                categoryStage.getScene().getStylesheets().add(
                    getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                categoryStage.getScene().getStylesheets().add(
                    getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
            
            categoryStage.showAndWait();
            
            // Refresh category display after closing dialog
            updateCategoryDisplay();
            
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le gestionnaire de catégories");
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MainMenu.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 700);
            
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
            
            // Set darkMode in MainMenuController
            Object controller = loader.getController();
            if (controller instanceof MainMenuController mmc) {
                mmc.setDarkMode(darkMode);
            }
            
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleStartGame() {
        if (!gameConfig.isValid()) {
            return;
        }
        
        try {
            Stage stage = (Stage) startGameButton.getScene().getWindow();
            
            switch (gameConfig.getMode()) {
                case SOLO -> startSoloGame(stage);
                case LOCAL -> startLocalGame(stage);
                case DISTANT -> showError("Non Implémenté", "Le mode distant n'est pas encore disponible");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de démarrer la partie");
        }
    }
    
    private void startSoloGame(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/GameView.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 750);
        
        if (darkMode) {
            scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
        }
        
        // Configure GameController with our settings
        Object controller = loader.getController();
        if (controller instanceof GameController gc) {
            gc.setDarkMode(darkMode);
            try {
                gc.configureGame(gameConfig);
            } catch (Exception e) {
                System.err.println("Error configuring GameController: " + e.getMessage());
                e.printStackTrace();
                throw new IOException("Failed to configure game controller", e);
            }
            
            // Show the scene first, then start the game
            stage.setScene(scene);
            stage.show();
            
            // Start the game after the scene is shown
            Platform.runLater(() -> {
                gc.startGameAfterSceneShown();
            });
        } else {
            stage.setScene(scene);
            stage.show();
        }
    }
    
    private void startLocalGame(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MultiplayerGame.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 750);
        
        if (darkMode) {
            scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
        } else {
            scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
        }
        
        // Configure MultiplayerGameController with our settings
        Object controller = loader.getController();
        if (controller instanceof MultiplayerGameController mgc) {
            try {
                mgc.configureGame(gameConfig);
            } catch (Exception e) {
                System.err.println("Error configuring MultiplayerGameController: " + e.getMessage());
                e.printStackTrace();
                throw new IOException("Failed to configure multiplayer game controller", e);
            }
            
            // Show the scene first, then start the game
            stage.setScene(scene);
            stage.show();
            
            // Start the game after the scene is shown
            Platform.runLater(() -> {
                mgc.startGameAfterSceneShown();
            });
        } else {
            stage.setScene(scene);
            stage.show();
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}