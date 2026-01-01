package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;

/**
 * Interface for category validation engines.
 * Each implementation provides a different validation strategy.
 */
public interface CategoryValidator {
    
    /**
     * Validates whether a word belongs to the given category.
     * 
     * @param word the word to validate
     * @param category the target category
     * @return ValidationResult containing status, confidence, and source
     */
    ValidationResult validate(String word, Category category);
    
    /**
     * Returns the validation source name for traceability.
     * 
     * @return source name (e.g., "FIXED_LIST", "API", "AI")
     */
    String getSourceName();
    
    /**
     * Indicates if this validator is currently available/enabled.
     * 
     * @return true if validator is ready to use
     */
    boolean isAvailable();
}