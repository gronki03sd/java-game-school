package com.baccalaureat.model;

import com.baccalaureat.service.CategoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameSession {
    private static int highScore = 0;
    private static int gamesPlayed = 0;

    private String currentLetter;
    private int currentScore;
    private int currentRound;
    private List<Category> categories;
    private List<String> usedLetters;
    private final GameConfig gameConfig;
    private final CategoryService categoryService;

    public GameSession() {
        // Default constructor for backward compatibility
        this.gameConfig = new GameConfig();
        this.currentScore = 0;
        this.currentRound = 1;
        this.usedLetters = new ArrayList<>();
        this.categoryService = new CategoryService();
        setupDefaultCategories();
        generateRandomLetter();
    }
    
    public GameSession(GameConfig config) {
        this.gameConfig = config;
        this.currentScore = 0;
        this.currentRound = 1;
        this.usedLetters = new ArrayList<>();
        this.categoryService = new CategoryService();
        this.categories = new ArrayList<>(config.getSelectedCategories());
        generateRandomLetter();
    }

    private void setupDefaultCategories() {
        // For backward compatibility, use enabled categories
        List<Category> allCategories = new ArrayList<>(categoryService.getEnabledCategories());
        this.categories = allCategories.subList(0, Math.min(6, allCategories.size()));
        gameConfig.setSelectedCategories(this.categories);
    }

    public void generateRandomLetter() {
        Random r = new Random();
        String excludeLetters = "WXYZ"; // Difficult letters
        char letter;
        do {
            letter = (char) ('A' + r.nextInt(26));
        } while (excludeLetters.indexOf(letter) >= 0 || usedLetters.contains(String.valueOf(letter)));
        
        this.currentLetter = String.valueOf(letter);
        usedLetters.add(currentLetter);
    }

    public boolean nextRound() {
        if (currentRound >= gameConfig.getNumberOfRounds()) {
            endGame();
            return false;
        }
        currentRound++;
        generateRandomLetter();
        return true;
    }

    public void endGame() {
        gamesPlayed++;
        if (currentScore > highScore) {
            highScore = currentScore;
        }
    }

    public String getCurrentLetter() {
        return currentLetter;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int getTotalRounds() {
        return gameConfig.getNumberOfRounds();
    }

    public int getTimeSeconds() {
        return gameConfig.getRoundDurationSeconds();
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void addPoints(int points) {
        this.currentScore += points;
    }

    public GameConfig getGameConfig() {
        return gameConfig;
    }

    // Static methods for settings (for backward compatibility)
    public static int getHighScore() {
        return highScore;
    }

    public static int getGamesPlayed() {
        return gamesPlayed;
    }

    public static void updateHighScore(int score) {
        if (score > highScore) {
            highScore = score;
        }
    }

    public static void incrementGamesPlayed() {
        gamesPlayed++;
    }
}
