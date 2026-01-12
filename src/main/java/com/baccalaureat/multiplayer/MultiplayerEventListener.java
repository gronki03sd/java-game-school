package com.baccalaureat.multiplayer;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Interface for handling multiplayer game events.
 * Controllers should implement this to react to server events.
 */
public interface MultiplayerEventListener {
    
    /**
     * Called when connection to server is established
     */
    default void onConnectionEstablished() {}
    
    /**
     * Called when connection to server is lost
     */
    default void onConnectionLost() {}
    
    /**
     * Called when a game session is successfully created
     * @param sessionId The unique session identifier
     */
    default void onGameCreated(String sessionId) {}
    
    /**
     * Called when a player joins the current session
     * @param playerName Name of the player who joined
     */
    default void onPlayerJoined(String playerName) {}
    
    /**
     * Called when the game round starts
     * @param letter The starting letter for the round
     * @param categories List of categories to fill
     * @param duration Round duration in seconds
     */
    default void onGameStarted(String letter, List<String> categories, int duration) {}
    
    /**
     * Called when the round time expires
     */
    default void onRoundEnded() {}
    
    /**
     * Called when results are received from server
     * @param results JSON node containing result data
     */
    default void onResultsReceived(JsonNode results) {}
    
    /**
     * Called when a new round starts
     * @param letter The new letter for this round
     * @param currentRound Current round number
     * @param totalRounds Total number of rounds
     */
    default void onRoundStarted(String letter, int currentRound, int totalRounds) {}
    
    /**
     * Called when the game ends and leaderboard is available
     * @param leaderboard JSON array of final player rankings
     */
    default void onGameEnded(JsonNode leaderboard) {}
    
    /**
     * Called when an error occurs
     * @param errorMessage Human-readable error description
     */
    default void onError(String errorMessage) {}
}