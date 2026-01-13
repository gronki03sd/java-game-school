package com.baccalaureat.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baccalaureat.controller.GameController;
import com.baccalaureat.controller.MultiplayerGameController;
import com.baccalaureat.model.Category;
import com.baccalaureat.model.GameConfig;
import com.baccalaureat.multiplayer.MultiplayerEventListener;
import com.baccalaureat.service.CategoryService;
import com.baccalaureat.util.DialogHelper;
import com.baccalaureat.util.ThemeManager;
import com.fasterxml.jackson.databind.JsonNode;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller for the Game Configuration Screen.
 * Replaces the old difficulty selection system with customizable game settings.
 */
public class GameConfigurationController implements MultiplayerEventListener {
    
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
    
    // Multiplayer-specific fields
    private String multiplayerSessionId;
    private String multiplayerPlayerName;
    private MultiplayerLobbyController lobbyController;
    
    // Timeout management
    private Thread timeoutThread = null;
    
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
    
    /**
     * Configure controller for multiplayer mode.
     */
    public void setMultiplayerMode(String sessionId, String playerName, MultiplayerLobbyController lobbyController) {
        this.multiplayerSessionId = sessionId;
        this.multiplayerPlayerName = playerName;
        this.lobbyController = lobbyController;
        
        // Set DISTANT mode and hide player configuration section for multiplayer
        gameConfig.setMode(GameConfig.GameMode.DISTANT);
        modeLabel.setText("Mode Multijoueur - Session: " + sessionId);
        playerConfigSection.setVisible(false);
        playerConfigSection.setManaged(false);
        
        // Set default player name for host - remove local multiplayer restrictions
        List<String> hostPlayer = List.of(playerName);
        gameConfig.setPlayerNicknames(hostPlayer);
        
        // Update start button text for multiplayer
        startGameButton.setText("Confirmer et Démarrer");
        
        // Register as WebSocket event listener for GAME_STARTED
        if (lobbyController != null && lobbyController.getMultiplayerService() != null) {
            lobbyController.getMultiplayerService().addEventListener(this);
            System.out.println("[CONFIG] Registered as WebSocket event listener");
        }
        
        // Force update category display to ensure categories are loaded
        System.out.println("[CONFIG] Multiplayer mode set - forcing category update");
        updateCategoryDisplay();
        
        System.out.println("[CONFIG] Multiplayer mode set - Session: " + sessionId + ", Player: " + playerName);
    }
    
    /**
     * Called when GAME_STARTED event is received to close the configuration window
     */
    public void closeConfigurationWindow() {
        Platform.runLater(() -> {
            // Cleanup: Remove event listener when closing
            if (lobbyController != null && lobbyController.getMultiplayerService() != null) {
                lobbyController.getMultiplayerService().removeEventListener(this);
                System.out.println("[CONFIG] Removed WebSocket event listener during cleanup");
            }
            
            // Cancel timeout thread if still running
            if (timeoutThread != null && timeoutThread.isAlive()) {
                timeoutThread.interrupt();
                System.out.println("[CONFIG] Cancelled timeout thread during cleanup");
            }
            
            Stage stage = (Stage) startGameButton.getScene().getWindow();
            if (stage != null) {
                stage.close();
                System.out.println("[CONFIG] Configuration window closed after GAME_STARTED");
            }
        });
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
        // Set initial timer value to match the default selection
        gameConfig.setRoundDurationSeconds(120);
        
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
        System.out.println("[CONFIG] updateCategoryDisplay() called");
        List<Category> categories = categoryService.getEnabledCategories();
        System.out.println("[CONFIG] Enabled categories from service: " + categories.size());
        
        // Ensure at least some basic categories are enabled for testing
        if (categories.isEmpty()) {
            System.out.println("[CONFIG] No categories enabled - enabling default categories");
            List<Category> allCategories = categoryService.getAllCategories();
            System.out.println("[CONFIG] All available categories: " + allCategories.size());
            
            if (!allCategories.isEmpty()) {
                // Enable the first 3 categories by default for easier testing
                for (int i = 0; i < Math.min(3, allCategories.size()); i++) {
                    Category cat = allCategories.get(i);
                    try {
                        System.out.println("[CONFIG] Attempting to enable category: " + cat.getName() + " (ID: " + cat.getId() + ")");
                        categoryService.enableCategory(cat.getId());
                        System.out.println("[CONFIG] Auto-enabled category: " + cat.getName());
                    } catch (Exception e) {
                        System.err.println("[CONFIG] Failed to enable category " + cat.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                // Refresh the categories list
                categories = categoryService.getEnabledCategories();
                System.out.println("[CONFIG] Categories after auto-enable: " + categories.size());
            } else {
                System.err.println("[CONFIG] ERROR: No categories available at all!");
            }
        }
        
        System.out.println("[CONFIG] Final categories to set: " + categories.size());
        gameConfig.setSelectedCategories(categories);
        
        categoryCountLabel.setText(categories.size() + " catégories sélectionnées");
        
        categoryChipsContainer.getChildren().clear();
        for (Category category : categories) {
            Label chip = new Label(category.getIcon() + " " + category.displayName());
            chip.getStyleClass().add("category-chip");
            categoryChipsContainer.getChildren().add(chip);
        }
        
        System.out.println("[CONFIG] Calling validateConfiguration after category update");
        validateConfiguration();
    }
    
    private void validateConfiguration() {
        System.out.println("[CONFIG] validateConfiguration called");
        System.out.println("[CONFIG] Lobby controller present: " + (lobbyController != null));
        
        boolean isValid;
        
        // Special validation for multiplayer mode - less restrictive
        if (lobbyController != null) {
            // For multiplayer, only require categories to be selected
            isValid = gameConfig.getSelectedCategories() != null && 
                     !gameConfig.getSelectedCategories().isEmpty();
            System.out.println("[CONFIG] Multiplayer mode - Categories count: " + 
                (gameConfig.getSelectedCategories() != null ? gameConfig.getSelectedCategories().size() : "null"));
        } else {
            // Standard validation for local/solo modes
            isValid = gameConfig.isValid();
            System.out.println("[CONFIG] Standard mode - Valid: " + isValid);
        }
        
        System.out.println("[CONFIG] Configuration valid: " + isValid + ", Button will be " + (isValid ? "enabled" : "disabled"));
        startGameButton.setDisable(!isValid);
        
        if (!isValid) {
            String message;
            if (lobbyController != null) {
                message = "Sélectionnez au moins une catégorie pour démarrer";
            } else {
                message = gameConfig.getValidationMessage();
            }
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
            Scene scene = new Scene(root, 700, 500);
            
            ThemeManager.applySavedTheme(scene);
            categoryStage.setScene(scene);
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
        // Cleanup: Remove event listener when going back
        if (lobbyController != null && lobbyController.getMultiplayerService() != null) {
            lobbyController.getMultiplayerService().removeEventListener(this);
            System.out.println("[CONFIG] Removed WebSocket event listener when going back");
        }
        
        // Cancel timeout thread if still running
        if (timeoutThread != null && timeoutThread.isAlive()) {
            timeoutThread.interrupt();
            System.out.println("[CONFIG] Cancelled timeout thread when going back");
        }
        
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            
            // Check if we're in multiplayer mode - return to lobby instead of main menu
            if (lobbyController != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MultiplayerLobby.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 1000, 750);
                
                if (darkMode) {
                    scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
                } else {
                    scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
                }
                
                // Restore lobby controller state
                MultiplayerLobbyController restoredController = loader.getController();
                if (restoredController != null) {
                    restoredController.setDarkMode(darkMode);
                    // Copy session state from original lobby controller
                    restoredController.restoreSessionState(multiplayerSessionId, multiplayerPlayerName, true);
                }
                
                stage.setScene(scene);
                stage.show();
                return;
            }
            
            // Default behavior - return to main menu
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MainMenu.fxml"));
            Parent root = loader.load();
            
            ThemeManager.switchToFullScreenScene(stage, root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleStartGame() {
        System.out.println("[CONFIG] handleStartGame called - Button clicked!");
        System.out.println("[CONFIG] Game mode: " + gameConfig.getMode());
        System.out.println("[CONFIG] Lobby controller present: " + (lobbyController != null));
        
        // Use same validation logic as validateConfiguration()
        boolean isValid;
        if (lobbyController != null) {
            // For multiplayer, only require categories to be selected
            List<Category> selectedCategories = gameConfig.getSelectedCategories();
            System.out.println("[CONFIG] Multiplayer validation - Selected categories: " + 
                (selectedCategories != null ? selectedCategories.size() + " categories" : "NULL"));
            if (selectedCategories != null) {
                for (Category cat : selectedCategories) {
                    System.out.println("[CONFIG]   - Category: " + cat.getName() + " (ID: " + cat.getId() + ")");
                }
            }
            
            isValid = selectedCategories != null && !selectedCategories.isEmpty();
            System.out.println("[CONFIG] Multiplayer validation result: " + isValid);
        } else {
            // Standard validation for local/solo modes
            isValid = gameConfig.isValid();
            System.out.println("[CONFIG] Standard validation result: " + isValid);
            if (!isValid) {
                System.out.println("[CONFIG] Validation message: " + gameConfig.getValidationMessage());
            }
        }
        
        System.out.println("[CONFIG] Final validation result: " + isValid);
        
        if (!isValid) {
            System.out.println("[CONFIG] Game config is not valid, returning early");
            return;
        }
        
        try {
            Stage stage = (Stage) startGameButton.getScene().getWindow();
            
            switch (gameConfig.getMode()) {
                case SOLO -> startSoloGame(stage);
                case LOCAL -> startLocalGame(stage);
                case DISTANT -> {
                    if (lobbyController != null) {
                        // Multiplayer mode - send configuration via WebSocket START_GAME
                        startMultiplayerGame();
                    } else {
                        showError("Erreur", "Contrôleur de lobby non disponible");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de démarrer la partie");
        }
    }
    
    private void startSoloGame(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/GameView.fxml"));
        Parent root = loader.load();
        
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
            ThemeManager.switchToFullScreenScene(stage, root);
            stage.show();
            
            // Start the game after the scene is shown
            Platform.runLater(() -> {
                gc.startGameAfterSceneShown();
            });
        } else {
            ThemeManager.switchToFullScreenScene(stage, root);
            stage.show();
        }
    }
    
    private void startLocalGame(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MultiplayerGame.fxml"));
        Parent root = loader.load();
        
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
            ThemeManager.switchToFullScreenScene(stage, root);
            stage.show();
            
            // Start the game after the scene is shown
            Platform.runLater(() -> {
                mgc.startGameAfterSceneShown();
            });
        } else {
            ThemeManager.switchToFullScreenScene(stage, root);
            stage.show();
        }
    }
    
    private void showError(String title, String message) {
        DialogHelper.showError(title, null, message);
    }
    
    /**
     * Start multiplayer game by sending configuration via WebSocket
     */
    private void startMultiplayerGame() {
        if (lobbyController == null || multiplayerSessionId == null) {
            showError("Erreur", "Session multijoueur non disponible");
            return;
        }
        
        // Get multiplayer service from lobby controller
        var multiplayerService = lobbyController.getMultiplayerService();
        if (multiplayerService == null) {
            showError("Erreur", "Service multijoueur non disponible");
            return;
        }
        
        // Check WebSocket connection
        System.out.println("[CONFIG] Checking WebSocket connection status...");
        boolean isConnected = multiplayerService.isConnected();
        System.out.println("[CONFIG] WebSocket connected: " + isConnected);
        
        if (!isConnected) {
            System.err.println("[CONFIG] WebSocket not connected - attempting to reconnect...");
            showError("Erreur", "WebSocket non connecté. Veuillez vérifier la connexion au serveur.");
            return;
        }
        
        // Collect configuration
        int numberOfRounds = (int) roundsSlider.getValue();
        int roundDuration = parseDuration(durationComboBox.getValue());
        List<String> categoryNames = gameConfig.getSelectedCategories().stream()
            .map(Category::getName)
            .collect(java.util.stream.Collectors.toList());
        
        System.out.println("[CONFIG] === SENDING START_GAME PAYLOAD ===");
        System.out.println("[CONFIG] Session ID: " + multiplayerSessionId);
        System.out.println("[CONFIG] Player name: " + multiplayerPlayerName);
        System.out.println("[CONFIG] Number of rounds: " + numberOfRounds);
        System.out.println("[CONFIG] Round duration: " + roundDuration + "s");
        System.out.println("[CONFIG] Categories (" + categoryNames.size() + "):");
        for (String catName : categoryNames) {
            System.out.println("[CONFIG]   - " + catName);
        }
        System.out.println("[CONFIG] WebSocket connected: " + multiplayerService.isConnected());
        System.out.println("[CONFIG] =====================================");
        
        try {
            // Send START_GAME message via WebSocket
            System.out.println("[CONFIG] About to call multiplayerService.startGame()...");
            multiplayerService.startGame(numberOfRounds, roundDuration, categoryNames);
            
            // Show confirmation to user
            startGameButton.setText("Envoi en cours...");
            startGameButton.setDisable(true);
            
            System.out.println("[CONFIG] START_GAME message sent successfully");
            System.out.println("[CONFIG] Waiting for GAME_STARTED response from server...");
            
            // Add a simple timeout mechanism with cancellation support
            timeoutThread = new Thread(() -> {
                try {
                    Thread.sleep(15000); // Wait 15 seconds
                    Platform.runLater(() -> {
                        if (startGameButton.getText().equals("Envoi en cours...")) {
                            System.err.println("[CONFIG] Timeout waiting for GAME_STARTED - resetting button");
                            startGameButton.setText("Confirmer et Démarrer");
                            startGameButton.setDisable(false);
                            showError("Timeout", "Le serveur ne répond pas. Veuillez réessayer.");
                        }
                    });
                } catch (InterruptedException e) {
                    // Thread interrupted - probably because game started
                    System.out.println("[CONFIG] Timeout thread interrupted - game started successfully");
                }
            });
            timeoutThread.start();
            
            // Don't close window immediately - wait for GAME_STARTED event
            // The MultiplayerLobbyController will handle the transition when GAME_STARTED is received
            
        } catch (Exception e) {
            System.err.println("[CONFIG] Error sending START_GAME: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur", "Erreur lors de l'envoi de la configuration: " + e.getMessage());
            
            // Reset button state
            startGameButton.setText("Confirmer et Démarrer");
            startGameButton.setDisable(false);
        }
    }
    
    /**
     * Parse duration string to seconds
     */
    private int parseDuration(String durationString) {
        if (durationString == null) return 60;
        
        try {
            if (durationString.contains("min")) {
                String[] parts = durationString.split(" ");
                int minutes = Integer.parseInt(parts[0]);
                return minutes * 60;
            } else if (durationString.contains("s")) {
                return Integer.parseInt(durationString.replace("s", ""));
            } else {
                return Integer.parseInt(durationString);
            }
        } catch (NumberFormatException e) {
            return 60; // Default to 60 seconds
        }
    }

    // ==========================
    // MultiplayerEventListener Implementation
    // ==========================
    
    @Override
    public void onPlayerJoined(String playerName) {
        // Not needed for configuration screen
    }

    @Override
    public void onGameStarted(String letter, List<String> categories, int duration) {
        System.out.println("[CONFIG] *** GAME_STARTED event received by host! ***");
        System.out.println("[CONFIG] Letter: " + letter + ", Categories: " + categories.size() + ", Duration: " + duration + "s");
        
        // Cancel timeout thread
        if (timeoutThread != null && timeoutThread.isAlive()) {
            System.out.println("[CONFIG] Cancelling timeout thread - GAME_STARTED received successfully");
            timeoutThread.interrupt();
        }
        
        Platform.runLater(() -> {
            try {
                Stage stage = (Stage) startGameButton.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MultiplayerGame.fxml"));
                Parent root = loader.load();
                
                // Configure MultiplayerGameController
                Object controller = loader.getController();
                if (controller instanceof MultiplayerGameController mgc) {
                    mgc.setDarkMode(darkMode);
                    if (lobbyController != null && lobbyController.getMultiplayerService() != null) {
                        mgc.initializeMultiplayerGame(lobbyController.getMultiplayerService(), letter, categories, duration);
                    }
                }
                
                Scene scene = new Scene(root, 1200, 800);
                
                // Apply current theme
                if (darkMode) {
                    scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
                } else {
                    scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
                }
                
                stage.setScene(scene);
                stage.show();
                
                System.out.println("[CONFIG] Successfully transitioned to multiplayer game for host");
                
            } catch (IOException e) {
                System.err.println("[CONFIG] Error transitioning to game: " + e.getMessage());
                e.printStackTrace();
                showError("Erreur", "Erreur lors du chargement du jeu: " + e.getMessage());
                
                // Reset button state if transition failed
                startGameButton.setText("Confirmer et Démarrer");
                startGameButton.setDisable(false);
            }
        });
    }

    @Override
    public void onRoundEnded() {
        // Not needed for configuration screen
    }

    @Override
    public void onGameEnded(com.fasterxml.jackson.databind.JsonNode leaderboard) {
        // Not needed for configuration screen
    }
}