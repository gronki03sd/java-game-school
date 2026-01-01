package com.baccalaureat.service;

import com.baccalaureat.dao.DatabaseManager;
import com.baccalaureat.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;

/**
 * CacheService handles local database operations for validated words.
 * Provides database access only - no validation logic.
 */
public class CacheService {
    
    private static final String CHECK_QUERY = 
        "SELECT 1 FROM validated_words WHERE word = ? AND category = ? LIMIT 1";
    
    private static final String INSERT_QUERY = 
        "INSERT OR IGNORE INTO validated_words (word, category) VALUES (?, ?)";
    
    /**
     * Checks if a word has been previously validated for a category.
     * 
     * @param word the word to check (will be normalized)
     * @param category the target category
     * @return true if word+category exists in cache
     */
    public boolean isWordValidated(String word, Category category) {
        String normalizedWord = normalizeInput(word);
        String categoryName = category.name();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(CHECK_QUERY)) {
            
            stmt.setString(1, normalizedWord);
            stmt.setString(2, categoryName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("Database error checking cached word: " + e.getMessage());
            return false; // Fail safe
        }
    }
    
    /**
     * Saves a validated word to cache.
     * 
     * @param word the validated word (will be normalized)
     * @param category the category it was validated for
     */
    public void saveValidatedWord(String word, Category category) {
        String normalizedWord = normalizeInput(word);
        String categoryName = category.name();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_QUERY)) {
            
            stmt.setString(1, normalizedWord);
            stmt.setString(2, categoryName);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Database error saving validated word: " + e.getMessage());
            // Non-fatal - continue without caching
        }
    }
    
    /**
     * Normalizes input for consistent storage and lookup.
     * - Lowercase
     * - Trim whitespace  
     * - Remove accents
     */
    private String normalizeInput(String input) {
        if (input == null) {
            return "";
        }
        
        // Trim and lowercase
        String normalized = input.trim().toLowerCase();
        
        // Remove accents
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        return normalized;
    }
}