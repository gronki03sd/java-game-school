package com.baccalaureat.controller;

/*
 * MULTIPLAYER MANUAL TEST CHECKLIST
 * 
 * HOW TO TEST REMOTE MULTIPLAYER WITH TWO JavaFX CLIENTS:
 * 
 * 1. START SERVER:
 *    - Navigate to bac-game-server directory
 *    - Run: .\mvnw.cmd spring-boot:run
 *    - Verify server starts on localhost:8080
 * 
 * 2. START TWO JavaFX CLIENTS:
 *    - Open two terminal windows in java-game-school directory
 *    - Run in both: mvn javafx:run
 *    - Both clients should show main menu
 * 
 * 3. TEST SCENARIO - HOST CREATES SESSION:
 *    - CLIENT 1: Click "Multijoueur" button
 *    - CLIENT 1: Click "Se connecter au serveur"
 *    - CLIENT 1: Verify connection status shows "Connecté"
 *    - CLIENT 1: Click "Créer une partie"
 *    - CLIENT 1: Verify session ID appears (e.g., "ABC123")
 *    - CLIENT 1: Verify status shows "Hôte" (Host)
 * 
 * 4. TEST SCENARIO - REMOTE PLAYER JOINS:
 *    - CLIENT 2: Click "Multijoueur" button
 *    - CLIENT 2: Click "Se connecter au serveur"
 *    - CLIENT 2: Verify connection status shows "Connecté"
 *    - CLIENT 2: Enter session ID from CLIENT 1
 *    - CLIENT 2: Click "Rejoindre la partie"
 *    - CLIENT 1: Verify player list shows both players
 *    - CLIENT 2: Verify player list shows both players
 * 
 * 5. TEST SCENARIO - GAME START:
 *    - CLIENT 1 (Host): Click "Commencer la partie"
 *    - BOTH CLIENTS: Verify navigation to game screen
 *    - BOTH CLIENTS: Verify same letter displayed
 *    - BOTH CLIENTS: Verify same categories shown
 *    - BOTH CLIENTS: Verify countdown timer starts
 * 
 * 6. TEST SCENARIO - ROUND END:
 *    - BOTH CLIENTS: Enter answers or wait for timer
 *    - BOTH CLIENTS: Verify timer stops exactly once
 *    - BOTH CLIENTS: Verify round ends exactly once
 *    - BOTH CLIENTS: Verify results dialog appears exactly once
 * 
 * 7. TEST SCENARIO - RESULTS & NAVIGATION:
 *    - BOTH CLIENTS: Verify results dialog is scrollable
 *    - BOTH CLIENTS: Verify "Next Round" and "Back to Lobby" buttons
 *    - CLIENT 1: Click "Back to Lobby"
 *    - CLIENT 2: Click "Back to Lobby"
 *    - BOTH CLIENTS: Verify return to multiplayer lobby
 * 
 * EXPECTED DIAGNOSTIC LOGS:
 * [WS] Connected
 * [LOBBY] Host created session
 * [LOBBY] Player joined session
 * [GAME] GAME_STARTED received
 * [SYNC] Letter and categories applied
 * [TIMER] Started
 * [TIMER] Stopped
 * [ROUND] Ended
 * [RESULTS] Received
 * [DIALOG] Results displayed
 * 
 * SAFETY WARNINGS TO WATCH FOR:
 * [WARN] Duplicate call prevented - multiple timers
 * [WARN] Duplicate call prevented - multiple round endings
 * [WARN] Duplicate call prevented - multiple result dialogs
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.GameConfig;
import com.baccalaureat.model.GameSession;
import com.baccalaureat.model.Player;
import com.baccalaureat.model.RoundState;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.multiplayer.MultiplayerEventListener;
import com.baccalaureat.multiplayer.MultiplayerService;
import com.baccalaureat.service.CategoryService;
import com.baccalaureat.service.ValidationService;
import com.baccalaureat.util.DialogHelper;
import com.baccalaureat.util.ThemeManager;
import com.fasterxml.jackson.databind.JsonNode;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MultiplayerGameController implements MultiplayerEventListener {
    private boolean darkMode = false;
    private MultiplayerService multiplayerService;
    
    // SAFETY GUARDS - Prevent duplicate operations
    private boolean countdownStarted = false;
    private boolean roundEnded = false;
    private boolean resultsDialogShown = false;
    @FXML private Label letterLabel;
    @FXML private Label timerLabel;
    @FXML private Label roundLabel;
    @FXML private Label currentPlayerLabel;
    @FXML private VBox categoriesContainer;
    @FXML private HBox scoresBar;
    @FXML private Button validateButton;
    @FXML private Button backButton;
    @FXML private ProgressBar timerProgress;

    private final ValidationService validationService = new ValidationService();
    private List<Player> players;
    private int currentPlayerIndex = 0;
    private GameSession session;
    private final Map<Category, TextField> inputFields = new HashMap<>();
    private final Map<Category, Label> statusLabels = new HashMap<>();
    private final Map<Player, VBox> playerScoreCards = new HashMap<>();

    private Timeline countdown;
    private RoundState roundState = RoundState.INIT; // Add round state management
    private int remainingSeconds;
    private int totalSeconds;
    private int currentRound = 1;
    private int totalRounds;
    private List<Category> categories;
    private String currentLetter;
    private final List<String> usedLetters = new ArrayList<>();

    @FXML
    private void initialize() {
        // Initialize UI only - game configuration comes from configureGame() method
        // This fixes the conflict between lobby-based and configuration-based initialization
        
        // Apply theme (scene may not be ready yet)
        Scene scene = letterLabel.getScene();
        if (scene != null) {
            scene.getStylesheets().removeIf(s -> s.contains("theme-light.css") || s.contains("theme-dark.css"));
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
        }
        
        // Game setup will happen via configureGame() call from GameConfigurationController
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
    }
    
    public void configureGame(GameConfig config) {
        // Convert GameConfig to list of players
        List<Player> configuredPlayers = new ArrayList<>();
        for (String nickname : config.getPlayerNicknames()) {
            configuredPlayers.add(new Player(nickname));
        }
        
        // Store configuration settings
        this.players = configuredPlayers;
        this.categories = new ArrayList<>(config.getSelectedCategories());
        this.totalRounds = config.getNumberOfRounds();
        this.totalSeconds = config.getRoundDurationSeconds();
        this.remainingSeconds = totalSeconds;
        
        // Setup the game
        generateNewLetter();
        setupUI();
        setupScoresBar();
        // Don't start the player turn yet - wait for scene to be fully shown
        // This will be started by calling startGameAfterSceneShown()
    }
    
    public void startGameAfterSceneShown() {
        startPlayerTurn();
    }

    private void generateNewLetter() {
        Random r = new Random();
        String excludeLetters = "WXYZ";
        char letter;
        do {
            letter = (char) ('A' + r.nextInt(26));
        } while (excludeLetters.indexOf(letter) >= 0 || usedLetters.contains(String.valueOf(letter)));

        currentLetter = String.valueOf(letter);
        usedLetters.add(currentLetter);
    }

    private void setupUI() {
        letterLabel.setText(currentLetter);
        roundLabel.setText("%d/%d".formatted(currentRound, totalRounds));
        timerProgress.setProgress(1.0);

        // Clear categories
        categoriesContainer.getChildren().clear();
        inputFields.clear();
        statusLabels.clear();

        // Build category cards
        for (Category c : categories) {
            HBox card = createCategoryCard(c);
            categoriesContainer.getChildren().add(card);
        }

        validateButton.setDisable(false);
    }

    private HBox createCategoryCard(Category category) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("category-card");
        card.setPadding(new Insets(12, 20, 12, 20));

        Label iconLabel = new Label(category.getIcon());
        iconLabel.setStyle("-fx-font-size: 32px;");
        iconLabel.setMinWidth(50);

        VBox infoBox = new VBox(2);
        Label nameLabel = new Label(category.displayName());
        nameLabel.getStyleClass().add("category-name");
        Label hintLabel = new Label(category.getHint());
        hintLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 12px;");
        infoBox.getChildren().addAll(nameLabel, hintLabel);
        infoBox.setMinWidth(150);

        TextField tf = new TextField();
        tf.setPromptText("Mot en " + currentLetter + "...");
        tf.getStyleClass().add("category-input");
        tf.setPrefWidth(250);
        HBox.setHgrow(tf, Priority.ALWAYS);

        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            Label status = statusLabels.get(category);
            
            if (newVal != null && !newVal.trim().isEmpty()) {
                // Show pending status while typing (no validation until submit)
                status.setText("⏳");
                status.getStyleClass().removeAll("status-valid", "status-invalid", "status-uncertain");
                status.getStyleClass().add("status-pending");
                
                // Simple visual feedback for first letter (no validation)
                boolean startsCorrect = newVal.toUpperCase().startsWith(currentLetter);
                tf.setStyle(startsCorrect ? 
                    "" : "-fx-border-color: #ffa726; -fx-border-width: 2; -fx-border-radius: 10;");
            } else {
                // Empty input - show pending state
                status.setText("⏳");
                status.getStyleClass().removeAll("status-valid", "status-invalid", "status-uncertain");
                status.getStyleClass().add("status-pending");
                tf.setStyle("");
            }
        });

        inputFields.put(category, tf);

        Label status = new Label("⏳");
        status.setStyle("-fx-text-fill: #ffd93d; -fx-font-size: 24px;");
        status.setMinWidth(40);
        status.setAlignment(Pos.CENTER);
        statusLabels.put(category, status);

        card.getChildren().addAll(iconLabel, infoBox, tf, status);
        return card;
    }

    private void setupScoresBar() {
        scoresBar.getChildren().clear();
        playerScoreCards.clear();

        String[] colors = {"#e94560", "#4ecca3", "#ffd93d", "#6c5ce7", "#00cec9", "#fd79a8", "#a29bfe", "#ff7675"};
        int colorIndex = 0;

        for (Player player : players) {
            VBox card = createScoreCard(player, colors[colorIndex % colors.length]);
            playerScoreCards.put(player, card);
            scoresBar.getChildren().add(card);
            colorIndex++;
        }

        updateScoresBar();
    }

    private VBox createScoreCard(Player player, String color) {
        VBox card = new VBox(2);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(8, 15, 8, 15));
        card.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.05);" +
            "-fx-background-radius: 10;" +
            "-fx-border-radius: 10;"
        );

        Label nameLabel = new Label(player.getName());
        nameLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label scoreLabel = new Label(String.valueOf(player.getScore()));
        scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        scoreLabel.setUserData("score");

        card.getChildren().addAll(nameLabel, scoreLabel);
        return card;
    }

    private void updateScoresBar() {
        Player current = players.get(currentPlayerIndex);
        
        for (Map.Entry<Player, VBox> entry : playerScoreCards.entrySet()) {
            Player p = entry.getKey();
            VBox card = entry.getValue();

            // Update score
            for (javafx.scene.Node node : card.getChildren()) {
                if (node instanceof Label label && "score".equals(label.getUserData())) {
                    label.setText(String.valueOf(p.getScore()));
                }
            }

            // Highlight current player
            if (p == current) {
                card.setStyle(
                    "-fx-background-color: rgba(78, 204, 163, 0.2);" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-color: #4ecca3;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 10;"
                );
            } else {
                card.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.05);" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-radius: 10;"
                );
            }
        }
    }

    private void startPlayerTurn() {
        Player current = players.get(currentPlayerIndex);
        current.resetForNewRound();
        
        roundState = RoundState.RUNNING; // Mark round as active
        
        currentPlayerLabel.setText(current.getName());
        remainingSeconds = totalSeconds;
        timerLabel.setText(formatTime(remainingSeconds));
        timerLabel.getStyleClass().remove("timer-warning");
        timerProgress.setProgress(1.0);

        // Clear inputs
        for (TextField tf : inputFields.values()) {
            tf.clear();
            tf.setDisable(false);
            tf.setStyle("");
        }
        for (Label status : statusLabels.values()) {
            status.setText("⏳");
            status.setStyle("-fx-text-fill: #ffd93d; -fx-font-size: 24px;");
        }

        validateButton.setDisable(false);
        updateScoresBar();
        animateLetterReveal();
        startCountdown();
    }

    private void animateLetterReveal() {
        ScaleTransition scale = new ScaleTransition(Duration.millis(500), letterLabel);
        scale.setFromX(0);
        scale.setFromY(0);
        scale.setToX(1);
        scale.setToY(1);
        scale.play();
    }

    private void startCountdown() {
        // SAFETY GUARD - Prevent multiple countdown timers
        if (countdownStarted) {
            System.out.println("[WARN] Duplicate call prevented - startCountdown() already called");
            return;
        }
        countdownStarted = true;
        System.out.println("[TIMER] Started - Duration: " + remainingSeconds + "s");
        
        if (countdown != null) countdown.stop();

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            timerLabel.setText(formatTime(remainingSeconds));
            timerProgress.setProgress((double) remainingSeconds / totalSeconds);

            if (remainingSeconds <= 10) {
                timerLabel.getStyleClass().add("timer-warning");
            }

            if (remainingSeconds <= 0) {
                countdown.stop();
                System.out.println("[TIMER] Stopped - Time expired");
                roundState = RoundState.FINISHED; // Mark round as finished
                handleValidate();
            }
        }));
        countdown.setCycleCount(remainingSeconds);
        countdown.playFromStart();
    }

    @FXML
    private void handleValidate() {
        // Guard against validation after round end
        if (roundState != RoundState.RUNNING) {
            return; // Round already ended or not running
        }
        
        if (countdown != null) countdown.stop();
        roundState = RoundState.FINISHED; // Mark round as finished

        Player current = players.get(currentPlayerIndex);
        int points = 0;

        // Clear used words tracker for this round (same as solo mode)
        Set<String> usedWordsThisRound = new HashSet<>();

        // Collect and validate answers using EXACT same pipeline as solo mode
        for (Category c : categories) {
            TextField tf = inputFields.get(c);
            String word = tf.getText() != null ? tf.getText().trim() : "";
            Label status = statusLabels.get(c);

            current.setAnswer(c, word);

            // Use EXACT same validation pipeline as solo mode
            ValidationResult result = validateWordComplete(word, c, usedWordsThisRound);
            
            // Apply visual feedback based on validation result
            applyValidationStatus(result, status);
            
            // Use EXACT same scoring as solo mode
            if (result.isValid()) {
                points += 1;  // Same as solo mode: +1 per correct answer
                usedWordsThisRound.add(word.trim().toLowerCase()); // Track for duplicates
            }

            tf.setDisable(true);
        }

        current.addPoints(points);
        current.setFinished(true);
        validateButton.setDisable(true);
        updateScoresBar();

        // Show turn result
        showTurnResult(current, points);
    }

    /**
     * Complete word validation using EXACT same pipeline as solo mode.
     * This ensures multiplayer behaves identically to solo mode.
     */
    private ValidationResult validateWordComplete(String word, Category category, Set<String> usedWordsThisRound) {
        // Step 1: Basic input validation (same as solo)
        if (word == null || word.trim().isEmpty()) {
            return new ValidationResult(ValidationStatus.INVALID, 0.0, "UI", "Empty word");
        }
        
        String normalizedWord = word.trim().toLowerCase();
        String requiredStart = currentLetter.toLowerCase();
        
        // Step 2: First letter check (same as solo)
        if (!normalizedWord.startsWith(requiredStart)) {
            return new ValidationResult(ValidationStatus.INVALID, 0.0, "UI", 
                "Word must start with '" + currentLetter + "'");
        }
        
        // Step 3: Duplicate check within current round (same as solo)
        if (usedWordsThisRound.contains(normalizedWord)) {
            return new ValidationResult(ValidationStatus.INVALID, 0.0, "UI", "Duplicate word in this round");
        }
        
        // Step 4: BACKEND VALIDATION via ValidationService (EXACT same as solo)
        ValidationResult backendResult = validationService.validateWord(category.name(), word);
        
        return backendResult;
    }

    /**
     * Apply visual validation status to status label using CSS classes.
     * This provides consistent visual feedback across single and multiplayer modes.
     */
    private void applyValidationStatus(ValidationResult result, Label status) {
        // Determine if this is a low-confidence valid result
        boolean isLowConfidenceValid = result.isValid() && result.getConfidence() < 0.85;
        
        // Update status icon with nuanced feedback
        String statusIcon = switch (result.getStatus()) {
            case VALID -> isLowConfidenceValid ? "⚠️" : "✅";  // Warning for low confidence
            case INVALID -> "❌"; 
            case UNCERTAIN -> "❓";
            case ERROR -> "⚠️";
        };
        status.setText(statusIcon);
        
        // Update CSS classes for consistent theming
        status.getStyleClass().removeAll("status-valid", "status-invalid", "status-uncertain", "status-pending");
        String cssClass;
        if (result.isValid() && !isLowConfidenceValid) {
            cssClass = "status-valid";     // Green for high-confidence valid
        } else if (result.isValid() && isLowConfidenceValid || result.isUncertain()) {
            cssClass = "status-uncertain"; // Orange for low-confidence valid or uncertain
        } else {
            cssClass = "status-invalid";   // Red for invalid/error
        }
        status.getStyleClass().add(cssClass);
    }

    private void showTurnResult(Player player, int points) {
        ButtonType nextBtn = new ButtonType("Continuer →", ButtonBar.ButtonData.OK_DONE);
        DialogHelper.showConfirmation(
            "Tour terminé",
            player.getName() + (points > 0 ? " - Bravo! 🎉" : " - Dommage! 😅"),
            "Points gagnés: +" + points + "\nScore total: " + player.getScore(),
            nextBtn
        );

        proceedToNext();
    }

    private void proceedToNext() {
        currentPlayerIndex++;

        if (currentPlayerIndex >= players.size()) {
            // All players finished this round
            currentPlayerIndex = 0;
            
            if (currentRound >= totalRounds) {
                // Game over
                showFinalResults();
            } else {
                // Next round
                currentRound++;
                generateNewLetter();
                showRoundSummary();
            }
        } else {
            // Next player
            startPlayerTurn();
        }
    }

    private void showRoundSummary() {
        StringBuilder sb = new StringBuilder("Scores après la manche " + (currentRound - 1) + ":\n\n");
        
        List<Player> sorted = new ArrayList<>(players);
        sorted.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        
        int rank = 1;
        for (Player p : sorted) {
            String medal = switch (rank) {
                case 1 -> "🥇 ";
                case 2 -> "🥈 ";
                case 3 -> "🥉 ";
                default -> rank + ". ";
            };
            sb.append(medal).append(p.getName()).append(": ").append(p.getScore()).append(" pts\n");
            rank++;
        }

        ButtonType nextRound = new ButtonType("Manche " + currentRound + " →", ButtonBar.ButtonData.OK_DONE);
        DialogHelper.showConfirmation(
            "Fin de manche",
            "Manche " + (currentRound - 1) + " terminée!",
            sb.toString() + "\nProchaine lettre: " + currentLetter,
            nextRound
        );

        setupUI();
        startPlayerTurn();
    }

    private void showFinalResults() {
        GameSession.incrementGamesPlayed();
        
        List<Player> sorted = new ArrayList<>(players);
        sorted.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        
        Player winner = sorted.get(0);
        GameSession.updateHighScore(winner.getScore());

        StringBuilder sb = new StringBuilder();
        int rank = 1;
        for (Player p : sorted) {
            String medal = switch (rank) {
                case 1 -> "🥇 ";
                case 2 -> "🥈 ";
                case 3 -> "🥉 ";
                default -> rank + ". ";
            };
            sb.append(medal).append(p.getName()).append(": ").append(p.getScore()).append(" pts\n");
            rank++;
        }

        ButtonType playAgain = new ButtonType("Rejouer", ButtonBar.ButtonData.OK_DONE);
        ButtonType menu = new ButtonType("Menu principal", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        Optional<ButtonType> result = DialogHelper.showConfirmation(
            "🏆 Partie terminée!",
            "👑 " + winner.getName() + " gagne avec " + winner.getScore() + " points!",
            "Classement final:\n\n" + sb.toString(),
            playAgain, menu
        );
        if (result.isPresent() && result.get() == playAgain) {
            restartGame();
        } else {
            navigateToMenu();
        }
    }

    private void restartGame() {
        for (Player p : players) {
            p.resetForNewGame();
        }
        currentPlayerIndex = 0;
        currentRound = 1;
        usedLetters.clear();
        generateNewLetter();
        setupUI();
        setupScoresBar();
        startPlayerTurn();
    }

    private void navigateToMenu() {
        if (countdown != null) countdown.stop();
        try {
            Stage stage = (Stage) letterLabel.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/baccalaureat/MainMenu.fxml"));
            ThemeManager.switchToFullScreenScene(stage, root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToMenu() {
        try {
            if (countdown != null) countdown.stop();
            roundState = RoundState.INIT; // Reset round state
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MainMenu.fxml"));
            Parent root = loader.load();
            
            // Use ThemeManager for proper theme application
            ThemeManager.switchToFullScreenScene(stage, root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatTime(int seconds) {
        if (seconds < 0) seconds = 0;
        int m = seconds / 60;
        int s = seconds % 60;
        return "%02d:%02d".formatted(m, s);
    }
    
    /**
     * Initialize multiplayer game with server-provided data.
     * This integrates remote multiplayer with existing game logic.
     */
    public void initializeMultiplayerGame(MultiplayerService multiplayerService, 
                                          String letter, List<String> categoryNames, int duration) {
        
        // Extract additional parameters from the server by inspecting the last received message
        // This is a temporary solution until we can modify the interface properly
        int totalRounds = extractTotalRoundsFromService(multiplayerService);
        int currentRound = 1; // Start with round 1
        
        System.out.println("[GAME] GAME_STARTED received - Letter: " + letter + ", Categories: " + categoryNames.size() + ", Duration: " + duration + "s, Total Rounds: " + totalRounds);
        
        // RESET SAFETY GUARDS for new game
        countdownStarted = false;
        roundEnded = false;
        resultsDialogShown = false;
        
        this.multiplayerService = multiplayerService;
        this.multiplayerService.addEventListener(this);
        
        // Create players list (only local player for now)
        List<Player> multiplayerPlayers = new ArrayList<>();
        String localPlayerName = multiplayerService.getCurrentPlayerName();
        if (localPlayerName != null) {
            multiplayerPlayers.add(new Player(localPlayerName));
        } else {
            multiplayerPlayers.add(new Player("Joueur"));
        }
        
        // Convert category names to proper Category objects using CategoryService
        List<Category> gameCategories = new ArrayList<>();
        CategoryService categoryService = new CategoryService();
        for (String catName : categoryNames) {
            // Try to find existing category by name for proper icons and descriptions
            Optional<Category> existingCategory = findCategoryByName(catName, categoryService);
            if (existingCategory.isPresent()) {
                gameCategories.add(existingCategory.get());
            } else {
                // Create minimal Category if not found in database
                gameCategories.add(new Category(catName, catName, "📝", "Catégorie: " + catName));
            }
        }
        
        // Initialize game session with server-provided data (same as solo mode)
        this.players = multiplayerPlayers;
        this.categories = gameCategories;
        this.currentLetter = letter.toUpperCase();
        this.totalSeconds = duration;
        this.remainingSeconds = duration;
        this.totalRounds = totalRounds; // Use server-provided rounds
        this.currentRound = currentRound; // Use server-provided round number
        
        System.out.println("[SYNC] Game configured - Letter: " + currentLetter + ", Categories: " + categories.size() + ", Rounds: " + totalRounds + ", Duration: " + totalSeconds + "s");
        
        // Create game session using existing game logic (same approach as solo mode)
        this.session = new GameSession();
        // Note: In multiplayer, GameSession is only used for basic game state tracking
        // Server manages the actual game flow, but we use same validation pipeline
        
        // Initialize UI (same as solo mode)
        initializeUI();
        
        // Start the round (same as solo mode)
        startPlayerTurn();
    }
    
    /**
     * Extract total rounds from MultiplayerService (temporary solution)
     */
    private int extractTotalRoundsFromService(MultiplayerService service) {
        // For now, return default value - this should be improved to extract from server data
        // TODO: Implement proper extraction of totalRounds from last received message
        return 3; // Default typical game rounds
    }
    
    /**
     * Find existing category by name for proper icons and descriptions
     */
    private Optional<Category> findCategoryByName(String categoryName, CategoryService categoryService) {
        try {
            // Use CategoryService to get all available categories
            List<Category> allCategories = categoryService.getAllCategories();
            return allCategories.stream()
                .filter(cat -> cat.getName().equalsIgnoreCase(categoryName) || 
                              cat.displayName().equalsIgnoreCase(categoryName))
                .findFirst();
        } catch (Exception e) {
            System.err.println("[GAME] Error finding category by name: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Initialize UI components
     */
    private void initializeUI() {
        // Display letter
        if (letterLabel != null) {
            letterLabel.setText(currentLetter);
        }
        
        // Display round info
        if (roundLabel != null) {
            roundLabel.setText("Manche " + currentRound + "/" + totalRounds);
        }
        
        // Display current player
        if (currentPlayerLabel != null && !players.isEmpty()) {
            currentPlayerLabel.setText("Joueur: " + players.get(currentPlayerIndex).getName());
        }
        
        // Setup categories
        setupCategoriesUI();
        
        // Setup scores bar
        setupScoresBar();
    }
    
    /**
     * Setup categories input fields
     */
    private void setupCategoriesUI() {
        if (categoriesContainer == null) return;
        
        categoriesContainer.getChildren().clear();
        inputFields.clear();
        statusLabels.clear();
        
        for (Category category : categories) {
            VBox categoryBox = new VBox(10);
            categoryBox.setPadding(new Insets(15));
            categoryBox.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.05);" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: rgba(255, 255, 255, 0.1);" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: 1;"
            );
            
            // Category name
            Label categoryLabel = new Label(category.getName());
            categoryLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
            
            // Input field
            TextField input = new TextField();
            input.setPromptText("Commence par " + currentLetter + "...");
            input.setStyle("-fx-font-size: 14px;");
            HBox.setHgrow(input, Priority.ALWAYS);
            
            // Status label
            Label statusLabel = new Label();
            statusLabel.setStyle("-fx-font-size: 12px;");
            
            inputFields.put(category, input);
            statusLabels.put(category, statusLabel);
            
            HBox inputRow = new HBox(10, input, statusLabel);
            inputRow.setAlignment(Pos.CENTER_LEFT);
            
            categoryBox.getChildren().addAll(categoryLabel, inputRow);
            categoriesContainer.getChildren().add(categoryBox);
        }
    }
    
    /**
     * Create score card for multiplayer with default color
     */
    private VBox createScoreCard(Player player) {
        return createScoreCard(player, "#4ecca3"); // Default green color for multiplayer
    }
    
    /**
     * MultiplayerEventListener implementation
     */
    @Override
    public void onRoundEnded() {
        javafx.application.Platform.runLater(() -> {
            // SAFETY GUARD - Prevent multiple round endings
            if (roundEnded) {
                System.out.println("[WARN] Duplicate call prevented - onRoundEnded() already called");
                return;
            }
            roundEnded = true;
            System.out.println("[ROUND] Ended - Server triggered round end");
            
            // Stop timer
            if (countdown != null) {
                countdown.stop();
                System.out.println("[TIMER] Stopped - Server round end");
            }
            
            // Collect and submit answers
            submitAnswersToServer();
        });
    }
    
    @Override
    public void onResultsReceived(JsonNode results) {
        javafx.application.Platform.runLater(() -> {
            System.out.println("[RESULTS] Received round results from server");
            showRoundResults(results);
        });
    }
    
    @Override
    public void onRoundStarted(String letter, int currentRound, int totalRounds) {
        javafx.application.Platform.runLater(() -> {
            System.out.println("[ROUND] New round started - Letter: " + letter + 
                              ", Round: " + currentRound + "/" + totalRounds);
            
            // Reset safety guards for new round
            countdownStarted = false;
            roundEnded = false;
            resultsDialogShown = false;
            
            // Update game state
            this.currentLetter = letter;
            this.currentRound = currentRound;
            this.totalRounds = totalRounds;
            
            // Update UI
            letterLabel.setText(letter);
            roundLabel.setText("Manche " + currentRound + "/" + totalRounds);
            
            // Clear input fields
            for (TextField field : inputFields.values()) {
                field.clear();
            }
            
            // Start countdown
            startPlayerTurn();
        });
    }
    
    @Override
    public void onGameEnded(JsonNode leaderboard) {
        javafx.application.Platform.runLater(() -> {
            System.out.println("[GAME] Game ended - Showing leaderboard");
            showLeaderboard(leaderboard);
        });
    }
    
    @Override
    public void onError(String errorMessage) {
        javafx.application.Platform.runLater(() -> {
            DialogHelper.showError("Erreur multijoueur", "Une erreur s'est produite", errorMessage);
        });
    }
    
    /**
     * Submit player answers to server at round end
     */
    private void submitAnswersToServer() {
        Map<String, String> answers = new HashMap<>();
        
        for (Map.Entry<Category, TextField> entry : inputFields.entrySet()) {
            String categoryName = entry.getKey().getName();
            String answer = entry.getValue().getText().trim();
            if (!answer.isEmpty()) {
                answers.put(categoryName, answer);
            }
        }
        
        System.out.println("[ROUND] Submitting " + answers.size() + " answers to server");
        if (multiplayerService != null) {
            multiplayerService.submitAnswers(answers);
        }
    }
    
    /**
     * Display multiplayer results dialog
     */
    private void showMultiplayerResults(JsonNode results) {
        System.out.println("[DIALOG] Results displayed");
        
        Alert alert = DialogHelper.createStyledAlert(
            Alert.AlertType.INFORMATION,
            "Résultats de la manche",
            "Fin de la manche!",
            ""
        );
        alert.setResizable(true);
        
        // Parse and display results
        StringBuilder content = new StringBuilder();
        content.append("=== RÉSULTATS MULTIJOUEUR ===\n\n");
        
        if (results != null && results.isObject()) {
            content.append("Données reçues du serveur:\n");
            results.fieldNames().forEachRemaining(field -> {
                content.append("• ").append(field).append(": ").append(results.get(field).asText()).append("\n");
            });
        } else {
            content.append("Résultats envoyés au serveur.\n");
            content.append("En attente des résultats des autres joueurs...\n");
        }
        
        content.append("\n=== VOS RÉPONSES ===\n");
        for (Map.Entry<Category, TextField> entry : inputFields.entrySet()) {
            String categoryName = entry.getKey().getName();
            String answer = entry.getValue().getText().trim();
            content.append("• ").append(categoryName).append(": ");
            content.append(answer.isEmpty() ? "(pas de réponse)" : answer).append("\n");
        }
        
        alert.setContentText(content.toString());
        
        // Add custom buttons
        ButtonType nextRoundBtn = new ButtonType("Manche suivante");
        ButtonType backToLobbyBtn = new ButtonType("Retour au lobby");
        alert.getButtonTypes().setAll(nextRoundBtn, backToLobbyBtn);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == nextRoundBtn) {
                System.out.println("[NAV] Next Round clicked - feature not implemented yet");
                handleBackToLobby(); // For now, go back to lobby
            } else if (result.get() == backToLobbyBtn) {
                System.out.println("[NAV] Back to Lobby clicked");
                handleBackToLobby();
            }
        }
    }
    
    /**
     * Display round results and allow progression to next round
     */
    private void showRoundResults(JsonNode results) {
        System.out.println("[DIALOG] Round results dialog");
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Résultats de la manche " + currentRound);
        alert.setHeaderText("Fin de la manche!");
        alert.setResizable(true);
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(400);
        
        StringBuilder content = new StringBuilder();
        content.append("=== RÉSULTATS MANCHE ").append(currentRound).append(" ===\n\n");
        
        if (results != null && results.isArray()) {
            content.append("🏆 CLASSEMENT:\n");
            int position = 1;
            for (JsonNode playerResult : results) {
                String name = playerResult.has("playerName") ? playerResult.get("playerName").asText() : "Joueur";
                int totalScore = playerResult.has("totalScore") ? playerResult.get("totalScore").asInt() : 0;
                int roundScore = playerResult.has("roundScore") ? playerResult.get("roundScore").asInt() : 0;
                
                String medal = position == 1 ? "🥇" : position == 2 ? "🥈" : position == 3 ? "🥉" : "  ";
                content.append(String.format("%s %d. %s - %d pts (+%d cette manche)\n", 
                              medal, position++, name, totalScore, roundScore));
            }
        }
        
        content.append("\n=== VOS RÉPONSES ===\n");
        for (Map.Entry<Category, TextField> entry : inputFields.entrySet()) {
            String categoryName = entry.getKey().getName();
            String answer = entry.getValue().getText().trim();
            content.append("• ").append(categoryName).append(": ");
            content.append(answer.isEmpty() ? "(pas de réponse)" : answer).append("\n");
        }
        
        alert.setContentText(content.toString());
        
        // Add custom buttons based on game state
        if (currentRound < totalRounds) {
            ButtonType nextRoundBtn = new ButtonType("Manche suivante");
            ButtonType exitBtn = new ButtonType("Quitter");
            alert.getButtonTypes().setAll(nextRoundBtn, exitBtn);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == nextRoundBtn) {
                    // Next round will be started by server automatically
                    System.out.println("[NAV] Waiting for next round from server");
                } else {
                    navigateToMainMenu();
                }
            }
        } else {
            // Last round - leaderboard will be shown automatically
            ButtonType okBtn = new ButtonType("Voir le classement final");
            alert.getButtonTypes().setAll(okBtn);
            alert.showAndWait();
        }
    }
    
    /**
     * Display final leaderboard at game end
     */
    private void showLeaderboard(JsonNode leaderboard) {
        System.out.println("[DIALOG] Final leaderboard");
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Classement Final");
        alert.setHeaderText("🏁 Partie Terminée!");
        alert.setResizable(true);
        alert.getDialogPane().setPrefWidth(650);
        alert.getDialogPane().setPrefHeight(450);
        
        StringBuilder content = new StringBuilder();
        content.append("🏆 CLASSEMENT FINAL 🏆\n\n");
        
        if (leaderboard != null && leaderboard.isArray()) {
            int position = 1;
            for (JsonNode playerScore : leaderboard) {
                String name = playerScore.has("playerName") ? playerScore.get("playerName").asText() : "Joueur";
                int totalScore = playerScore.has("totalScore") ? playerScore.get("totalScore").asInt() : 0;
                
                String decoration = "";
                if (position == 1) {
                    decoration = "👑 CHAMPION! 👑";
                } else if (position == 2) {
                    decoration = "🥈 Vice-Champion";
                } else if (position == 3) {
                    decoration = "🥉 Troisième place";
                }
                
                content.append(String.format("%d. %s - %d points %s\n", 
                              position++, name, totalScore, decoration));
            }
        }
        
        content.append("\n\nMerci d'avoir joué! 🎉");
        alert.setContentText(content.toString());
        
        // Add exit button
        ButtonType exitBtn = new ButtonType("Retour au Menu Principal");
        alert.getButtonTypes().setAll(exitBtn);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            navigateToMainMenu();
        }
    }
    
    /**
     * Navigate back to main menu
     */
    private void navigateToMainMenu() {
        System.out.println("[NAV] Returning to main menu");
        try {
            if (countdown != null) countdown.stop();
            
            Stage stage = (Stage) letterLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MainMenu.fxml"));
            Parent root = loader.load();
            
            // Use ThemeManager for proper theme application
            ThemeManager.switchToFullScreenScene(stage, root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Return to multiplayer lobby
     */
    private void handleBackToLobby() {
        System.out.println("[NAV] Returning to multiplayer lobby");
        try {
            Stage stage = (Stage) letterLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MultiplayerLobby.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 750);
            
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
            
            Object controller = loader.getController();
            if (controller instanceof MultiplayerLobbyController mlc) {
                mlc.setDarkMode(darkMode);
            }
            
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
