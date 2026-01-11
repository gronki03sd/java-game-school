package com.baccalaureat.multiplayer.websocket;

/**
 * Interface for listening to multiplayer WebSocket events and messages.
 * Implementations of this interface will receive callbacks for connection state changes
 * and incoming messages from the multiplayer server.
 */
public interface MultiplayerMessageListener {
    
    /**
     * Called when the WebSocket connection is successfully established.
     */
    void onConnected();
    
    /**
     * Called when the WebSocket connection is closed or lost.
     */
    void onDisconnected();
    
    /**
     * Called when an error occurs with the WebSocket connection or message processing.
     * 
     * @param message Human-readable error description
     */
    void onError(String message);
    
    /**
     * Called when a JSON message is received from the server.
     * The implementation should parse and handle the JSON according to the message type.
     * 
     * @param json Raw JSON message from the server
     */
    void onMessageReceived(String json);
}