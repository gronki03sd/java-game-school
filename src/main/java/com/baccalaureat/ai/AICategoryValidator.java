package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;

import java.text.Normalizer;

/**
 * AI-powered category validator that uses machine learning models
 * to determine if a word belongs to a specific category.
 * 
 * This validator queries an AI service with natural language questions
 * and interprets the confidence levels to make validation decisions.
 */
public class AICategoryValidator implements CategoryValidator {
    
    private final AIClient aiClient;
    private final double confidenceThreshold;
    private final boolean normalizeInput;
    
    /**
     * Create an AI category validator with default settings.
     * Uses confidence threshold of 0.7 and input normalization enabled.
     * 
     * @param aiClient the AI client implementation to use
     */
    public AICategoryValidator(AIClient aiClient) {
        this(aiClient, 0.7, true);
    }
    
    /**
     * Create an AI category validator with custom settings.
     * 
     * @param aiClient the AI client implementation to use
     * @param confidenceThreshold minimum confidence for VALID result (0.0-1.0)
     * @param normalizeInput whether to normalize input before querying AI
     */
    public AICategoryValidator(AIClient aiClient, double confidenceThreshold, boolean normalizeInput) {
        if (aiClient == null) {
            throw new IllegalArgumentException("AIClient cannot be null");
        }
        if (confidenceThreshold < 0.0 || confidenceThreshold > 1.0) {
            throw new IllegalArgumentException("Confidence threshold must be between 0.0 and 1.0");
        }
        
        this.aiClient = aiClient;
        this.confidenceThreshold = confidenceThreshold;
        this.normalizeInput = normalizeInput;
    }
    
    @Override
    public ValidationResult validate(String word, Category category) {
        if (word == null || word.trim().isEmpty()) {
            return new ValidationResult(
                ValidationStatus.INVALID,
                0.0,
                getSourceName(),
                "Empty word cannot be validated"
            );
        }
        
        if (category == null) {
            return new ValidationResult(
                ValidationStatus.ERROR,
                0.0,
                getSourceName(),
                "Category cannot be null"
            );
        }
        
        // Check if AI client is healthy before proceeding
        if (!aiClient.isHealthy()) {
            return new ValidationResult(
                ValidationStatus.UNCERTAIN,
                0.0,
                getSourceName(),
                "AI client is not available: " + aiClient.getClientName()
            );
        }
        
        try {
            // Normalize input if enabled
            String normalizedWord = normalizeInput ? normalizeInput(word) : word.trim();
            
            // Construct the AI prompt
            String prompt = buildPrompt(normalizedWord, category);
            
            // Query the AI
            AIResponse aiResponse = aiClient.query(prompt);
            
            // Interpret the response
            return interpretAIResponse(aiResponse, normalizedWord, category);
            
        } catch (AIClientException e) {
            // Log the error for debugging (in real implementation, use proper logging)
            System.err.println("[AICategoryValidator] AI query failed: " + e.toString());
            
            // Return UNCERTAIN as fail-safe
            return new ValidationResult(
                ValidationStatus.UNCERTAIN,
                0.0,
                getSourceName(),
                "AI validation failed: " + e.getMessage()
            );
        }
    }
    
    @Override
    public String getSourceName() {
        return "AI_" + aiClient.getClientName().toUpperCase();
    }
    
    @Override
    public boolean isAvailable() {
        return aiClient != null && aiClient.isHealthy();
    }
    
    /**
     * Build the natural language prompt for the AI model.
     * Updated for n8n webhook format - simple question format.
     * 
     * @param word the normalized word to validate
     * @param category the category to validate against
     * @return formatted prompt string
     */
    private String buildPrompt(String word, Category category) {
        return String.format("Is '%s' a valid example of the category '%s'?", word, category.getDisplayName());
    }
    
    /**
     * Normalize input text by trimming, converting to lowercase, and removing accents.
     * 
     * @param input the raw input string
     * @return normalized string
     */
    private String normalizeInput(String input) {
        if (input == null) {
            return "";
        }
        
        // Trim whitespace and convert to lowercase
        String normalized = input.trim().toLowerCase();
        
        // Remove accents/diacritics
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        
        return normalized;
    }
    
    /**
     * Interpret the AI response and convert it to a ValidationResult.
     * 
     * @param aiResponse the response from the AI client
     * @param word the word that was validated
     * @param category the category that was tested
     * @return ValidationResult based on AI confidence and threshold
     */
    private ValidationResult interpretAIResponse(AIResponse aiResponse, String word, Category category) {
        double confidence = aiResponse.getConfidence();
        boolean aiSaysValid = aiResponse.isValid();
        
        ValidationStatus status;
        String message;
        
        if (confidence >= confidenceThreshold) {
            // High confidence - trust the AI's judgment
            status = aiSaysValid ? ValidationStatus.VALID : ValidationStatus.INVALID;
            message = String.format(
                "AI validation (%s): %s with %.1f%% confidence",
                aiClient.getClientName(),
                aiSaysValid ? "valid" : "invalid",
                confidence * 100
            );
        } else {
            // Low confidence - mark as uncertain
            status = ValidationStatus.UNCERTAIN;
            message = String.format(
                "AI validation (%s): uncertain (confidence %.1f%% below threshold %.1f%%)",
                aiClient.getClientName(),
                confidence * 100,
                confidenceThreshold * 100
            );
        }
        
        // Include AI reasoning if available
        if (aiResponse.getReasoning() != null && !aiResponse.getReasoning().trim().isEmpty()) {
            message += " - " + aiResponse.getReasoning();
        }
        
        return new ValidationResult(status, confidence, getSourceName(), message);
    }
    
    /**
     * Get the AI client used by this validator.
     * Useful for testing and debugging.
     * 
     * @return the AIClient instance
     */
    public AIClient getAIClient() {
        return aiClient;
    }
    
    /**
     * Get the confidence threshold used by this validator.
     * 
     * @return confidence threshold (0.0-1.0)
     */
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    /**
     * Check if input normalization is enabled.
     * 
     * @return true if inputs are normalized before AI queries
     */
    public boolean isNormalizationEnabled() {
        return normalizeInput;
    }
}