package com.baccalaureat.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.GameSession;
import com.baccalaureat.model.Player;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MultiplayerGameController {
    private boolean darkMode = false;
    @FXML private Label letterLabel;
    @FXML private Label timerLabel;
    @FXML private Label roundLabel;
    @FXML private Label currentPlayerLabel;
    @FXML private VBox categoriesContainer;
    @FXML private HBox scoresBar;
    @FXML private Button validateButton;
    @FXML private ProgressBar timerProgress;

    private final ValidationService validationService = new ValidationService();
    private List<Player> players;
    private int currentPlayerIndex = 0;
    private GameSession session;
    private final Map<Category, TextField> inputFields = new HashMap<>();
    private final Map<Category, Label> statusLabels = new HashMap<>();
    private final Map<Player, VBox> playerScoreCards = new HashMap<>();

    private Timeline countdown;
    private int remainingSeconds;
    private int totalSeconds;
    private int currentRound = 1;
    private int totalRounds;
    private List<Category> categories;
    private String currentLetter;
    private final List<String> usedLetters = new ArrayList<>();

    @FXML
    private void initialize() {
        players = MultiplayerLobbyController.getGamePlayers();
        if (players == null || players.isEmpty()) {
            navigateToMenu();
            return;
        }

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

        // Reset all players for new game
        for (Player p : players) {
            p.resetForNewGame();
        }

        session = new GameSession();
        totalSeconds = session.getTimeSeconds();
        totalRounds = session.getTotalRounds();
        categories = session.getCategories();

        generateNewLetter();
        setupUI();
        setupScoresBar();
        startPlayerTurn();
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
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
            if (newVal != null && !newVal.isEmpty()) {
                boolean startsCorrect = newVal.toUpperCase().startsWith(currentLetter);
                tf.setStyle(startsCorrect ? 
                    "-fx-border-color: #4ecca3; -fx-border-width: 2; -fx-border-radius: 10;" :
                    "-fx-border-color: #ff6b6b; -fx-border-width: 2; -fx-border-radius: 10;");
            } else {
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
                handleValidate();
            }
        }));
        countdown.setCycleCount(remainingSeconds);
        countdown.playFromStart();
    }

    @FXML
    private void handleValidate() {
        if (countdown != null) countdown.stop();

        Player current = players.get(currentPlayerIndex);
        int points = 0;

        // Collect and validate answers
        for (Category c : categories) {
            TextField tf = inputFields.get(c);
            String word = tf.getText() != null ? tf.getText().trim() : "";
            Label status = statusLabels.get(c);

            current.setAnswer(c, word);

            boolean startsCorrect = !word.isEmpty() && word.substring(0, 1).equalsIgnoreCase(currentLetter);
            boolean valid = startsCorrect && validationService.validateWord(c.name(), word);

            if (valid) {
                status.setText("✅");
                status.setStyle("-fx-text-fill: #4ecca3; -fx-font-size: 24px;");
                points += 2;
            } else {
                status.setText("❌");
                status.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 24px;");
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

    private void showTurnResult(Player player, int points) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Tour terminé");
        alert.setHeaderText(player.getName() + (points > 0 ? " - Bravo! 🎉" : " - Dommage! 😅"));
        alert.setContentText("Points gagnés: +" + points + "\nScore total: " + player.getScore());

        ButtonType nextBtn = new ButtonType("Continuer →");
        alert.getButtonTypes().setAll(nextBtn);
        alert.showAndWait();

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

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fin de manche");
        alert.setHeaderText("Manche " + (currentRound - 1) + " terminée!");
        alert.setContentText(sb.toString() + "\nProchaine lettre: " + currentLetter);

        ButtonType nextRound = new ButtonType("Manche " + currentRound + " →");
        alert.getButtonTypes().setAll(nextRound);
        alert.showAndWait();

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

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🏆 Partie terminée!");
        alert.setHeaderText("👑 " + winner.getName() + " gagne avec " + winner.getScore() + " points!");
        alert.setContentText("Classement final:\n\n" + sb.toString());

        ButtonType playAgain = new ButtonType("Rejouer");
        ButtonType menu = new ButtonType("Menu principal");
        alert.getButtonTypes().setAll(playAgain, menu);

        Optional<ButtonType> result = alert.showAndWait();
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
