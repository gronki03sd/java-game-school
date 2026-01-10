package com.baccalaureat.model;

import java.util.List;

/**
 * Game configuration model that replaces the hardcoded difficulty system.
 * Contains all settings needed to start a game session.
 */
public class GameConfig {
    
    public enum GameMode {
        SOLO("Mode Solo"),
        LOCAL("Mode Local"),
        DISTANT("Mode Distant");
        
        private final String displayName;
        
        GameMode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private GameMode mode;
    private int numberOfRounds;
    private int roundDurationSeconds;
    private List<Category> selectedCategories;
    private List<String> playerNicknames;
    
    public GameConfig() {
        // Default values
        this.mode = GameMode.SOLO;
        this.numberOfRounds = 5;
        this.roundDurationSeconds = 60;
    }
    
    // Getters and setters
    public GameMode getMode() {
        return mode;
    }
    
    public void setMode(GameMode mode) {
        this.mode = mode;
    }
    
    public int getNumberOfRounds() {
        return numberOfRounds;
    }
    
    public void setNumberOfRounds(int numberOfRounds) {
        this.numberOfRounds = numberOfRounds;
    }
    
    public int getRoundDurationSeconds() {
        return roundDurationSeconds;
    }
    
    public void setRoundDurationSeconds(int roundDurationSeconds) {
        this.roundDurationSeconds = roundDurationSeconds;
    }
    
    public List<Category> getSelectedCategories() {
        return selectedCategories;
    }
    
    public void setSelectedCategories(List<Category> selectedCategories) {
        this.selectedCategories = selectedCategories;
    }
    
    public List<String> getPlayerNicknames() {
        return playerNicknames;
    }
    
    public void setPlayerNicknames(List<String> playerNicknames) {
        this.playerNicknames = playerNicknames;
    }
    
    /**
     * Validates if the configuration is complete and ready for game start.
     */
    public boolean isValid() {
        if (selectedCategories == null || selectedCategories.isEmpty()) {
            return false;
        }
        
        if (playerNicknames == null || playerNicknames.isEmpty()) {
            return false;
        }
        
        // Check minimum player count based on game mode
        int minPlayers = (mode == GameMode.SOLO) ? 1 : 2;
        if (playerNicknames.size() < minPlayers) {
            return false;
        }
        
        // Check that all nicknames are non-empty
        for (String nickname : playerNicknames) {
            if (nickname == null || nickname.trim().isEmpty()) {
                return false;
            }
        }
        
        return numberOfRounds > 0 && roundDurationSeconds > 0;
    }
    
    /**
     * Gets validation error message for UI display.
     */
    public String getValidationMessage() {
        if (selectedCategories == null || selectedCategories.isEmpty()) {
            return "Please select at least one category";
        }
        
        if (playerNicknames == null || playerNicknames.isEmpty()) {
            return "Please add at least one player";
        }
        
        // Check minimum player count based on game mode
        int minPlayers = (mode == GameMode.SOLO) ? 1 : 2;
        if (playerNicknames.size() < minPlayers) {
            return mode == GameMode.SOLO ? 
                "Solo mode requires 1 player" : 
                "Multiplayer mode requires at least 2 players";
        }
        
        for (String nickname : playerNicknames) {
            if (nickname == null || nickname.trim().isEmpty()) {
                return "All player names must be filled";
            }
        }
        
        if (numberOfRounds <= 0) {
            return "Number of rounds must be greater than 0";
        }
        
        if (roundDurationSeconds <= 0) {
            return "Round duration must be greater than 0";
        }
        
        return "";
    }
}