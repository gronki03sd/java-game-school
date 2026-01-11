package com.baccalaureat.multiplayer;

import com.baccalaureat.multiplayer.websocket.MultiplayerMessageListener;
import com.baccalaureat.multiplayer.websocket.MultiplayerWebSocketClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * High-level service for multiplayer game coordination.
 * Wraps the WebSocket client and provides a clean API for controllers.
 * Handles message routing and event dispatch.
 */
public class MultiplayerService implements MultiplayerMessageListener {
    
    private static final System.Logger logger = System.getLogger(MultiplayerService.class.getName());
    
    private final MultiplayerWebSocketClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<MultiplayerEventListener> eventListeners = new CopyOnWriteArrayList<>();
    
    // Connection state
    private boolean connected = false;
    private String currentSessionId = null;
    private String currentPlayerName = null;
    private boolean isHost = false;
    
    public MultiplayerService() {
        this.client = new MultiplayerWebSocketClient();
        this.client.addListener(this);
    }
    
    /**
     * Connect to the multiplayer server
     */
    public boolean connect(String serverUrl) {
        logger.log(System.Logger.Level.INFO, "Connecting to multiplayer server: " + serverUrl);
        return client.connect(serverUrl);
    }
    
    /**
     * Disconnect from the server
     */
    public void disconnect() {
        logger.log(System.Logger.Level.INFO, "Disconnecting from multiplayer server");
        client.disconnect();
        connected = false;
        currentSessionId = null;
        isHost = false;
    }
    
    /**
     * Create a new game session (become host)
     */
    public void createGame(String playerName, List<String> categories, int roundDuration) {
        this.currentPlayerName = playerName;
        this.isHost = true;
        
        logger.log(System.Logger.Level.INFO, "Creating game session: player=" + playerName);
        client.sendJoinGame("CREATE", playerName);
    }
    
    /**
     * Join an existing game session
     */
    public void joinGame(String sessionId, String playerName) {
        this.currentPlayerName = playerName;
        this.currentSessionId = sessionId;
        this.isHost = false;
        
        logger.log(System.Logger.Level.INFO, "Joining game session: " + sessionId + " as " + playerName);
        client.sendJoinGame(sessionId, playerName);
    }
    
    /**
     * Start the game (host only)
     */
    public void startGame() {
        if (!isHost) {
            logger.log(System.Logger.Level.WARNING, "Only host can start the game");
            return;
        }
        
        logger.log(System.Logger.Level.INFO, "Starting game session: " + currentSessionId);
        // Send start game message to server
        // Format will depend on server implementation
    }
    
    /**
     * Submit player answers at round end
     */
    public void submitAnswers(Map<String, String> answers) {
        logger.log(System.Logger.Level.INFO, "Submitting answers for round");
        client.sendSubmitAnswers(answers);
    }
    
    /**
     * Signal readiness for next round
     */
    public void readyForNextRound() {
        logger.log(System.Logger.Level.INFO, "Player ready for next round");
        client.sendReadyForNextRound();
    }
    
    /**
     * Add an event listener
     */
    public void addEventListener(MultiplayerEventListener listener) {
        eventListeners.add(listener);
    }
    
    /**
     * Remove an event listener
     */
    public void removeEventListener(MultiplayerEventListener listener) {
        eventListeners.remove(listener);
    }
    
    // Getters
    public boolean isConnected() {
        return connected && client.isConnected();
    }
    
    public String getCurrentSessionId() {
        return currentSessionId;
    }
    
    public String getCurrentPlayerName() {
        return currentPlayerName;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    // MultiplayerMessageListener implementation
    
    @Override
    public void onConnected() {
        connected = true;
        logger.log(System.Logger.Level.INFO, "Connected to multiplayer server");
        notifyListeners(listener -> listener.onConnectionEstablished());
    }
    
    @Override
    public void onDisconnected() {
        connected = false;
        logger.log(System.Logger.Level.INFO, "Disconnected from multiplayer server");
        notifyListeners(listener -> listener.onConnectionLost());
    }
    
    @Override
    public void onError(String message) {
        logger.log(System.Logger.Level.ERROR, "Multiplayer error: " + message);
        notifyListeners(listener -> listener.onError(message));
    }
    
    @Override
    public void onMessageReceived(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String messageType = node.has("type") ? node.get("type").asText() : "UNKNOWN";
            
            logger.log(System.Logger.Level.INFO, "Processing message type: " + messageType);
            
            switch (messageType) {
                case "gameCreated" -> handleGameCreated(node);
                case "playerJoined" -> handlePlayerJoined(node);
                case "gameStarted" -> handleGameStarted(node);
                case "roundEnded" -> handleRoundEnded(node);
                case "resultsReceived" -> handleResultsReceived(node);
                case "error" -> handleError(node);
                default -> logger.log(System.Logger.Level.WARNING, "Unknown message type: " + messageType);
            }
            
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to process message", e);
            notifyListeners(listener -> listener.onError("Failed to process server message: " + e.getMessage()));
        }
    }
    
    // Message handlers
    
    private void handleGameCreated(JsonNode node) {
        String sessionId = node.has("sessionId") ? node.get("sessionId").asText() : null;
        boolean success = node.has("success") ? node.get("success").asBoolean() : false;
        
        if (success && sessionId != null) {
            currentSessionId = sessionId;
            isHost = true;
            logger.log(System.Logger.Level.INFO, "Game created successfully: " + sessionId);
            notifyListeners(listener -> listener.onGameCreated(sessionId));
        } else {
            String error = node.has("error") ? node.get("error").asText() : "Failed to create game";
            logger.log(System.Logger.Level.ERROR, "Game creation failed: " + error);
            notifyListeners(listener -> listener.onError(error));
        }
    }
    
    private void handlePlayerJoined(JsonNode node) {
        String playerName = node.has("playerName") ? node.get("playerName").asText() : "Unknown";
        String sessionId = node.has("sessionId") ? node.get("sessionId").asText() : currentSessionId;
        
        logger.log(System.Logger.Level.INFO, "Player joined: " + playerName + " in session " + sessionId);
        notifyListeners(listener -> listener.onPlayerJoined(playerName));
    }
    
    private void handleGameStarted(JsonNode node) {
        try {
            String letter = node.has("letter") ? node.get("letter").asText() : null;
            int duration = node.has("duration") ? node.get("duration").asInt() : 60;
            
            List<String> categories = new ArrayList<>();
            if (node.has("categories") && node.get("categories").isArray()) {
                node.get("categories").forEach(cat -> categories.add(cat.asText()));
            }
            
            logger.log(System.Logger.Level.INFO, 
                "Game started: letter=" + letter + ", duration=" + duration + ", categories=" + categories.size());
            
            notifyListeners(listener -> listener.onGameStarted(letter, categories, duration));
            
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to parse game start data", e);
        }
    }
    
    private void handleRoundEnded(JsonNode node) {
        logger.log(System.Logger.Level.INFO, "Round ended");
        notifyListeners(listener -> listener.onRoundEnded());
    }
    
    private void handleResultsReceived(JsonNode node) {
        logger.log(System.Logger.Level.INFO, "Results received");
        // Parse results and notify
        notifyListeners(listener -> listener.onResultsReceived(node));
    }
    
    private void handleError(JsonNode node) {
        String errorMessage = node.has("message") ? node.get("message").asText() : "Unknown error";
        logger.log(System.Logger.Level.ERROR, "Server error: " + errorMessage);
        notifyListeners(listener -> listener.onError(errorMessage));
    }
    
    // Helper to notify all listeners on JavaFX thread
    private void notifyListeners(Consumer<MultiplayerEventListener> action) {
        Platform.runLater(() -> {
            for (MultiplayerEventListener listener : eventListeners) {
                try {
                    action.accept(listener);
                } catch (Exception e) {
                    logger.log(System.Logger.Level.ERROR, "Error in event listener", e);
                }
            }
        });
    }
}