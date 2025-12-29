package com.baccalaureat.model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GameSession {
    private String currentLetter;
    private int currentScore;
    private final List<Category> categories = Arrays.asList(
            Category.PAYS,
            Category.VILLE,
            Category.ANIMAL,
            Category.METIER);

    public GameSession() {
        generateRandomLetter();
        currentScore = 0;
    }

    public void generateRandomLetter() {
        Random r = new Random();
        char letter = (char) ('A' + r.nextInt(26));
        this.currentLetter = String.valueOf(letter);
    }

    public String getCurrentLetter() {
        return currentLetter;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void addPoints(int points) {
        this.currentScore += points;
    }
}
