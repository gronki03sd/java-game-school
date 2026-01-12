package com.baccalaureat.multiplayer.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;

import jakarta.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket client for multiplayer communication with the game server.
 * Handles connection lifecycle, message sending/receiving, and event dispatch to listeners.
 * 
 * This class ensures thread safety and proper JavaFX Platform thread handling
 * for UI updates triggered by WebSocket events.
 */
@ClientEndpoint
public class MultiplayerWebSocketClient {
    
    private static final System.Logger logger = System.getLogger(MultiplayerWebSocketClient.class.getName());
    
    private final List<MultiplayerMessageListener> listeners = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    
    private Session session;
    private String serverUrl;
    
    /**
     * Creates a new WebSocket client instance.
     */
    public MultiplayerWebSocketClient() {
        // Default constructor
    }
    
    /**
     * Connects to the WebSocket server at the specified URL.
     * 
     * @param serverUrl WebSocket server URL (e.g., "ws://localhost:8080/websocket")
     * @return true if connection initiated successfully, false otherwise
     */
    public boolean connect(String serverUrl) {
        if (connected.get()) {
            logger.log(System.Logger.Level.WARNING, "Already connected to WebSocket server");
            return true;
        }
        
        this.serverUrl = serverUrl;
        
        try {
            logger.log(System.Logger.Level.INFO, "Connecting to WebSocket server: " + serverUrl);
            
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            URI serverUri = URI.create(serverUrl);
            
            // Connect asynchronously to avoid blocking JavaFX UI thread
            container.connectToServer(this, serverUri);
            
            return true;
            
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to connect to WebSocket server", e);
            notifyError("Connection failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Disconnects from the WebSocket server.
     */
    public void disconnect() {
        if (session != null && session.isOpen()) {
            try {
                logger.log(System.Logger.Level.INFO, "Disconnecting from WebSocket server");
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Client disconnect"));
            } catch (IOException e) {
                logger.log(System.Logger.Level.ERROR, "Error during disconnect", e);
            }
        }
        connected.set(false);
    }
    
    /**
     * Checks if the client is currently connected to the server.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected.get() && session != null && session.isOpen();
    }
    
    /**
     * Adds a message listener to receive WebSocket events and messages.
     * 
     * @param listener The listener to add
     */
    public void addListener(MultiplayerMessageListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a message listener.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(MultiplayerMessageListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Sends a raw JSON message to the server.
     * 
     * @param json JSON message to send
     * @return true if sent successfully, false otherwise
     */
    public boolean sendMessage(String json) {
        if (!isConnected()) {
            logger.log(System.Logger.Level.WARNING, "Cannot send message: not connected to server");
            return false;
        }
        
        try {
            logger.log(System.Logger.Level.INFO, "=== Sending WebSocket Message ===");
            logger.log(System.Logger.Level.INFO, "Outgoing JSON: " + json);
            session.getBasicRemote().sendText(json);
            logger.log(System.Logger.Level.INFO, "Message sent successfully");
            return true;
        } catch (IOException e) {
            logger.log(System.Logger.Level.ERROR, "Failed to send message", e);
            notifyError("Failed to send message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to send a join session message.
     * 
     * @param sessionId The session ID to join
     * @param playerName The player's display name
     * @return true if sent successfully, false otherwise
     */
    public boolean sendJoinSession(String sessionId, String playerName) {
        try {
            ObjectNode message = objectMapper.createObjectNode();
            message.put("type", "JOIN_SESSION");
            message.put("sessionId", sessionId);
            message.put("playerName", playerName);
            
            return sendMessage(objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to create join session message", e);
            notifyError("Failed to create join session message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to send game start with configuration.
     * 
     * @param config Game configuration including rounds, duration, categories
     * @return true if sent successfully, false otherwise
     */
    public boolean sendStartGame(Map<String, Object> config) {
        try {
            ObjectNode message = objectMapper.createObjectNode();
            message.put("type", "START_GAME");
            message.set("config", objectMapper.valueToTree(config));
            
            String messageJson = objectMapper.writeValueAsString(message);
            System.out.println("[WEBSOCKET] Sending START_GAME message: " + messageJson);
            
            boolean sent = sendMessage(messageJson);
            System.out.println("[WEBSOCKET] START_GAME message sent successfully: " + sent);
            return sent;
        } catch (Exception e) {
            System.err.println("[WEBSOCKET] Failed to create/send START_GAME message: " + e.getMessage());
            logger.log(System.Logger.Level.ERROR, "Failed to create start game message", e);
            notifyError("Failed to create start game message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to send player answers for the current round.
     * 
     * @param answers Map of category to player's answer
     * @return true if sent successfully, false otherwise
     */
    public boolean sendSubmitAnswers(Map<String, String> answers) {
        try {
            ObjectNode message = objectMapper.createObjectNode();
            message.put("type", "SUBMIT_ANSWERS");
            message.set("answers", objectMapper.valueToTree(answers));
            
            return sendMessage(objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to create submit answers message", e);
            notifyError("Failed to create submit answers message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to signal readiness for the next round.
     * 
     * @return true if sent successfully, false otherwise
     */
    public boolean sendReadyForNextRound() {
        try {
            ObjectNode message = objectMapper.createObjectNode();
            message.put("type", "NEXT_ROUND");
            
            return sendMessage(objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to create next round message", e);
            notifyError("Failed to create next round message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to end the game and show leaderboard.
     * 
     * @return true if sent successfully, false otherwise
     */
    public boolean sendEndGame() {
        try {
            ObjectNode message = objectMapper.createObjectNode();
            message.put("type", "END_GAME");
            
            return sendMessage(objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to create end game message", e);
            notifyError("Failed to create end game message: " + e.getMessage());
            return false;
        }
    }
    
    // WebSocket lifecycle callbacks
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("[WS] Connected - Session ID: " + session.getId());
        this.session = session;
        connected.set(true);
        logger.log(System.Logger.Level.INFO, "=== WebSocket Connection Opened ===");
        logger.log(System.Logger.Level.INFO, "Session ID: " + session.getId());
        logger.log(System.Logger.Level.INFO, "Server URL: " + serverUrl);
        logger.log(System.Logger.Level.INFO, "Connection successful - ready to send/receive messages");
        
        // Ensure UI updates happen on JavaFX Application Thread
        Platform.runLater(() -> {
            for (MultiplayerMessageListener listener : listeners) {
                try {
                    listener.onConnected();
                } catch (Exception e) {
                    logger.log(System.Logger.Level.ERROR, "Error in listener onConnected callback", e);
                }
            }
        });
    }
    
    @OnMessage
    public void onMessage(String message) {
        System.out.println("[WS] Message received: " + message.substring(0, Math.min(100, message.length())) + (message.length() > 100 ? "..." : ""));
        logger.log(System.Logger.Level.INFO, "=== WebSocket Message Received ===");
        logger.log(System.Logger.Level.INFO, "Raw JSON: " + message);
        
        // Try to parse and log message type
        try {
            var jsonNode = objectMapper.readTree(message);
            String messageType = jsonNode.has("type") ? jsonNode.get("type").asText() : "UNKNOWN";
            logger.log(System.Logger.Level.INFO, "Message Type: " + messageType);
            
            // Log key fields if present
            if (jsonNode.has("sessionId")) {
                logger.log(System.Logger.Level.INFO, "Session ID: " + jsonNode.get("sessionId").asText());
            }
            if (jsonNode.has("playerName")) {
                logger.log(System.Logger.Level.INFO, "Player Name: " + jsonNode.get("playerName").asText());
            }
            if (jsonNode.has("success")) {
                logger.log(System.Logger.Level.INFO, "Success: " + jsonNode.get("success").asBoolean());
            }
            if (jsonNode.has("error")) {
                logger.log(System.Logger.Level.INFO, "Error: " + jsonNode.get("error").asText());
            }
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, "Could not parse JSON message type: " + e.getMessage());
        }
        
        // Ensure UI updates happen on JavaFX Application Thread
        Platform.runLater(() -> {
            for (MultiplayerMessageListener listener : listeners) {
                try {
                    listener.onMessageReceived(message);
                } catch (Exception e) {
                    logger.log(System.Logger.Level.ERROR, "Error in listener onMessageReceived callback", e);
                }
            }
        });
    }
    
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("[WS] Disconnected - Code: " + closeReason.getCloseCode() + ", Reason: " + closeReason.getReasonPhrase());
        connected.set(false);
        this.session = null;
        logger.log(System.Logger.Level.INFO, "=== WebSocket Connection Closed ===");
        logger.log(System.Logger.Level.INFO, "Close Code: " + closeReason.getCloseCode());
        logger.log(System.Logger.Level.INFO, "Close Reason: " + closeReason.getReasonPhrase());
        logger.log(System.Logger.Level.INFO, "Session was: " + (session != null ? session.getId() : "null"));
        
        // Ensure UI updates happen on JavaFX Application Thread
        Platform.runLater(() -> {
            for (MultiplayerMessageListener listener : listeners) {
                try {
                    listener.onDisconnected();
                } catch (Exception e) {
                    logger.log(System.Logger.Level.ERROR, "Error in listener onDisconnected callback", e);
                }
            }
        });
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.log(System.Logger.Level.ERROR, "=== WebSocket Error Occurred ===");
        logger.log(System.Logger.Level.ERROR, "Error Type: " + throwable.getClass().getSimpleName());
        logger.log(System.Logger.Level.ERROR, "Error Message: " + throwable.getMessage());
        logger.log(System.Logger.Level.ERROR, "Session: " + (session != null ? session.getId() : "null"));
        logger.log(System.Logger.Level.ERROR, "Connection Status: " + connected.get());
        logger.log(System.Logger.Level.ERROR, "Full Exception:", throwable);
        notifyError("WebSocket error: " + throwable.getMessage());
    }
    
    /**
     * Notifies all listeners of an error in a thread-safe manner.
     * 
     * @param errorMessage Human-readable error message
     */
    private void notifyError(String errorMessage) {
        // Ensure UI updates happen on JavaFX Application Thread
        Platform.runLater(() -> {
            for (MultiplayerMessageListener listener : listeners) {
                try {
                    listener.onError(errorMessage);
                } catch (Exception e) {
                    logger.log(System.Logger.Level.ERROR, "Error in listener onError callback", e);
                }
            }
        });
    }
}