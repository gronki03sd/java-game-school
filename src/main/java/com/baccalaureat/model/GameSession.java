package com.baccalaureat.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameSession {
    public enum Difficulty {
        EASY(90, 4, 3),      // 90 seconds, 4 categories, 3 rounds
        MEDIUM(60, 6, 5),    // 60 seconds, 6 categories, 5 rounds
        HARD(45, 8, 7);      // 45 seconds, 8 categories, 7 rounds

        private final int timeSeconds;
        private final int categoryCount;
        private final int totalRounds;

        Difficulty(int timeSeconds, int categoryCount, int totalRounds) {
            this.timeSeconds = timeSeconds;
            this.categoryCount = categoryCount;
            this.totalRounds = totalRounds;
        }

        public int getTimeSeconds() { return timeSeconds; }
        public int getCategoryCount() { return categoryCount; }
        public int getTotalRounds() { return totalRounds; }
    }

    private static Difficulty selectedDifficulty = Difficulty.MEDIUM;
    private static int highScore = 0;
    private static int gamesPlayed = 0;

    private String currentLetter;
    private int currentScore;
    private int currentRound;
    private List<Category> categories;
    private List<String> usedLetters;
    private final Difficulty difficulty;

    public GameSession() {
        this.difficulty = selectedDifficulty;
        this.currentScore = 0;
        this.currentRound = 1;
        this.usedLetters = new ArrayList<>();
        selectCategories();
        generateRandomLetter();
    }

    private void selectCategories() {
        List<Category> allCategories = new ArrayList<>(Arrays.asList(Category.values()));
        Collections.shuffle(allCategories);
        this.categories = allCategories.subList(0, Math.min(difficulty.getCategoryCount(), allCategories.size()));
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
        if (currentRound >= difficulty.getTotalRounds()) {
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
        return difficulty.getTotalRounds();
    }

    public int getTimeSeconds() {
        return difficulty.getTimeSeconds();
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void addPoints(int points) {
        this.currentScore += points;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    // Static methods for settings
    public static void setDifficulty(Difficulty difficulty) {
        selectedDifficulty = difficulty;
    }

    public static Difficulty getSelectedDifficulty() {
        return selectedDifficulty;
    }

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
