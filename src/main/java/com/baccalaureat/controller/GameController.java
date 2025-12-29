package com.baccalaureat.controller;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.GameSession;
import com.baccalaureat.service.ValidationService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class GameController {
    @FXML
    private Label letterLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private GridPane inputGrid;
    @FXML
    private Button stopButton;

    private final ValidationService validationService = new ValidationService();
    private final GameSession session = new GameSession();
    private final Map<Category, TextField> inputFields = new HashMap<>();
    private final Map<Category, Label> statusLabels = new HashMap<>();

    private Timeline countdown;
    private int remainingSeconds = 60;

    @FXML
    private void initialize() {
        // Top labels
        letterLabel.setText(session.getCurrentLetter());
        timerLabel.setText(formatTime(remainingSeconds));

        // Build grid: Category | TextField | Status
        inputGrid.setHgap(10);
        inputGrid.setVgap(8);
        inputGrid.setPadding(new Insets(10));

        int row = 0;
        for (Category c : session.getCategories()) {
            Label catLabel = new Label(c.displayName());
            TextField tf = new TextField();
            tf.setPromptText("Mot commençant par " + session.getCurrentLetter());
            Label status = new Label("⏳");

            inputFields.put(c, tf);
            statusLabels.put(c, status);

            inputGrid.add(catLabel, 0, row);
            inputGrid.add(tf, 1, row);
            inputGrid.add(status, 2, row);
            row++;
        }

        startCountdown();
    }

    private void startCountdown() {
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            timerLabel.setText(formatTime(remainingSeconds));
            if (remainingSeconds <= 0) {
                countdown.stop();
                validateAll();
            }
        }));
        countdown.setCycleCount(remainingSeconds);
        countdown.playFromStart();
    }

    @FXML
    private void handleStopAndValidate() {
        if (countdown != null)
            countdown.stop();
        validateAll();
    }

    private void validateAll() {
        int points = 0;
        String requiredStart = session.getCurrentLetter();
        for (Category c : session.getCategories()) {
            TextField tf = inputFields.get(c);
            String word = tf.getText() != null ? tf.getText().trim() : "";
            Label status = statusLabels.get(c);

            boolean startsWith = !word.isEmpty() && word.substring(0, 1).equalsIgnoreCase(requiredStart);
            boolean valid = startsWith && validationService.validateWord(c.name(), word);

            if (valid) {
                status.setText("✅");
                points += 1; // simple scoring: 1 point per valid word
            } else {
                status.setText(word.isEmpty() ? "❌" : (startsWith ? "❌" : "❌"));
            }

            tf.setDisable(true);
        }
        session.addPoints(points);
        showScore(points, session.getCurrentScore());
        stopButton.setDisable(true);
    }

    private void showScore(int gained, int total) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Validation terminée");
        alert.setHeaderText("Résultats");
        alert.setContentText("Points gagnés: " + gained + "\nScore total: " + total);
        alert.showAndWait();
    }

    private String formatTime(int seconds) {
        if (seconds < 0)
            seconds = 0;
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
