package com.baccalaureat.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for Remote Multiplayer Lobby - STEP 3 REST API Integration.
 * Connects to REST API server for multiplayer session management.
 */
public class MultiplayerLobbyController {
    private static final String SERVER_URL = "http://localhost:8080/api/sessions";
    
    private boolean darkMode = false;
    private String sessionId = null;
    private boolean isHost = false;
    private String playerName = "";
    
    // HTTP client for REST API calls
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObservableList<String> connectedPlayers = FXCollections.observableArrayList();
    
    @FXML private TextField playerNameInput;
    @FXML private TextField sessionIdInput;
    @FXML private ListView<String> playersListView;
    @FXML private Button startGameButton;
    @FXML private Button createGameButton;
    @FXML private Button joinGameButton;
    @FXML private Label noticeLabel;
    @FXML private Label sessionIdLabel;
    @FXML private Label connectionStatusLabel;
    
    @FXML
    private void initialize() {
        // Initialize ListView with connected players
        playersListView.setItems(connectedPlayers);
        
        // Set initial UI state
        connectionStatusLabel.setText("♾️ Prêt pour REST API");
        startGameButton.setDisable(true);
        noticeLabel.setText("Entrez votre nom et créez ou rejoignez une partie");
    }
    
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }
    
    @FXML
    private void handleCreateGame() {
        String playerNameText = playerNameInput.getText().trim();
        
        if (playerNameText.isEmpty()) {
            showError("Veuillez entrer votre nom");
            return;
        }
        
        this.playerName = playerNameText;
        this.isHost = true;
        
        System.out.println("[LOBBY] Host creating session via REST API...");
        connectionStatusLabel.setText("🔄 Création de la session...");
        
        // Call REST API to create session
        createSessionViaAPI();
    }
    
    @FXML
    private void handleJoinGame() {
        String playerNameText = playerNameInput.getText().trim();
        String sessionCode = sessionIdInput.getText().trim();
        
        if (playerNameText.isEmpty()) {
            showError("Veuillez entrer votre nom");
            return;
        }
        
        if (sessionCode.isEmpty()) {
            showError("Veuillez entrer un code de session");
            return;
        }
        
        this.playerName = playerNameText;
        this.sessionId = sessionCode;
        this.isHost = false;
        
        System.out.println("[LOBBY] Player attempting to join session: " + sessionCode + " as " + playerNameText);
        connectionStatusLabel.setText("🔄 Rejoindre la session...");
        
        // Call REST API to join session
        joinSessionViaAPI();
    }
    
    // REST API Integration
    
    private void createSessionViaAPI() {
        try {
            // Create JSON request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("hostUsername", playerName);
            requestBody.put("roundDuration", 120); // 2 minutes default
            
            // Add default categories
            var categoriesArray = requestBody.putArray("categories");
            categoriesArray.add("Animal");
            categoriesArray.add("Pays");
            categoriesArray.add("Prénom");
            
            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
            
            // Send request asynchronously
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::handleCreateSessionResponse)
                .exceptionally(this::handleAPIError);
                
        } catch (Exception e) {
            Platform.runLater(() -> {
                showError("Erreur lors de la création: " + e.getMessage());
                connectionStatusLabel.setText("❌ Erreur de création");
            });
        }
    }
    
    private void joinSessionViaAPI() {
        try {
            // Create JSON request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("sessionId", sessionId);
            requestBody.put("playerUsername", playerName);  // Server expects playerUsername
            
            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/join"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
            
            // Send request asynchronously
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::handleJoinSessionResponse)
                .exceptionally(this::handleAPIError);
                
        } catch (Exception e) {
            Platform.runLater(() -> {
                showError("Erreur lors de la connexion: " + e.getMessage());
                connectionStatusLabel.setText("❌ Erreur de connexion");
            });
        }
    }
    
    private void handleCreateSessionResponse(String responseBody) {
        Platform.runLater(() -> {
            try {
                System.out.println("[API] Create session response: " + responseBody);
                JsonNode response = objectMapper.readTree(responseBody);
                
                // Server returns CreateSessionResponse with sessionId field
                JsonNode sessionIdNode = response.get("sessionId");
                if (sessionIdNode == null || sessionIdNode.isNull()) {
                    showError("Réponse serveur invalide: sessionId manquant");
                    return;
                }
                
                String newSessionId = sessionIdNode.asText();
                if (newSessionId == null || newSessionId.trim().isEmpty()) {
                    showError("Session ID vide reçu du serveur");
                    return;
                }
                
                this.sessionId = newSessionId;
                
                System.out.println("[LOBBY] Host created session: " + newSessionId);
                
                // Update UI
                connectionStatusLabel.setText("✅ Hôte - Session: " + newSessionId);
                sessionIdInput.setText(newSessionId);
                sessionIdInput.setEditable(false);
                
                // Add host to players list
                connectedPlayers.clear();
                connectedPlayers.add(playerName + " (Hôte)");
                
                // Enable start game button for host
                startGameButton.setDisable(false);
                
                // Update notice
                noticeLabel.setText("Partie créée! Partagez le code: " + newSessionId);
                
            } catch (Exception e) {
                showError("Erreur lors du traitement de la réponse: " + e.getMessage());
            }
        });
    }
    
    private void handleJoinSessionResponse(String responseBody) {
        Platform.runLater(() -> {
            try {
                JsonNode response = objectMapper.readTree(responseBody);
                
                // Server returns GameStateDTO, check if it contains session info
                JsonNode statusNode = response.get("status");
                String status = statusNode != null ? statusNode.asText() : "unknown";
                
                System.out.println("[LOBBY] Join response status: " + status);
                
                // Update UI for successful join
                connectionStatusLabel.setText("✅ Connecté - Session: " + sessionId);
                sessionIdInput.setEditable(false);
                
                // Add players to list (simplified for now)
                connectedPlayers.clear();
                connectedPlayers.add("Host (Hôte)");
                connectedPlayers.add(playerName);
                
                // Update notice
                noticeLabel.setText("Rejoint la partie! En attente du démarrage par l'hôte.");
                
            } catch (Exception e) {
                showError("Erreur lors du traitement de la réponse: " + e.getMessage());
            }
        });
    }
    
    private Void handleAPIError(Throwable throwable) {
        Platform.runLater(() -> {
            String errorMessage = throwable.getMessage();
            System.out.println("[API] Error: " + errorMessage);
            
            // Handle specific error types
            if (throwable.getCause() != null) {
                System.out.println("[API] Cause: " + throwable.getCause().getMessage());
            }
            
            // User-friendly error messages
            String userMessage = "Erreur serveur";
            if (errorMessage != null) {
                if (errorMessage.contains("Connection refused") || errorMessage.contains("ConnectException")) {
                    userMessage = "Serveur non accessible. V\u00e9rifiez que le serveur est d\u00e9marr\u00e9.";
                } else if (errorMessage.contains("timeout") || errorMessage.contains("timed out")) {
                    userMessage = "Timeout de connexion. Serveur trop lent \u00e0 r\u00e9pondre.";
                } else {
                    userMessage = "Erreur serveur: " + errorMessage;
                }
            }
            
            showError(userMessage);
            connectionStatusLabel.setText("❌ Erreur serveur");
        });
        return null;
    }
    
    @FXML
    private void handleStartGame() {
        if (!isHost) {
            showError("Seul l'hôte peut démarrer la partie");
            return;
        }
        
        if (connectedPlayers.size() < 1) {
            showError("Pas assez de joueurs");
            return;
        }
        
        try {
            // Build HTTP request - sessionId is part of the URL path
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/" + sessionId + "/start"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))  // Empty JSON body
                .build();
            
            System.out.println("[REST] Sending START_GAME request");
            noticeLabel.setText("Démarrage de la partie...");
            startGameButton.setDisable(true);
            
            // Send request asynchronously
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::handleStartGameResponse)
                .exceptionally(this::handleAPIError);
            
        } catch (Exception e) {
            showError("Erreur lors du démarrage: " + e.getMessage());
        }
    }
    
    private void handleStartGameResponse(String responseBody) {
        Platform.runLater(() -> {
            try {
                JsonNode response = objectMapper.readTree(responseBody);
                System.out.println("[API] Start game response: " + responseBody);
                
                // Extract game state information
                String status = response.has("status") ? response.get("status").asText() : "UNKNOWN";
                String letter = response.has("letter") ? response.get("letter").asText() : null;
                
                System.out.println("[LOBBY] Game started via REST API - Status: " + status + ", Letter: " + letter);
                
                // Navigate to game screen
                navigateToGameScreen();
                
            } catch (Exception e) {
                System.out.println("[API] Error parsing start game response: " + e.getMessage());
                showError("Erreur lors du traitement de la réponse: " + e.getMessage());
            }
        });
    }
    
    private void navigateToGameScreen() {
        try {
            Stage stage = (Stage) createGameButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MultiplayerGame.fxml"));
            Parent root = loader.load();
            
            // Pass game state data to the multiplayer game controller
            MultiplayerGameController gameController = loader.getController();
            if (gameController != null) {
                gameController.setDarkMode(darkMode);
            }
            
            Scene scene = new Scene(root, 1000, 750);
            
            // Apply current theme
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
            
            stage.setScene(scene);
            stage.show();
            
        } catch (IOException e) {
            showError("Erreur lors du chargement de l'écran de jeu: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleBackToMenu() {
        try {
            Stage stage = (Stage) createGameButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MainMenu.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 750);
            
            // Apply current theme
            if (darkMode) {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
            }
            
            // Pass dark mode setting back to main menu
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
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur s'est produite");
        alert.setContentText(message);
        alert.showAndWait();
    }
}