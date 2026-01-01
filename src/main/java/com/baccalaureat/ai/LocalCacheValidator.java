package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.service.CacheService;

/**
 * LocalCacheValidator - STEP 1 of validation pipeline.
 * Provides instant validation for previously validated words.
 * 
 * Behavior:
 * - Cache hit: Returns VALID
 * - Cache miss: Returns UNCERTAIN (never rejects)
 */
public class LocalCacheValidator implements CategoryValidator {
    
    private final CacheService cacheService;
    
    public LocalCacheValidator() {
        this.cacheService = new CacheService();
    }
    
    /**
     * Constructor for dependency injection.
     */
    public LocalCacheValidator(CacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    @Override
    public ValidationResult validate(String word, Category category) {
        if (word == null || word.trim().isEmpty()) {
            return new ValidationResult(
                ValidationStatus.UNCERTAIN, 
                0.0, 
                "LOCAL_DB", 
                "Empty word - skipping cache"
            );
        }
        
        // Check cache
        boolean isValidated = cacheService.isWordValidated(word, category);
        
        if (isValidated) {
            // Cache hit - instant validation
            return new ValidationResult(
                ValidationStatus.VALID,
                0.90,
                "LOCAL_DB",
                "Previously validated word (local cache)"
            );
        } else {
            // Cache miss - let other validators decide
            return new ValidationResult(
                ValidationStatus.UNCERTAIN,
                0.0,
                "LOCAL_DB",
                "Word not found in cache"
            );
        }
    }
    
    @Override
    public boolean isAvailable() {
        // Always available (no external dependencies)
        return true;
    }
    
    @Override
    public String getSourceName() {
        return "LOCAL_DB";
    }
}