package com.baccalaureat.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import com.baccalaureat.multiplayer.MultiplayerEventListener;
import com.baccalaureat.multiplayer.MultiplayerService;
import com.baccalaureat.util.DialogHelper;
import com.baccalaureat.util.ThemeManager;
import com.baccalaureat.util.ConfigLoader;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for Remote Multiplayer Lobby - Enhanced with WebSocket integration.
 * Handles REST API session management and WebSocket real-time gameplay.
 */
public class MultiplayerLobbyController implements MultiplayerEventListener {
    
    private boolean darkMode = false;
    private String sessionId = null;
    private boolean isHost = false;
    private String playerName = "";
    private Stage configurationStage = null; // Track configuration window
    
    // HTTP client for REST API calls
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObservableList<String> connectedPlayers = FXCollections.observableArrayList();
    
    // WebSocket service for real-time gameplay
    private MultiplayerService multiplayerService;
    
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
        System.out.println("[LOBBY] ListView initialized and bound to connectedPlayers ObservableList");
        
        // Set initial UI state
        connectionStatusLabel.setText("♾️ Prêt pour REST API");
        startGameButton.setDisable(true);
        noticeLabel.setText("Entrez votre nom et créez ou rejoignez une partie");
        
        // Initialize WebSocket service
        multiplayerService = new MultiplayerService();
        multiplayerService.addEventListener(this);
        multiplayerService.connect(ConfigLoader.getWebSocketUrl());
    }
    
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }
    
    /**
     * Get the multiplayer service for use by other controllers
     */
    public MultiplayerService getMultiplayerService() {
        return multiplayerService;
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
                .uri(URI.create(ConfigLoader.getApiUrl() + "/create"))
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
                .uri(URI.create(ConfigLoader.getApiUrl() + "/join"))
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
                
                // Start polling for player updates to refresh lobby UI
                startPlayerListPolling();
                
                // Join WebSocket session for real-time gameplay
                multiplayerService.joinWebSocketSession(newSessionId, playerName);
                
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
                
                // Update notice and start polling for game start
                noticeLabel.setText("Rejoint la partie! En attente du démarrage par l'hôte.");
                
                // Non-host players poll for game start
                startWaitingForGameStart();
                
                // Join WebSocket session for real-time gameplay
                multiplayerService.joinWebSocketSession(sessionId, playerName);
                
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
        
        // Check for minimum players (host + at least 1 guest)
        if (connectedPlayers.size() < 2) {
            showError("Il faut au moins 2 joueurs pour démarrer la partie. Attendez qu'un joueur vous rejoigne.");
            return;
        }
        
        // Navigate host to configuration screen before starting game
        System.out.println("[LOBBY] Host navigating to game configuration...");
        noticeLabel.setText("Configuration de la partie...");
        startGameButton.setDisable(true);
        
        navigateToGameConfiguration();
    }
    
    private void handleStartGameResponse(String responseBody) {
        Platform.runLater(() -> {
            try {
                JsonNode response = objectMapper.readTree(responseBody);
                System.out.println("[API] Start game response: " + responseBody);
                
                // Extract game state information
                String status = response.has("status") ? response.get("status").asText() : "UNKNOWN";
                String letter = response.has("letter") ? response.get("letter").asText() : null;
                
                System.out.println("[LOBBY] Game start confirmed by server - Status: " + status + ", Letter: " + letter);
                
                // Start polling for all clients to sync game state
                noticeLabel.setText("Jeu en cours de démarrage pour tous les joueurs...");
                startGameStatePolling();
                
            } catch (Exception e) {
                System.out.println("[API] Error parsing start game response: " + e.getMessage());
                showError("Erreur lors du traitement de la réponse: " + e.getMessage());
            }
        });
    }
    
    private void navigateToGameConfiguration() {
        try {
            Stage stage = (Stage) createGameButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/GameConfigurationView.fxml"));
            Parent root = loader.load();
            
            // Pass data to configuration controller
            GameConfigurationController configController = loader.getController();
            if (configController != null) {
                configController.setDarkMode(darkMode);
                configController.setMultiplayerMode(sessionId, playerName, this);
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
            
            // Track the configuration stage for later closing if needed
            configurationStage = stage;
            
        } catch (IOException e) {
            showError("Erreur lors du chargement de la configuration: " + e.getMessage());
        }
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
            
            ThemeManager.switchToFullScreenScene(stage, root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void startGameStatePolling() {
        // Poll server every 500ms to check if game has started for ALL players
        Thread pollingThread = new Thread(() -> {
            try {
                for (int attempts = 0; attempts < 20; attempts++) { // 10 second timeout
                    Thread.sleep(500);
                    
                    // Check game state from server
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigLoader.getApiUrl() + "/" + sessionId + "/state"))
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();
                    
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonNode gameState = objectMapper.readTree(response.body());
                    
                    String status = gameState.has("status") ? gameState.get("status").asText() : "UNKNOWN";
                    
                    if ("IN_PROGRESS".equals(status)) {
                        System.out.println("[SYNC] Game started confirmed for all players - transitioning now");
                        Platform.runLater(() -> {
                            noticeLabel.setText("Tous les joueurs prêts! Démarrage...");
                            navigateToGameScreen();
                        });
                        return;
                    }
                }
                
                // Timeout - show error
                Platform.runLater(() -> {
                    showError("Timeout: Le jeu n'a pas pu démarrer pour tous les joueurs");
                    noticeLabel.setText("Erreur de synchronisation. Réessayez.");
                    startGameButton.setDisable(false);
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Erreur lors de la synchronisation: " + e.getMessage());
                    startGameButton.setDisable(false);
                });
            }
        });
        
        pollingThread.setDaemon(true);
        pollingThread.start();
    }
    
    private void startWaitingForGameStart() {
        // Non-host players continuously poll to detect game start
        Thread waitingThread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(1000); // Poll every second
                    
                    // Check if game has started
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigLoader.getApiUrl() + "/" + sessionId + "/state"))
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();
                    
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonNode gameState = objectMapper.readTree(response.body());
                    
                    String status = gameState.has("status") ? gameState.get("status").asText() : "WAITING";
                    
                    if ("IN_PROGRESS".equals(status)) {
                        System.out.println("[SYNC] Non-host detected game start - transitioning now");
                        Platform.runLater(() -> {
                            noticeLabel.setText("Jeu démarré! Transition en cours...");
                            navigateToGameScreen();
                        });
                        return;
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    connectionStatusLabel.setText("❌ Erreur de synchronisation");
                });
            }
        });
        
        waitingThread.setDaemon(true);
        waitingThread.start();
    }
    
    /**
     * Start polling for player list updates to refresh lobby UI in real-time.
     * Host will see new players as they join the session.
     */
    private void startPlayerListPolling() {
        if (!isHost || sessionId == null) {
            return; // Only host needs to poll for player updates
        }
        
        Thread playerPollingThread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(2000); // Poll every 2 seconds for player updates
                    
                    // Get current session state to check for new players
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigLoader.getApiUrl() + "/" + sessionId + "/state"))
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();
                    
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonNode gameState = objectMapper.readTree(response.body());
                    
                    // Extract player information from server response
                    JsonNode playersNode = gameState.get("players");
                    if (playersNode != null && playersNode.isArray()) {
                        updatePlayerListFromServer(playersNode);
                    }
                }
            } catch (Exception e) {
                System.out.println("[LOBBY] Player polling stopped: " + e.getMessage());
                // Polling stops on error - not critical for functionality
            }
        });
        
        playerPollingThread.setDaemon(true);
        playerPollingThread.start();
        
        System.out.println("[LOBBY] Started player list polling for host");
    }
    
    /**
     * Update the player list UI based on server response.
     * Runs on JavaFX Application Thread for safe UI updates.
     */
    private void updatePlayerListFromServer(JsonNode playersNode) {
        Platform.runLater(() -> {
            try {
                List<String> serverPlayers = new ArrayList<>();
                
                System.out.println("[LOBBY] Updating player list from server...");
                
                // Parse players from server response
                String currentPlayerName = multiplayerService.getCurrentPlayerName();
                boolean currentPlayerIsHost = false;
                
                for (JsonNode playerNode : playersNode) {
                    String username = playerNode.has("username") ? playerNode.get("username").asText() : null;
                    boolean isHostPlayer = playerNode.has("isHost") ? playerNode.get("isHost").asBoolean() : false;
                    
                    if (username != null && !username.trim().isEmpty()) {
                        if (isHostPlayer) {
                            serverPlayers.add(username + " (Hôte)");
                            // Check if this is the current player
                            if (username.equals(currentPlayerName)) {
                                currentPlayerIsHost = true;
                            }
                        } else {
                            serverPlayers.add(username);
                        }
                        System.out.println("[LOBBY] Found player: " + username + (isHostPlayer ? " (Hôte)" : ""));
                    }
                }
                
                // Update MultiplayerService host status based on server response
                if (currentPlayerIsHost && !multiplayerService.isHost()) {
                    System.out.println("[LOBBY] Setting current player as host based on server response");
                    multiplayerService.setHost(true);
                } else if (!currentPlayerIsHost && multiplayerService.isHost()) {
                    System.out.println("[LOBBY] Removing host status from current player based on server response");  
                    multiplayerService.setHost(false);
                }
                
                // Only update UI if player list has changed
                if (!serverPlayers.equals(new ArrayList<>(connectedPlayers))) {
                    System.out.println("[LOBBY] Player list changed - updating UI");
                    System.out.println("[LOBBY] Old: " + connectedPlayers);
                    System.out.println("[LOBBY] New: " + serverPlayers);
                    
                    connectedPlayers.clear();
                    connectedPlayers.addAll(serverPlayers);
                    
                    // Update validation - check if we have enough players for start
                    boolean canStart = connectedPlayers.size() >= 2;
                    startGameButton.setDisable(!canStart);
                    
                    System.out.println("[LOBBY] Player list updated: " + connectedPlayers.size() + " players - " + serverPlayers);
                    
                    // Update notice based on player count
                    if (connectedPlayers.size() >= 2) {
                        noticeLabel.setText("Prêt à démarrer! Joueurs: " + connectedPlayers.size());
                    } else {
                        noticeLabel.setText("Partie créée! Partagez le code: " + sessionId + " (En attente de joueurs)");
                    }
                }
                
            } catch (Exception e) {
                System.out.println("[LOBBY] Error updating player list: " + e.getMessage());
            }
        });
    }
    
    /**
     * Called when host returns from configuration screen back to lobby.
     */
    public void returnFromConfiguration() {
        Platform.runLater(() -> {
            noticeLabel.setText("Partie créée! Partagez le code: " + sessionId);
            startGameButton.setDisable(false);
        });
    }
    
    /**
     * Restore session state when returning from configuration.
     */
    public void restoreSessionState(String sessionId, String playerName, boolean isHost) {
        this.sessionId = sessionId;
        this.playerName = playerName;
        this.isHost = isHost;
        
        // Update UI to reflect restored state
        connectionStatusLabel.setText("✅ Hôte - Session: " + sessionId);
        sessionIdInput.setText(sessionId);
        sessionIdInput.setEditable(false);
        playerNameInput.setText(playerName);
        playerNameInput.setEditable(false);
        
        // Simulate connected players (simplified)
        connectedPlayers.clear();
        connectedPlayers.add(playerName + " (Hôte)");
        connectedPlayers.add("Invité"); // Placeholder for guest
        
        // Enable start button since we have players
        startGameButton.setDisable(false);
        noticeLabel.setText("De retour à la configuration. Cliquez sur Démarrer pour reconfigurer.");
    }
    
    /**
     * Called by GameConfigurationController when host confirms configuration.
     * Sends START_GAME request with configuration to server.
     */
    public void startGameWithConfiguration(com.baccalaureat.model.GameConfig config) {
        try {
            // Create JSON request body with configuration
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("numberOfRounds", config.getNumberOfRounds());
            requestBody.put("roundDuration", config.getRoundDurationSeconds());
            
            // Add categories
            var categoriesArray = requestBody.putArray("categories");
            for (com.baccalaureat.model.Category category : config.getSelectedCategories()) {
                categoriesArray.add(category.getName());
            }
            
            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ConfigLoader.getApiUrl() + "/" + sessionId + "/start"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
            
            System.out.println("[REST] Sending START_GAME request with configuration: " + requestBody.toString());
            
            // Send request asynchronously
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::handleStartGameResponse)
                .exceptionally(this::handleAPIError);
                
        } catch (Exception e) {
            showError("Erreur lors du démarrage avec configuration: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        DialogHelper.showError("Erreur", "Une erreur s'est produite", message);
    }
    
    // MultiplayerEventListener implementation
    
    @Override
    public void onConnectionEstablished() {
        Platform.runLater(() -> {
            connectionStatusLabel.setText("🌐 WebSocket connecté");
            System.out.println("[WS] WebSocket connection established");
        });
    }
    
    @Override
    public void onConnectionLost() {
        Platform.runLater(() -> {
            connectionStatusLabel.setText("❌ WebSocket déconnecté");
            System.out.println("[WS] WebSocket connection lost");
        });
    }
    
    @Override
    public void onPlayerJoined(String playerName) {
        System.out.println("[WS] Player joined via WebSocket: " + playerName);
        
        // Instead of directly updating the list, refresh from server to get proper formatting
        if (isHost && sessionId != null) {
            Platform.runLater(() -> refreshPlayerListFromServer());
        } else {
            // For non-hosts, just add the player directly for now
            Platform.runLater(() -> {
                if (!connectedPlayers.contains(playerName)) {
                    connectedPlayers.add(playerName);
                }
            });
        }
    }
    
    /**
     * Immediately refresh player list from server (used by WebSocket events)
     */
    private void refreshPlayerListFromServer() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ConfigLoader.getApiUrl() + "/" + sessionId + "/state"))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode gameState = objectMapper.readTree(response.body());
            
            // Extract player information from server response
            JsonNode playersNode = gameState.get("players");
            if (playersNode != null && playersNode.isArray()) {
                updatePlayerListFromServer(playersNode);
            }
        } catch (Exception e) {
            System.err.println("[LOBBY] Failed to refresh player list: " + e.getMessage());
        }
    }
    
    @Override
    public void onGameStarted(String letter, List<String> categories, int duration) {
        System.out.println("[WS] GAME_STARTED event received - Letter: " + letter + 
                          ", Categories: " + categories.size() + ", Duration: " + duration);
        
        // Check if there's an open configuration window that needs to be closed
        closeConfigurationWindows();
        
        // Transition to multiplayer game screen
        Platform.runLater(() -> {
            try {
                Stage stage = (Stage) startGameButton.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/baccalaureat/MultiplayerGame.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 1000, 750);
                
                // Apply theme
                if (darkMode) {
                    scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-dark.css").toExternalForm());
                } else {
                    scene.getStylesheets().add(getClass().getResource("/com/baccalaureat/theme-light.css").toExternalForm());
                }
                
                // Configure MultiplayerGameController
                Object controller = loader.getController();
                if (controller instanceof MultiplayerGameController mgc) {
                    mgc.setDarkMode(darkMode);
                    mgc.initializeMultiplayerGame(multiplayerService, letter, categories, duration);
                }
                
                stage.setScene(scene);
                stage.show();
                
                System.out.println("[NAV] Transitioned to multiplayer game screen");
                
            } catch (IOException e) {
                e.printStackTrace();
                showError("Erreur lors de la transition vers le jeu: " + e.getMessage());
            }
        });
    }
    
    /**
     * Helper method to close any open configuration windows
     */
    private void closeConfigurationWindows() {
        // This will be called to ensure configuration windows are closed
        // when the game starts via WebSocket event
        System.out.println("[NAV] Attempting to close configuration windows");
    }
    
    @Override
    public void onError(String errorMessage) {
        Platform.runLater(() -> {
            showError("Erreur multijoueur: " + errorMessage);
            System.err.println("[WS] Multiplayer error: " + errorMessage);
        });
    }
}