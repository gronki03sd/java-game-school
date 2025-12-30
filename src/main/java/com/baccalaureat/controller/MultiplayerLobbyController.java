package com.baccalaureat.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.baccalaureat.model.GameSession;
import com.baccalaureat.model.Player;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MultiplayerLobbyController {
    private boolean darkMode = false;
    @FXML private TextField playerNameInput;
    @FXML private FlowPane playersPane;
    @FXML private Button startGameButton;
    @FXML private Label noticeLabel;
    @FXML private ToggleButton easyBtn;
    @FXML private ToggleButton mediumBtn;
    @FXML private ToggleButton hardBtn;

    private final List<Player> players = new ArrayList<>();
    private ToggleGroup difficultyGroup;
    private static List<Player> gamePlayers;

    @FXML
    private void initialize() {
        // Setup toggle group for difficulty
        difficultyGroup = new ToggleGroup();
        easyBtn.setToggleGroup(difficultyGroup);
        mediumBtn.setToggleGroup(difficultyGroup);
        hardBtn.setToggleGroup(difficultyGroup);

        // Set default selection
        switch (GameSession.getSelectedDifficulty()) {
            case EASY -> easyBtn.setSelected(true);
            case MEDIUM -> mediumBtn.setSelected(true);
            case HARD -> hardBtn.setSelected(true);
        }
        updateSelectedStyle();

        // Enter key to add player
        playerNameInput.setOnAction(e -> handleAddPlayer());
        
        updateUI();
    }

    @FXML
    private void handleAddPlayer() {
        String name = playerNameInput.getText().trim();
        if (name.isEmpty()) {
            shakeInput();
            return;
        }
        
        if (players.size() >= 8) {
            noticeLabel.setText("Maximum 8 joueurs!");
            noticeLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
            return;
        }

        // Check for duplicate names
        if (players.stream().anyMatch(p -> p.getName().equalsIgnoreCase(name))) {
            noticeLabel.setText("Ce nom existe déjà!");
            noticeLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
            shakeInput();
            return;
        }

        Player player = new Player(name);
        players.add(player);
        playerNameInput.clear();
        updateUI();
    }

    private void shakeInput() {
        playerNameInput.setStyle("-fx-border-color: #ff6b6b; -fx-border-width: 2;");
        new Thread(() -> {
            try {
                Thread.sleep(500);
                javafx.application.Platform.runLater(() -> 
                    playerNameInput.setStyle(""));
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void removePlayer(Player player) {
        players.remove(player);
        updateUI();
    }

    private void updateUI() {
        playersPane.getChildren().clear();

        // Create player cards
        String[] colors = {"#e94560", "#4ecca3", "#ffd93d", "#6c5ce7", "#00cec9", "#fd79a8", "#a29bfe", "#ff7675"};
        int colorIndex = 0;

        for (Player player : players) {
            VBox card = createPlayerCard(player, colors[colorIndex % colors.length]);
            playersPane.getChildren().add(card);
            colorIndex++;
        }

        // Update start button and notice
        boolean canStart = players.size() >= 2;
        startGameButton.setDisable(!canStart);

        if (players.isEmpty()) {
            noticeLabel.setText("Ajoutez au moins 2 joueurs pour commencer");
            noticeLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
        } else if (players.size() == 1) {
            noticeLabel.setText("Encore 1 joueur requis");
            noticeLabel.setStyle("-fx-text-fill: #ffd93d; -fx-font-size: 12px;");
        } else {
            noticeLabel.setText("✓ " + players.size() + " joueurs prêts!");
            noticeLabel.setStyle("-fx-text-fill: #4ecca3; -fx-font-size: 12px;");
        }
    }

    private VBox createPlayerCard(Player player, String accentColor) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setPrefWidth(140);
        card.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.08);" +
            "-fx-background-radius: 15;" +
            "-fx-border-color: " + accentColor + ";" +
            "-fx-border-radius: 15;" +
            "-fx-border-width: 2;"
        );

        // Player avatar/icon
        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 32px;");

        // Player name
        Label nameLabel = new Label(player.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(120);
        nameLabel.setAlignment(Pos.CENTER);

        // Remove button
        Button removeBtn = new Button("✕");
        removeBtn.setStyle(
            "-fx-background-color: rgba(255, 107, 107, 0.3);" +
            "-fx-text-fill: #ff6b6b;" +
            "-fx-font-size: 10px;" +
            "-fx-padding: 3 8;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        );
        removeBtn.setOnAction(e -> removePlayer(player));
        removeBtn.setOnMouseEntered(e -> removeBtn.setStyle(
            "-fx-background-color: #ff6b6b;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 10px;" +
            "-fx-padding: 3 8;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        ));
        removeBtn.setOnMouseExited(e -> removeBtn.setStyle(
            "-fx-background-color: rgba(255, 107, 107, 0.3);" +
            "-fx-text-fill: #ff6b6b;" +
            "-fx-font-size: 10px;" +
            "-fx-padding: 3 8;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        ));

        card.getChildren().addAll(avatar, nameLabel, removeBtn);
        return card;
    }

    @FXML
    private void handleDifficultyChange(ActionEvent event) {
        ToggleButton source = (ToggleButton) event.getSource();
        if (source.isSelected()) {
            String difficulty = (String) source.getUserData();
            GameSession.setDifficulty(GameSession.Difficulty.valueOf(difficulty));
            updateSelectedStyle();
        } else {
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
    private void handleStartGame() {
        if (players.size() < 2) return;

        gamePlayers = new ArrayList<>(players);
        try {
            Stage stage = (Stage) startGameButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MultiplayerGame.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 750);
            // Pass theme
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
            // Set darkMode in MultiplayerGameController
            Object controller = loader.getController();
            if (controller instanceof com.baccalaureat.controller.MultiplayerGameController mgc) {
                mgc.setDarkMode(darkMode);
            }
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
    }

    @FXML
    private void handleBackToMenu() {
        try {
            Stage stage = (Stage) playersPane.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/com/baccalaureat/MainMenu.fxml"));
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Player> getGamePlayers() {
        return gamePlayers;
    }
}
