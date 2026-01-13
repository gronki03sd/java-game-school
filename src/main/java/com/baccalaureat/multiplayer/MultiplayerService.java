package com.baccalaureat.multiplayer;

import com.baccalaureat.multiplayer.websocket.MultiplayerMessageListener;
import com.baccalaureat.multiplayer.websocket.MultiplayerWebSocketClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.HashMap;
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
        // Note: Game creation is handled by REST API, not WebSocket
        // WebSocket connection will be established after REST API creates the session
    }
    
    /**
     * Join an existing game session
     */
    public void joinGame(String sessionId, String playerName) {
        this.currentPlayerName = playerName;
        this.currentSessionId = sessionId;
        this.isHost = false;
        
        logger.log(System.Logger.Level.INFO, "Joining game session: " + sessionId + " as " + playerName);
        // Note: Game joining is handled by REST API, not WebSocket
        // WebSocket connection will be established after REST API join
    }
    
    /**
     * Start the game (host only) with configuration
     */
    public void startGame(int numberOfRounds, int roundDuration, List<String> categories) {
        if (!isHost) {
            logger.log(System.Logger.Level.WARNING, "Only host can start the game");
            System.err.println("[MULTIPLAYER] Not host - cannot start game");
            return;
        }
        
        System.out.println("[MULTIPLAYER] Starting game session: " + currentSessionId);
        System.out.println("[MULTIPLAYER] Config - Rounds: " + numberOfRounds + ", Duration: " + roundDuration + "s, Categories: " + categories.size());
        
        // Create configuration map
        Map<String, Object> config = new HashMap<>();
        config.put("numberOfRounds", numberOfRounds);
        config.put("roundDuration", roundDuration);
        config.put("categories", categories);
        
        System.out.println("[MULTIPLAYER] Sending START_GAME message via WebSocket...");
        boolean sent = client.sendStartGame(config);
        System.out.println("[MULTIPLAYER] START_GAME message sent: " + sent);
    }
    
    /**
     * Join a WebSocket session after REST API join
     */
    public void joinWebSocketSession(String sessionId, String playerName) {
        this.currentSessionId = sessionId;
        this.currentPlayerName = playerName;
        // Don't change isHost flag here - it should be set by createGame() or joinGame()
        
        System.out.println("[MULTIPLAYER] Joining WebSocket session: " + sessionId + " as " + playerName + " (isHost: " + isHost + ")");
        logger.log(System.Logger.Level.INFO, "Joining WebSocket session: " + sessionId + " as " + playerName);
        client.sendJoinSession(sessionId, playerName);
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
     * End the game and show leaderboard
     */
    public void endGame() {
        logger.log(System.Logger.Level.INFO, "Ending game");
        client.sendEndGame();
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
    
    public void setHost(boolean isHost) {
        System.out.println("[MULTIPLAYER] Host status updated: " + this.isHost + " -> " + isHost);
        this.isHost = isHost;
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
        System.out.println("[MULTIPLAYER] WebSocket message received: " + json);
        
        try {
            JsonNode node = objectMapper.readTree(json);
            String messageType = node.has("type") ? node.get("type").asText() : "UNKNOWN";
            
            System.out.println("[MULTIPLAYER] Processing message type: " + messageType);
            logger.log(System.Logger.Level.INFO, "Processing message type: " + messageType);
            
            switch (messageType) {
                case "SESSION_JOINED" -> handleSessionJoined(node);
                case "PLAYER_JOINED" -> handlePlayerJoined(node);
                case "GAME_STARTED" -> handleGameStarted(node);
                case "ROUND_ENDED" -> handleRoundEnded(node);
                case "ROUND_STARTED" -> handleRoundStarted(node);
                case "GAME_ENDED" -> handleGameEnded(node);
                case "ERROR" -> handleError(node);
                default -> logger.log(System.Logger.Level.WARNING, "Unknown message type: " + messageType);
            }
            
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to process message", e);
            notifyListeners(listener -> listener.onError("Failed to process server message: " + e.getMessage()));
        }
    }
    
    // Message handlers
    
    private void handleSessionJoined(JsonNode node) {
        String sessionId = node.has("sessionId") ? node.get("sessionId").asText() : null;
        String playerName = node.has("playerName") ? node.get("playerName").asText() : null;
        
        logger.log(System.Logger.Level.INFO, "Successfully joined session: " + sessionId + " as " + playerName);
        this.currentSessionId = sessionId;
        this.currentPlayerName = playerName;
    }
    
    private void handlePlayerJoined(JsonNode node) {
        String playerName = node.has("playerName") ? node.get("playerName").asText() : "Unknown";
        
        logger.log(System.Logger.Level.INFO, "Player joined: " + playerName);
        notifyListeners(listener -> listener.onPlayerJoined(playerName));
    }
    
    private void handleGameStarted(JsonNode node) {
        System.out.println("[MULTIPLAYER] handleGameStarted called");
        System.out.println("[MULTIPLAYER] GAME_STARTED message: " + node.toString());
        
        try {
            String letter = node.has("letter") ? node.get("letter").asText() : null;
            int duration = node.has("roundDuration") ? node.get("roundDuration").asInt() : 60;
            int totalRounds = node.has("totalRounds") ? node.get("totalRounds").asInt() : 1;
            int currentRound = node.has("currentRound") ? node.get("currentRound").asInt() : 1;
            
            List<String> categories = new ArrayList<>();
            if (node.has("categories") && node.get("categories").isArray()) {
                node.get("categories").forEach(cat -> categories.add(cat.asText()));
            }
            
            System.out.println("[MULTIPLAYER] Parsed GAME_STARTED - Letter: " + letter + 
                             ", Duration: " + duration + "s, Categories: " + categories.size() +
                             ", Total Rounds: " + totalRounds + ", Current Round: " + currentRound);
            
            logger.log(System.Logger.Level.INFO, 
                "Game started: letter=" + letter + ", duration=" + duration + 
                ", categories=" + categories.size() + ", rounds=" + totalRounds);
            
            System.out.println("[MULTIPLAYER] Notifying " + this.eventListeners.size() + " listeners of GAME_STARTED");
            notifyListeners(listener -> listener.onGameStarted(letter, categories, duration));
            
        } catch (Exception e) {
            System.err.println("[MULTIPLAYER] Failed to parse game start data: " + e.getMessage());
            logger.log(System.Logger.Level.ERROR, "Failed to parse game start data", e);
        }
    }
    
    private void handleRoundEnded(JsonNode node) {
        logger.log(System.Logger.Level.INFO, "Round ended");
        
        // Extract round results if available
        JsonNode results = node.has("results") ? node.get("results") : null;
        notifyListeners(listener -> listener.onRoundEnded());
        
        if (results != null) {
            notifyListeners(listener -> listener.onResultsReceived(results));
        }
    }
    
    private void handleRoundStarted(JsonNode node) {
        String letter = node.has("letter") ? node.get("letter").asText() : null;
        int currentRound = node.has("currentRound") ? node.get("currentRound").asInt() : 1;
        int totalRounds = node.has("totalRounds") ? node.get("totalRounds").asInt() : 1;
        
        logger.log(System.Logger.Level.INFO, 
            "Round started: letter=" + letter + ", round=" + currentRound + "/" + totalRounds);
        
        notifyListeners(listener -> listener.onRoundStarted(letter, currentRound, totalRounds));
    }
    
    private void handleGameEnded(JsonNode node) {
        logger.log(System.Logger.Level.INFO, "Game ended");
        
        JsonNode leaderboard = node.has("leaderboard") ? node.get("leaderboard") : null;
        notifyListeners(listener -> listener.onGameEnded(leaderboard));
    }
    
    private void handleError(JsonNode node) {
        String errorMessage = node.has("message") ? node.get("message").asText() : "Unknown error";
        logger.log(System.Logger.Level.ERROR, "Server error: " + errorMessage);
        notifyListeners(listener -> listener.onError(errorMessage));
    }
    
    // Utility methods
    
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