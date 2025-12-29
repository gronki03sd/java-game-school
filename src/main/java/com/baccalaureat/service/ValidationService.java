package com.baccalaureat.service;

import com.baccalaureat.dao.WordDAO;

public class ValidationService {
    private final WordDAO wordDAO = new WordDAO();

    /**
     * Validates a word for a category using local cache then a mock external API.
     * If valid externally, caches it locally.
     */
    public boolean validateWord(String category, String word) {
        if (word == null || word.trim().isEmpty())
            return false;

        // Step 1: Check local DB cache
        if (wordDAO.isWordInLocalDb(category, word)) {
            return true;
        }

        // Step 2: Mock external API validation
        boolean externalValid = mockExternalApiValidate(category, word);

        // Step 3: Cache if valid
        if (externalValid) {
            wordDAO.saveWord(category, word);
            return true;
        }
        return false;
    }

    /**
     * Mock external API: valid if word length > 2 (trimmed)
     */
    private boolean mockExternalApiValidate(String category, String word) {
        return word != null && word.trim().length() > 2;
    }
}
