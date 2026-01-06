package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.service.CategoryService;

import java.util.List;
import java.util.ArrayList;

/**
 * Orchestrates multiple category validators in a specific order.
 * Attempts validation with each validator until a confident result is obtained.
 * Order: FixedList → Web API → Semantic AI
 * 
 * Updated to use WebConfigurableValidator (DictionaryAPI.dev) instead of 
 * the old ConceptNet-based ApiCategoryValidator for cleaner, more reliable validation.
 */
public class CategorizationEngine {
    
    private final List<CategoryValidator> validators;
    private static final double CONFIDENCE_THRESHOLD = 0.7;
    
    public CategorizationEngine(CategoryService categoryService) {
        validators = new ArrayList<>();
        validators.add(new LocalCacheValidator());              // STEP 1: Local database cache
        validators.add(new FixedListValidator());            // STEP 2: Deterministic validation  
        
        // STEP 3: AI validation with N8n webhook (moved before WebAPI as requested)
        try {
            N8nAIClient n8nAIClient = new N8nAIClient();
            AICategoryValidator aiValidator = new AICategoryValidator(n8nAIClient, 0.7, true);
            validators.add(aiValidator);
        } catch (Exception e) {
            System.err.println("[CategorizationEngine] Warning: N8n AI validation disabled - " + e.getMessage());
            // Continue without AI validator if N8n client fails to initialize
        }
        
        validators.add(new WebConfigurableValidator(categoryService)); // STEP 4: Web API validation
    }
    
    /**
     * Custom constructor for testing with specific validators.
     */
    public CategorizationEngine(List<CategoryValidator> customValidators) {
        this.validators = new ArrayList<>(customValidators);
    }
    
    /**
     * Validates a word against a category using the orchestrated pipeline.
     * 
     * @param word the word to validate
     * @param category the target category
     * @return the best ValidationResult from available validators
     */
    public ValidationResult validate(String word, Category category) {
        if (word == null || word.trim().isEmpty()) {
            return new ValidationResult(ValidationStatus.INVALID, 0.0, "ENGINE", "Empty word");
        }
        
        ValidationResult bestResult = new ValidationResult(
            ValidationStatus.INVALID, 0.0, "AI_FALLBACK", "No confident validation available - defaulting to INVALID"
        );
        
        // Try each validator in order until we get a confident result
        // Pipeline: FixedListValidator → WebConfigurableValidator → SemanticAiValidator
        for (CategoryValidator validator : validators) {
            if (!validator.isAvailable()) {
                continue;
            }
            
            try {
                ValidationResult result = validator.validate(word, category);
                
                // Update best result if this one is better
                if (isBetterResult(result, bestResult)) {
                    bestResult = result;
                }
                
                // If we have a confident positive or negative result, stop here
                if (isConfidentResult(result)) {
                    return result;
                }
                
            } catch (Exception e) {
                // Log error and continue with next validator
                System.err.println("Validator " + validator.getSourceName() + " failed: " + e.getMessage());
            }
        }
        
        // AI FINAL RESOLUTION: Ensure no UNCERTAIN results reach UI
        if (bestResult.getStatus() == ValidationStatus.UNCERTAIN) {
            // Force AI to make a final decision
            bestResult = new ValidationResult(
                ValidationStatus.INVALID, 
                0.5, 
                "AI_RESOLVER", 
                "Uncertain result resolved to INVALID by AI fallback"
            );
        }
        
        return bestResult;
    }
    
    /**
     * Determines if a result is confident enough to stop the validation pipeline.
     */
    private boolean isConfidentResult(ValidationResult result) {
        return (result.getStatus() == ValidationStatus.VALID && result.getConfidence() >= CONFIDENCE_THRESHOLD) ||
               (result.getStatus() == ValidationStatus.INVALID && result.getConfidence() >= CONFIDENCE_THRESHOLD);
    }
    
    /**
     * Compares two results to determine which is better.
     */
    private boolean isBetterResult(ValidationResult newResult, ValidationResult currentBest) {
        // Prefer valid over uncertain over invalid
        if (newResult.getStatus() == ValidationStatus.VALID && currentBest.getStatus() != ValidationStatus.VALID) {
            return true;
        }
        if (newResult.getStatus() == ValidationStatus.UNCERTAIN && currentBest.getStatus() == ValidationStatus.INVALID) {
            return true;
        }
        
        // If same status, prefer higher confidence
        if (newResult.getStatus() == currentBest.getStatus()) {
            return newResult.getConfidence() > currentBest.getConfidence();
        }
        
        return false;
    }
    
    /**
     * Returns the current confidence threshold.
     */
    public double getConfidenceThreshold() {
        return CONFIDENCE_THRESHOLD;
    }
    
    /**
     * Returns the list of available validators.
     */
    public List<String> getAvailableValidators() {
        List<String> available = new ArrayList<>();
        for (CategoryValidator validator : validators) {
            if (validator.isAvailable()) {
                available.add(validator.getSourceName());
            }
        }
        return available;
    }
    
    /**
     * Adds a custom validator to the pipeline.
     */
    public void addValidator(CategoryValidator validator) {
        if (validator != null) {
            validators.add(validator);
        }
    }
    
    /**
     * Removes a validator by source name.
     */
    public void removeValidator(String sourceName) {
        validators.removeIf(v -> v.getSourceName().equals(sourceName));
    }
}