package com.baccalaureat.model;

import java.util.HashMap;
import java.util.Map;

public class Player {
    private final String name;
    private int score;
    private int roundScore;
    private final Map<Category, String> currentAnswers;
    private boolean hasFinished;

    public Player(String name) {
        this.name = name;
        this.score = 0;
        this.roundScore = 0;
        this.currentAnswers = new HashMap<>();
        this.hasFinished = false;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public int getRoundScore() {
        return roundScore;
    }

    public void addPoints(int points) {
        this.score += points;
        this.roundScore = points;
    }

    public void resetRoundScore() {
        this.roundScore = 0;
    }

    public Map<Category, String> getCurrentAnswers() {
        return currentAnswers;
    }

    public void setAnswer(Category category, String answer) {
        currentAnswers.put(category, answer);
    }

    public void clearAnswers() {
        currentAnswers.clear();
    }

    public boolean hasFinished() {
        return hasFinished;
    }

    public void setFinished(boolean finished) {
        this.hasFinished = finished;
    }

    public void resetForNewRound() {
        currentAnswers.clear();
        hasFinished = false;
        roundScore = 0;
    }

    public void resetForNewGame() {
        score = 0;
        roundScore = 0;
        currentAnswers.clear();
        hasFinished = false;
    }

    @Override
    public String toString() {
        return name;
    }
}
