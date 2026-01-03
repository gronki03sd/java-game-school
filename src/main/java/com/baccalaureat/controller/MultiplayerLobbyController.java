package com.baccalaureat.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.baccalaureat.model.GameConfig;
import com.baccalaureat.model.Player;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for Multiplayer Lobby.
 * NOTE: This controller is now deprecated as multiplayer setup
 * is handled by the new GameConfigurationController.
 * This is kept for backward compatibility and will redirect users
 * to the new configuration system.
 */
public class MultiplayerLobbyController {
    private boolean darkMode = false;
    @FXML private TextField playerNameInput;
    @FXML private FlowPane playersPane;
    @FXML private Button startGameButton;
    @FXML private Label noticeLabel;

    private final List<Player> players = new ArrayList<>();
    private static List<Player> gamePlayers;

    @FXML
    private void initialize() {
        // Show deprecation notice
        if (noticeLabel != null) {
            noticeLabel.setText("Cette interface sera bientôt remplacée par la nouvelle configuration de jeu.");
        }
        
        // Enter key to add player
        if (playerNameInput != null) {
            playerNameInput.setOnAction(e -> handleAddPlayer());
        }
        
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
            if (noticeLabel != null) {
                noticeLabel.setText("Maximum 8 joueurs!");
                noticeLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
            }
            return;
        }

        // Check for duplicate names
        if (players.stream().anyMatch(p -> p.getName().equalsIgnoreCase(name))) {
            if (noticeLabel != null) {
                noticeLabel.setText("Ce nom existe déjà!");
                noticeLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
            }
            shakeInput();
            return;
        }

        Player player = new Player(name);
        players.add(player);
        playerNameInput.clear();
        updateUI();
    }

    private void shakeInput() {
        if (playerNameInput != null) {
            playerNameInput.setStyle("-fx-border-color: #ff6b6b; -fx-border-width: 2;");
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    javafx.application.Platform.runLater(() -> 
                        playerNameInput.setStyle(""));
                } catch (InterruptedException ignored) {}
            }).start();
        }
    }

    private void removePlayer(Player player) {
        players.remove(player);
        updateUI();
    }

    private void updateUI() {
        if (playersPane != null) {
            playersPane.getChildren().clear();

            // Create player cards
            String[] colors = {"#e94560", "#4ecca3", "#ffd93d", "#6c5ce7", "#00cec9", "#fd79a8", "#a29bfe", "#ff7675"};
            int colorIndex = 0;

            for (Player player : players) {
                VBox card = createPlayerCard(player, colors[colorIndex % colors.length]);
                playersPane.getChildren().add(card);
                colorIndex++;
            }
        }

        updateStartButton();
    }

    private void updateStartButton() {
        // Update start button and notice
        boolean canStart = players.size() >= 2;
        if (startGameButton != null) {
            startGameButton.setDisable(!canStart);
        }

        if (noticeLabel != null) {
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
    private void handleStartGame() {
        if (players.size() < 2) return;

        // Redirect to new game configuration system instead of direct game launch
        try {
            Stage stage = (Stage) startGameButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/GameConfigurationView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 750);
            
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
            
            // Configure the GameConfigurationController with local mode and players
            Object controller = loader.getController();
            if (controller instanceof GameConfigurationController gcc) {
                gcc.setDarkMode(darkMode);
                gcc.setGameMode(GameConfig.GameMode.LOCAL);
                
                // Pre-populate with existing players
                List<String> playerNames = new ArrayList<>();
                for (Player player : players) {
                    playerNames.add(player.getName());
                }
                // Note: The GameConfigurationController would need a method to accept pre-configured players
                // This is a design improvement for the future
            }
            
            stage.setScene(scene);
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            
            // Fallback: Show message about using new configuration
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Nouvelle Interface");
            alert.setHeaderText("Configuration Améliorée");
            alert.setContentText("Veuillez utiliser le nouveau système de configuration de partie depuis le menu principal (Mode Multijoueur).");
            alert.showAndWait();
        }
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
    }

    @FXML
    private void handleBackToMenu() {
        try {
            Stage stage = (Stage) (playersPane != null ? playersPane.getScene() : startGameButton.getScene()).getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MainMenu.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 700);
            
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
            
            // Configure the MainMenuController
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

    public static List<Player> getGamePlayers() {
        return gamePlayers;
    }
}
