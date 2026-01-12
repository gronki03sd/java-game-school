package com.baccalaureat.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.baccalaureat.ai.CategorizationEngine;
import com.baccalaureat.model.Category;
import com.baccalaureat.model.GameConfig;
import com.baccalaureat.model.GameSession;
import com.baccalaureat.model.RoundState;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.service.CategoryService;
import com.baccalaureat.service.ValidationService;
import com.baccalaureat.util.DialogHelper;
import com.baccalaureat.util.ThemeManager;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

/**
 * GameController manages the Baccalauréat game UI and integrates with the backend validation pipeline.
 * 
 * KEY CHANGE: This controller now uses BACKEND VALIDATION instead of simple first-letter checks.
 * The old approach only validated that words started with the correct letter, which allowed
 * nonsense words to be highlighted as valid. Now every word is validated through the 
 * ValidationService which coordinates the complete validation pipeline.
 * 
 * VALIDATION PIPELINE:
 * 1. First letter check (preliminary filter)
 * 2. Duplicate detection within round
 * 3. Backend validation via ValidationService.validateWord()
 *    - Database cache lookup for performance
 *    - FixedListValidator: Deterministic validation with French word lists
 *    - WebConfigurableValidator: Web-based validation via DictionaryAPI.dev
 *    - SemanticAiValidator: Future AI-based validation (placeholder)
 * 
 * UI FEEDBACK BASED ON ValidationResult:
 * - VALID words: Green highlighting, confidence % shown
 * - INVALID words: Red highlighting, error reason shown
 * - UNCERTAIN results: Orange highlighting (API unavailable, low confidence)
 * - Low-confidence valid (<85%): Orange highlighting to indicate uncertainty
 * 
 * The UI now strictly follows backend validation results instead of misleading
 * orange highlights based on first-letter matches.
 */
public class GameController {
    private boolean darkMode = false;
    @FXML private Label letterLabel;
    @FXML private Label timerLabel;
    @FXML private Label scoreLabel;
    @FXML private Label roundLabel;
    @FXML private VBox categoriesContainer;
    @FXML private Button stopButton;
    @FXML private Button backButton;
    @FXML private Button hintButton;
    @FXML private Button skipButton;
    @FXML private ProgressBar timerProgress;

    // Backend validation service - coordinates full validation pipeline with caching
    private final CategoryService categoryService = new CategoryService();
    private final ValidationService validationService = new ValidationService();
    private final CategorizationEngine categorizationEngine = new CategorizationEngine(categoryService);
    private GameSession session;
    private final Map<Category, TextField> inputFields = new HashMap<>();
    private final Map<Category, Label> statusLabels = new HashMap<>();
    private final Map<Category, Label> confidenceLabels = new HashMap<>();
    private final Map<Category, HBox> categoryCards = new HashMap<>();
    private final Map<Category, ValidationResult> cachedResults = new HashMap<>();
    private final Set<String> usedWordsThisRound = new HashSet<>();

    private Timeline countdown;
    private boolean showingResults = false; // Guard to prevent dialog stacking
    private RoundState roundState = RoundState.INIT; // Authoritative round state
    private boolean hasScored = false; // Prevent multiple scoring
    private int remainingSeconds;
    private int totalSeconds;
    private int hintsUsed = 0;
    private static final int MAX_HINTS = 3;

    @FXML
    private void initialize() {
        // Initialize UI only - game configuration comes from configureGame() method
        // This prevents the default GameSession from overriding the configured timer duration
        
        // Apply theme
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
        session = new GameSession(config);
        totalSeconds = session.getTimeSeconds();
        remainingSeconds = totalSeconds;
        showingResults = false; // Reset dialog guard for new game
        roundState = RoundState.INIT; // Reset round state
        hasScored = false; // Reset scoring guard
        
        // Clear existing UI if any
        categoriesContainer.getChildren().clear();
        inputFields.clear();
        statusLabels.clear();
        confidenceLabels.clear();
        categoryCards.clear();
        cachedResults.clear();
        usedWordsThisRound.clear();
        
        // Setup UI with new configuration
        setupUI();
        
        // Don't start the game yet - wait for scene to be fully shown
        // The game will be started by calling startGameAfterSceneShown()
    }
    
    public void startGameAfterSceneShown() {
        roundState = RoundState.RUNNING; // Mark round as active
        showingResults = false; // Ensure dialog guard is reset
        startCountdown();
        animateLetterReveal();
    }
    
    private void startGame() {
        roundState = RoundState.RUNNING; // Mark round as active
        startCountdown();
        animateLetterReveal();
    }

    private void setupUI() {
        // Update header info
        letterLabel.setText(session.getCurrentLetter());
        timerLabel.setText(formatTime(remainingSeconds));
        scoreLabel.setText(String.valueOf(session.getCurrentScore()));
        roundLabel.setText("%d/%d".formatted(session.getCurrentRound(), session.getTotalRounds()));
        timerProgress.setProgress(1.0);

        // Clear previous categories
        categoriesContainer.getChildren().clear();
        inputFields.clear();
        statusLabels.clear();
        confidenceLabels.clear();
        categoryCards.clear();
        cachedResults.clear();
        usedWordsThisRound.clear();

        // Build category cards
        for (Category c : session.getCategories()) {
            HBox card = createCategoryCard(c);
            categoryCards.put(c, card);
            categoriesContainer.getChildren().add(card);
        }

        // Reset buttons
        stopButton.setDisable(false);
        hintButton.setDisable(hintsUsed >= MAX_HINTS);
        hintButton.setText("💡 Indice (%d/%d)".formatted(MAX_HINTS - hintsUsed, MAX_HINTS));
        skipButton.setDisable(false);
    }

    private HBox createCategoryCard(Category category) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("category-card");
        card.setPadding(new Insets(12, 20, 12, 20));

        // Icon
        Label iconLabel = new Label(category.getIcon());
        iconLabel.setStyle("-fx-font-size: 32px;");
        iconLabel.setMinWidth(50);

        // Category info
        VBox infoBox = new VBox(2);
        Label nameLabel = new Label(category.displayName());
        nameLabel.getStyleClass().add("category-name");
        Label hintLabel = new Label(category.getHint());
        hintLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        infoBox.getChildren().addAll(nameLabel, hintLabel);
        infoBox.setMinWidth(150);

        // Input field
        TextField tf = new TextField();
        tf.setPromptText("Mot en " + session.getCurrentLetter() + "...");
        tf.getStyleClass().add("category-input");
        tf.setPrefWidth(200); // Reduced to make room for confidence display
        HBox.setHgrow(tf, Priority.ALWAYS);

        // Show pending status while typing (no validation until submit/timeout)
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            Label status = statusLabels.get(category);
            Label confidenceLabel = confidenceLabels.get(category);
            
            if (newVal == null || newVal.trim().isEmpty()) {
                // Empty input - show pending state
                status.setText("⏳");
                status.getStyleClass().removeAll("status-valid", "status-invalid", "status-uncertain");
                status.getStyleClass().add("status-pending");
                confidenceLabel.setText("");
                tf.setStyle("");
            } else {
                // Non-empty input - show pending state (no validation yet)
                status.setText("⏳");
                status.getStyleClass().removeAll("status-valid", "status-invalid", "status-uncertain");
                status.getStyleClass().add("status-pending");
                confidenceLabel.setText("En attente...");
                
                // Simple visual feedback for first letter (no validation)
                String requiredStart = session.getCurrentLetter().toLowerCase();
                String input = newVal.trim().toLowerCase();
                boolean startsCorrect = input.startsWith(requiredStart);
                tf.setStyle(startsCorrect ? "" : "-fx-border-color: #ffa726; -fx-border-width: 2;");
            }
        });

        inputFields.put(category, tf);

        // Status label
        Label status = new Label("⏳");
        status.getStyleClass().add("status-pending");
        status.setMinWidth(30);
        status.setAlignment(Pos.CENTER);
        statusLabels.put(category, status);
        
        // Confidence and source display
        Label confidenceLabel = new Label("");
        confidenceLabel.getStyleClass().add("confidence-label");
        confidenceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        confidenceLabel.setMinWidth(80);
        confidenceLabel.setAlignment(Pos.CENTER);
        confidenceLabels.put(category, confidenceLabel);
        
        // Vertical box for status and confidence
        VBox statusBox = new VBox(2);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.getChildren().addAll(status, confidenceLabel);

        card.getChildren().addAll(iconLabel, infoBox, tf, statusBox);
        return card;
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
        if (countdown != null) countdown.stop(); // Prevent timer stacking
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            timerLabel.setText(formatTime(remainingSeconds));
            timerProgress.setProgress((double) remainingSeconds / totalSeconds);

            // Warning effect when time is low
            if (remainingSeconds <= 10) {
                timerLabel.getStyleClass().add("timer-warning");
                if (remainingSeconds <= 5) {
                    pulseTimer();
                }
            }

            if (remainingSeconds <= 0) {
                countdown.stop();
                endRound(); // Use proper round end method
            }
        }));
        countdown.setCycleCount(remainingSeconds);
        countdown.playFromStart();
    }

    private void pulseTimer() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(200), timerLabel);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.2);
        pulse.setToY(1.2);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }

    @FXML
    private void handleStopAndValidate() {
        if (countdown != null) countdown.stop();
        endRound(); // Use proper round end method
    }

    @FXML
    private void handleBackToMenu() {
        if (countdown != null) countdown.stop();

        boolean confirmed = DialogHelper.showConfirmation(
            "Quitter la partie",
            "Voulez-vous vraiment quitter?",
            "Votre progression sera perdue."
        );

        if (confirmed) {
            session.endGame();
            navigateToMenu();
        } else {
            // Resume countdown only if round is still running and game is not finished
            if (remainingSeconds > 0 && roundState == RoundState.RUNNING) {
                startCountdown();
            }
        }
    }

    @FXML
    private void handleHint() {
        if (hintsUsed >= MAX_HINTS) return;

        // Find first empty or invalid field
        for (Category c : session.getCategories()) {
            TextField tf = inputFields.get(c);
            String text = tf.getText();
            if (text == null || text.isEmpty()) {
                tf.setPromptText("Pensez à: " + c.getHint() + " commençant par " + session.getCurrentLetter());
                hintsUsed++;
                hintButton.setText("💡 Indice (%d/%d)".formatted(MAX_HINTS - hintsUsed, MAX_HINTS));
                if (hintsUsed >= MAX_HINTS) {
                    hintButton.setDisable(true);
                }
                break;
            }
        }
    }

    @FXML
    private void handleSkipRound() {
        if (countdown != null) countdown.stop();

        boolean confirmed = DialogHelper.showConfirmation(
            "Passer la manche",
            "Voulez-vous passer cette manche?",
            "Vous ne gagnerez aucun point pour cette manche."
        );

        if (confirmed) {
            proceedToNextRound();
        } else {
            if (remainingSeconds > 0 && roundState == RoundState.RUNNING) {
                startCountdown();
            }
        }
    }

    /**
     * Batch validation when time runs out or user clicks "Validate".
     * Validates ALL entered words in one batch for better performance.
     * This replaces the old real-time validation approach.
     */
    /**
     * Properly end the round with state management and single scoring.
     */
    private void endRound() {
        // Guard against multiple round endings
        if (roundState != RoundState.RUNNING) {
            return; // Round already ended
        }
        
        // Transition to FINISHED state immediately
        roundState = RoundState.FINISHED;
        
        // Validate and score ONCE
        if (!hasScored) {
            int points = validateAndScore();
            // hasScored is now set inside validateAndScore()
            
            // Show results after JavaFX finishes current processing
            Platform.runLater(() -> showRoundResults(points));
        }
    }
    
    /**
     * Validate all words and calculate score ONCE per round.
     */
    private int validateAndScore() {
        // Only score if round is finished and not yet scored
        if (roundState != RoundState.FINISHED || hasScored) {
            return 0;
        }
        
        // Immediately mark as scored to prevent race conditions
        hasScored = true;
        
        int points = 0;
        
        // Clear any cached results from previous rounds
        cachedResults.clear();
        usedWordsThisRound.clear();

        // Batch validate all entered words
        for (Category c : session.getCategories()) {
            TextField tf = inputFields.get(c);
            String word = tf.getText() != null ? tf.getText().trim() : "";
            Label status = statusLabels.get(c);
            Label confidenceLabel = confidenceLabels.get(c);
            HBox card = categoryCards.get(c);

            // Perform validation now (not during typing)
            ValidationResult result = validateWordComplete(word, c);
            
            // Cache result and apply visual feedback
            cachedResults.put(c, result);
            applyValidationResult(result, status, confidenceLabel, card);
            
            // Award points based on validation results
            if (result.isValid()) {
                points += calculatePoints(result);
                animateSuccess(card);
            } else if (result.isUncertain()) {
                // UNCERTAIN should not reach UI - resolve through AI
                points += 1; // Partial credit for uncertain results
            }

            tf.setDisable(true);
        }

        // Add points to session ONCE
        session.addPoints(points);
        scoreLabel.setText(String.valueOf(session.getCurrentScore()));
        
        // Disable all controls
        stopButton.setDisable(true);
        hintButton.setDisable(true);
        skipButton.setDisable(true);
        
        return points;
    }
    
    
    /**
     * Complete word validation using the sophisticated backend pipeline.
     * 
     * This method represents the CORE IMPROVEMENT over the old system:
     * OLD: Only checked if word started with correct letter → orange highlight
     * NEW: Full category-aware validation via ValidationService → accurate green/orange/red feedback
     * 
     * VALIDATION STAGES:
     * 1. Basic validation (empty, null)
     * 2. First letter check (preliminary filter, not final validation)
     * 3. Duplicate detection within current round
     * 4. **BACKEND VALIDATION** via ValidationService.validateWord():
     *    - Database cache lookup for performance
     *    - FixedListValidator: Fast lookup in French word lists
     *    - WebConfigurableValidator: Web API validation via DictionaryAPI.dev
     *    - SemanticAiValidator: Future AI validation (placeholder)
     * 
     * The ValidationService ensures consistent validation logic across the application
     * and provides caching for performance. It determines if a word actually belongs 
     * to the requested category, not just whether it exists or starts correctly.
     * 
     * EXAMPLES:
     * - "chien" in ANIMAL → VALID (found in fixed list)
     * - "dog" in ANIMAL → VALID (DictionaryAPI confirms animal category)
     * - "dog" in FRUIT → INVALID (DictionaryAPI knows it's not a fruit)
     * - "zzxqp" in ANIMAL → INVALID (not found anywhere)
     */
    private ValidationResult validateWordComplete(String word, Category category) {
        // Step 1: Basic input validation
        if (word == null || word.trim().isEmpty()) {
            return new ValidationResult(ValidationStatus.INVALID, 0.0, "UI", "Empty word");
        }
        
        String normalizedWord = word.trim().toLowerCase();
        String requiredStart = session.getCurrentLetter().toLowerCase();
        
        // Step 2: First letter check (preliminary filter, not final validation)
        if (!normalizedWord.startsWith(requiredStart)) {
            return new ValidationResult(ValidationStatus.INVALID, 0.0, "UI", 
                "Word must start with '" + session.getCurrentLetter() + "'");
        }
        
        // Step 3: Duplicate check within current round
        if (usedWordsThisRound.contains(normalizedWord)) {
            return new ValidationResult(ValidationStatus.INVALID, 0.0, "UI", "Duplicate word in this round");
        }
        
        // Step 4: BACKEND VALIDATION via ValidationService - The key improvement!
        // ValidationService coordinates the full validation pipeline with caching
        // This replaces the old "orange if starts with letter" logic with
        // sophisticated category-aware validation
        ValidationResult backendResult = validationService.validateWord(category.name(), word);
        
        // Step 5: Track valid words to prevent duplicates in future inputs
        if (backendResult.isValid()) {
            usedWordsThisRound.add(normalizedWord);
        }
        
        return backendResult;
    }
    
    /**
     * Applies validation result to UI elements with enhanced feedback.
     * 
     * This method replaces the old orange-for-correct-letter approach with
     * sophisticated backend-driven visual feedback:
     * 
     * - GREEN: Valid words with high confidence (>=85%)
     * - ORANGE: Valid words with low confidence (<85%) OR uncertain status
     * - RED: Invalid words or validation errors
     * 
     * The confidence percentage and validation source are displayed to help
     * users understand why their word was accepted/rejected.
     */
    private void applyValidationResult(ValidationResult result, Label status, Label confidenceLabel, HBox card) {
        // Determine if this is a low-confidence valid result
        boolean isLowConfidenceValid = result.isValid() && result.getConfidence() < 0.85;
        
        // Update status icon with more nuanced feedback
        String statusIcon = switch (result.getStatus()) {
            case VALID -> isLowConfidenceValid ? "⚠️" : "✅";  // Warning for low confidence
            case INVALID -> "❌"; 
            case UNCERTAIN -> "❓";
            case ERROR -> "⚠️";
        };
        status.setText(statusIcon);
        
        // Update CSS classes with low-confidence handling
        status.getStyleClass().removeAll("status-pending", "status-valid", "status-invalid", "status-uncertain");
        String cssClass;
        if (result.isValid() && !isLowConfidenceValid) {
            cssClass = "status-valid";
        } else if (result.isValid() && isLowConfidenceValid || result.isUncertain()) {
            cssClass = "status-uncertain";  // Orange for low confidence valid or uncertain
        } else {
            cssClass = "status-invalid";
        }
        status.getStyleClass().add(cssClass);
        
        // Enhanced confidence and source display with helpful messages
        if (result.getConfidence() > 0.0) {
            String confidenceText = String.format("%.0f%% (%s)", result.getConfidence() * 100, result.getSource());
            if (isLowConfidenceValid) {
                confidenceText += " - Incertain";
            }
            confidenceLabel.setText(confidenceText);
        } else {
            confidenceLabel.setText(result.getSource().equals("UI") ? result.getDetails() : result.getSource());
        }
        
        // Enhanced card border styling with low-confidence orange
        String borderColor;
        if (result.isValid() && !isLowConfidenceValid) {
            borderColor = "#4ecca3";  // Green for high-confidence valid
        } else if (result.isValid() && isLowConfidenceValid || result.isUncertain()) {
            borderColor = "#ffa726";  // Orange for low-confidence valid or uncertain
        } else {
            borderColor = "#ff6b6b";  // Red for invalid/error
        }
        card.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 2; -fx-border-radius: 15;");
    }
    
    /**
     * Calculates points based on validation result.
     * Simple scoring: +1 for valid words, 0 for invalid.
     */
    private int calculatePoints(ValidationResult result) {
        return result.isValid() ? 1 : 0;
    }

    private void animateSuccess(HBox card) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(1.03);
        scale.setToY(1.03);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    private void showRoundResults(int pointsGained) {
        // Guard against multiple dialog shows
        if (showingResults) {
            return; // Dialog already showing
        }
        showingResults = true;
        
        // Transition to dialog shown state
        roundState = RoundState.DIALOG_SHOWN;
        
        String title = pointsGained > 0 ? "🎉 Bravo!" : "😅 Dommage!";
        
        // Simple results message
        String message = "Points gagnés: +" + pointsGained + "\n" +
                        "Score total: " + session.getCurrentScore() + "\n\n" +
                        "Manche " + session.getCurrentRound() + "/" + session.getTotalRounds();

        if (session.getCurrentRound() < session.getTotalRounds()) {
            ButtonType nextRound = new ButtonType("Manche suivante →", ButtonBar.ButtonData.OK_DONE);
            ButtonType quit = new ButtonType("Quitter", ButtonBar.ButtonData.CANCEL_CLOSE);

            Optional<ButtonType> result = DialogHelper.showConfirmation(
                "Résultats de la manche",
                title,
                message,
                nextRound, quit
            );
            
            if (result.isPresent() && result.get() == nextRound) {
                showingResults = false; // Reset dialog guard
                proceedToNextRound();
            } else {
                session.endGame();
                showingResults = false; // Reset dialog guard
                showFinalResults();
            }
        } else {
            session.endGame();
            ButtonType finish = new ButtonType("Voir les résultats", ButtonBar.ButtonData.OK_DONE);
            DialogHelper.showConfirmation("Résultats de la manche", title, message, finish);
            showingResults = false; // Reset dialog guard
            showFinalResults();
        }
    }

    private void proceedToNextRound() {
        roundState = RoundState.TRANSITIONING;
        hasScored = false; // Reset scoring guard for next round
        showingResults = false; // Reset dialog guard for next round
        
        if (session.nextRound()) {
            remainingSeconds = session.getTimeSeconds();
            timerLabel.getStyleClass().remove("timer-warning");
            setupUI();
            roundState = RoundState.RUNNING; // Start next round
            startCountdown();
            animateLetterReveal();
        } else {
            showFinalResults();
        }
    }

    private void showFinalResults() {
        String title = "🏆 Partie terminée!";
        String header = "Score final: " + session.getCurrentScore() + " points";

        String rating;
        int maxPossible = session.getTotalRounds() * session.getCategories().size() * 2;
        double percentage = (double) session.getCurrentScore() / maxPossible * 100;

        if (percentage >= 80) rating = "🌟 Excellent!";
        else if (percentage >= 60) rating = "👏 Très bien!";
        else if (percentage >= 40) rating = "👍 Pas mal!";
        else rating = "💪 Continuez à vous entraîner!";

        String content = rating + "\n\n" +
                "Meilleur score: " + GameSession.getHighScore() + "\n" +
                "Parties jouées: " + GameSession.getGamesPlayed();

        ButtonType playAgain = new ButtonType("Rejouer", ButtonBar.ButtonData.OK_DONE);
        ButtonType menu = new ButtonType("Menu principal", ButtonBar.ButtonData.CANCEL_CLOSE);

        Optional<ButtonType> result = DialogHelper.showConfirmation(
            title,
            header,
            content,
            playAgain, menu
        );
        if (result.isPresent() && result.get() == playAgain) {
            restartGame();
        } else {
            navigateToMenu();
        }
    }

    private void restartGame() {
        session = new GameSession();
        totalSeconds = session.getTimeSeconds();
        remainingSeconds = totalSeconds;
        hintsUsed = 0;
        timerLabel.getStyleClass().remove("timer-warning");
        setupUI();
        startCountdown();
        animateLetterReveal();
    }

    private void navigateToMenu() {
        try {
            Stage stage = (Stage) letterLabel.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/baccalaureat/MainMenu.fxml"));
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
}
