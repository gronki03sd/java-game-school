package com.baccalaureat.multiplayer.websocket;

import java.util.HashMap;
import java.util.Map;

/**
 * Example usage of the MultiplayerWebSocketClient.
 * This class demonstrates how to integrate the WebSocket client with application logic.
 * 
 * This is NOT part of the UI layer - it's a reference implementation showing
 * proper usage patterns for the WebSocket client.
 */
public class WebSocketClientExample implements MultiplayerMessageListener {
    
    private static final System.Logger logger = System.getLogger(WebSocketClientExample.class.getName());
    
    private final MultiplayerWebSocketClient client;
    
    public WebSocketClientExample() {
        this.client = new MultiplayerWebSocketClient();
        this.client.addListener(this);
    }
    
    /**
     * Example: Connect to server and join a game session.
     */
    public void connectAndJoinGame(String serverUrl, String sessionCode, String playerName) {
        // Step 1: Connect to WebSocket server
        boolean connected = client.connect(serverUrl);
        if (!connected) {
            logger.log(System.Logger.Level.ERROR, "Failed to initiate connection to server");
            return;
        }
        
        // Note: Actual join will happen in onConnected() callback
        // Store session code and player name for later use
        // This would typically be handled by a proper state management system
    }
    
    /**
     * Example: Submit answers for the current round.
     */
    public void submitAnswers(Map<String, String> answers) {
        if (client.isConnected()) {
            boolean sent = client.sendSubmitAnswers(answers);
            if (sent) {
                logger.log(System.Logger.Level.INFO, "Answers submitted successfully");
            } else {
                logger.log(System.Logger.Level.ERROR, "Failed to submit answers");
            }
        } else {
            logger.log(System.Logger.Level.WARNING, "Cannot submit answers: not connected to server");
        }
    }
    
    /**
     * Example: Signal readiness for next round.
     */
    public void readyForNextRound() {
        if (client.isConnected()) {
            boolean sent = client.sendReadyForNextRound();
            if (sent) {
                logger.log(System.Logger.Level.INFO, "Ready signal sent");
            } else {
                logger.log(System.Logger.Level.ERROR, "Failed to send ready signal");
            }
        } else {
            logger.log(System.Logger.Level.WARNING, "Cannot signal ready: not connected to server");
        }
    }
    
    /**
     * Cleanup: Disconnect from server and remove listeners.
     */
    public void cleanup() {
        client.removeListener(this);
        client.disconnect();
    }
    
    // MultiplayerMessageListener implementation
    
    @Override
    public void onConnected() {
        logger.log(System.Logger.Level.INFO, "Successfully connected to multiplayer server");
        
        // Example: Auto-join game after connection is established
        // In a real application, this would be triggered by user action
        // client.sendJoinGame("ABC123", "PlayerName");
    }
    
    @Override
    public void onDisconnected() {
        logger.log(System.Logger.Level.INFO, "Disconnected from multiplayer server");
        
        // Handle disconnection - update UI, show reconnect options, etc.
        // This would typically update some application state or UI components
    }
    
    @Override
    public void onError(String message) {
        logger.log(System.Logger.Level.ERROR, "Multiplayer error: " + message);
        
        // Handle errors - show error messages, attempt reconnection, etc.
        // This would typically update UI error states or show user notifications
    }
    
    @Override
    public void onMessageReceived(String json) {
        logger.log(System.Logger.Level.DEBUG, "Received server message: " + json);
        
        // Parse JSON and handle different message types
        // Example message handling would go here:
        // - Game state updates
        // - Player join/leave notifications  
        // - Round start/end events
        // - Score updates
        // - Chat messages, etc.
        
        // This is where you would typically:
        // 1. Parse the JSON message
        // 2. Determine the message type
        // 3. Update application state
        // 4. Trigger UI updates (remember: this runs on JavaFX Application Thread)
    }
    
    /**
     * Example method showing how to create sample answers for testing.
     */
    public static Map<String, String> createSampleAnswers() {
        Map<String, String> answers = new HashMap<>();
        answers.put("Animals", "Elephant");
        answers.put("Colors", "Emerald");
        answers.put("Countries", "Ecuador");
        return answers;
    }
}