package com.baccalaureat.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.GameSession;
import com.baccalaureat.service.ValidationService;

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

    private final ValidationService validationService = new ValidationService();
    private GameSession session;
    private final Map<Category, TextField> inputFields = new HashMap<>();
    private final Map<Category, Label> statusLabels = new HashMap<>();
    private final Map<Category, HBox> categoryCards = new HashMap<>();

    private Timeline countdown;
    private int remainingSeconds;
    private int totalSeconds;
    private int hintsUsed = 0;
    private static final int MAX_HINTS = 3;

    @FXML
    private void initialize() {
        session = new GameSession();
        totalSeconds = session.getTimeSeconds();
        remainingSeconds = totalSeconds;

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

        setupUI();
        startCountdown();
        animateLetterReveal();
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
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
        categoryCards.clear();

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
        tf.setPrefWidth(250);
        HBox.setHgrow(tf, Priority.ALWAYS);

        // Real-time validation indicator
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                boolean startsCorrect = newVal.toUpperCase().startsWith(session.getCurrentLetter());
                if (startsCorrect) {
                    tf.setStyle("-fx-border-color: #4ecca3; -fx-border-width: 2; -fx-border-radius: 10;");
                } else {
                    tf.setStyle("-fx-border-color: #ff6b6b; -fx-border-width: 2; -fx-border-radius: 10;");
                }
            } else {
                tf.setStyle("");
            }
        });

        inputFields.put(category, tf);

        // Status label
        Label status = new Label("⏳");
        status.getStyleClass().add("status-pending");
        status.setMinWidth(40);
        status.setAlignment(Pos.CENTER);
        statusLabels.put(category, status);

        card.getChildren().addAll(iconLabel, infoBox, tf, status);
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
                validateAll();
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
        validateAll();
    }

    @FXML
    private void handleBackToMenu() {
        if (countdown != null) countdown.stop();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Quitter la partie");
        confirm.setHeaderText("Voulez-vous vraiment quitter?");
        confirm.setContentText("Votre progression sera perdue.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            session.endGame();
            navigateToMenu();
        } else {
            // Resume countdown
            if (remainingSeconds > 0) {
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

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Passer la manche");
        confirm.setHeaderText("Voulez-vous passer cette manche?");
        confirm.setContentText("Vous ne gagnerez aucun point pour cette manche.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            proceedToNextRound();
        } else {
            if (remainingSeconds > 0) {
                startCountdown();
            }
        }
    }

    private void validateAll() {
        int points = 0;
        String requiredStart = session.getCurrentLetter();

        for (Category c : session.getCategories()) {
            TextField tf = inputFields.get(c);
            String word = tf.getText() != null ? tf.getText().trim() : "";
            Label status = statusLabels.get(c);
            HBox card = categoryCards.get(c);

            boolean startsCorrect = !word.isEmpty() && word.substring(0, 1).equalsIgnoreCase(requiredStart);
            boolean valid = startsCorrect && validationService.validateWord(c.name(), word);

            if (valid) {
                status.setText("✅");
                status.getStyleClass().removeAll("status-pending", "status-invalid");
                status.getStyleClass().add("status-valid");
                card.setStyle("-fx-border-color: #4ecca3; -fx-border-width: 2; -fx-border-radius: 15;");
                points += 2;

                // Success animation
                animateSuccess(card);
            } else {
                status.setText("❌");
                status.getStyleClass().removeAll("status-pending", "status-valid");
                status.getStyleClass().add("status-invalid");
                card.setStyle("-fx-border-color: #ff6b6b; -fx-border-width: 2; -fx-border-radius: 15;");
            }

            tf.setDisable(true);
        }

        session.addPoints(points);
        scoreLabel.setText(String.valueOf(session.getCurrentScore()));
        stopButton.setDisable(true);
        hintButton.setDisable(true);
        skipButton.setDisable(true);

        // Show results after a short delay
        final int finalPoints = points;
        Timeline delay = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> showRoundResults(finalPoints)));
        delay.play();
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
        String title = pointsGained > 0 ? "🎉 Bravo!" : "😅 Dommage!";
        String message = "Points gagnés: +" + pointsGained + "\n" +
                        "Score total: " + session.getCurrentScore() + "\n\n" +
                        "Manche " + session.getCurrentRound() + "/" + session.getTotalRounds();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Résultats de la manche");
        alert.setHeaderText(title);
        alert.setContentText(message);

        if (session.getCurrentRound() < session.getTotalRounds()) {
            ButtonType nextRound = new ButtonType("Manche suivante →");
            ButtonType quit = new ButtonType("Quitter", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(nextRound, quit);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == nextRound) {
                proceedToNextRound();
            } else {
                session.endGame();
                showFinalResults();
            }
        } else {
            session.endGame();
            alert.getButtonTypes().setAll(new ButtonType("Voir les résultats"));
            alert.showAndWait();
            showFinalResults();
        }
    }

    private void proceedToNextRound() {
        if (session.nextRound()) {
            remainingSeconds = session.getTimeSeconds();
            timerLabel.getStyleClass().remove("timer-warning");
            setupUI();
            startCountdown();
            animateLetterReveal();
        } else {
            showFinalResults();
        }
    }

    private void showFinalResults() {
        Alert finalAlert = new Alert(Alert.AlertType.INFORMATION);
        finalAlert.setTitle("🏆 Partie terminée!");
        finalAlert.setHeaderText("Score final: " + session.getCurrentScore() + " points");

        String rating;
        int maxPossible = session.getTotalRounds() * session.getCategories().size() * 2;
        double percentage = (double) session.getCurrentScore() / maxPossible * 100;

        if (percentage >= 80) rating = "🌟 Excellent!";
        else if (percentage >= 60) rating = "👏 Très bien!";
        else if (percentage >= 40) rating = "👍 Pas mal!";
        else rating = "💪 Continuez à vous entraîner!";

        finalAlert.setContentText(rating + "\n\n" +
                "Meilleur score: " + GameSession.getHighScore() + "\n" +
                "Parties jouées: " + GameSession.getGamesPlayed());

        ButtonType playAgain = new ButtonType("Rejouer");
        ButtonType menu = new ButtonType("Menu principal");
        finalAlert.getButtonTypes().setAll(playAgain, menu);

        Optional<ButtonType> result = finalAlert.showAndWait();
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
            stage.setScene(new Scene(root, 900, 700));
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
