package com.baccalaureat.service;

import com.baccalaureat.ai.CategorizationEngine;
import com.baccalaureat.dao.WordDAO;
import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;

/**
 * Service layer for word validation.
 * Coordinates validation flow but delegates categorization logic to AI pipeline.
 * Responsibilities: input normalization, caching, orchestration.
 */
public class ValidationService {
    private final WordDAO wordDAO = new WordDAO();
    private final CategorizationEngine categorizationEngine = new CategorizationEngine();
    
    /**
     * Validates a word for a category using the AI-ready validation pipeline.
     * First checks local cache, then delegates to categorization engine.
     * Caches positive results for future lookups.
     * 
     * @param category the target category
     * @param word the word to validate
     * @return ValidationResult with status, confidence, and source information
     */
    public ValidationResult validateWord(String category, String word) {
        // Input validation
        if (word == null || word.trim().isEmpty()) {
            return new ValidationResult(ValidationStatus.INVALID, 0.0, "SERVICE", "Empty word");
        }
        
        if (category == null) {
            return new ValidationResult(ValidationStatus.ERROR, 0.0, "SERVICE", "Category is null");
        }
        
        // Normalize input
        String normalizedWord = normalizeInput(word);
        String normalizedCategory = normalizeInput(category);
        
        // Step 1: Check local database cache first
        if (wordDAO.isWordInLocalDb(normalizedCategory, normalizedWord)) {
            return new ValidationResult(ValidationStatus.VALID, 1.0, "DATABASE_CACHE", "Found in local cache");
        }
        
        // Step 2: Parse category enum (if possible)
        Category categoryEnum = parseCategory(normalizedCategory);
        if (categoryEnum == null) {
            return new ValidationResult(ValidationStatus.ERROR, 0.0, "SERVICE", "Unknown category: " + category);
        }
        
        // Step 3: Delegate to categorization engine
        ValidationResult result = categorizationEngine.validate(normalizedWord, categoryEnum);
        
        // Step 4: Cache positive results (only deterministic valid results)
        if (result.isValid() && result.getConfidence() == 1.0) {
            wordDAO.saveWord(normalizedCategory, normalizedWord);
        }
        
        return result;
    }
    
    /**
     * Legacy method for backward compatibility.
     * Returns simple boolean based on validation result.
     */
    public boolean validateWordBoolean(String category, String word) {
        ValidationResult result = validateWord(category, word);
        return result.isValid();
    }
    
    /**
     * Normalizes input text for consistent processing.
     * 
     * @param input the input text
     * @return normalized text
     */
    private String normalizeInput(String input) {
        if (input == null) return "";
        
        return input.trim()
                   .toLowerCase()
                   // Remove extra whitespace
                   .replaceAll("\\s+", " ")
                   // Basic accent removal for French
                   .replace("à", "a")
                   .replace("é", "e")
                   .replace("è", "e")
                   .replace("ê", "e")
                   .replace("ë", "e")
                   .replace("î", "i")
                   .replace("ï", "i")
                   .replace("ô", "o")
                   .replace("ö", "o")
                   .replace("ù", "u")
                   .replace("û", "u")
                   .replace("ü", "u")
                   .replace("ÿ", "y")
                   .replace("ç", "c");
    }
    
    /**
     * Attempts to parse a category string to the Category enum.
     * 
     * @param categoryStr the category string
     * @return Category enum or null if not found
     */
    private Category parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            return null;
        }
        
        // Try exact match first
        for (Category category : Category.values()) {
            if (category.name().toLowerCase().equals(categoryStr) ||
                category.displayName().toLowerCase().equals(categoryStr)) {
                return category;
            }
        }
        
        // Try partial matches
        for (Category category : Category.values()) {
            if (category.displayName().toLowerCase().contains(categoryStr) ||
                categoryStr.contains(category.displayName().toLowerCase())) {
                return category;
            }
        }
        
        return null;
    }
    
    /**
     * Gets the underlying categorization engine for advanced operations.
     * 
     * @return the categorization engine instance
     */
    public CategorizationEngine getCategorizationEngine() {
        return categorizationEngine;
    }
    
    /**
     * Clears the local word cache.
     * Useful for testing or when cache becomes stale.
     */
    public void clearCache() {
        // This would typically clear the database cache
        // For now, we rely on database operations
        System.out.println("Cache clear requested - implement database cache clearing if needed");
    }
    
    /**
     * Gets statistics about validation performance.
     * 
     * @return validation statistics summary
     */
    public String getValidationStats() {
        return String.format("Available validators: %s, Confidence threshold: %.2f",
                           categorizationEngine.getAvailableValidators(),
                           categorizationEngine.getConfidenceThreshold());
    }
}
