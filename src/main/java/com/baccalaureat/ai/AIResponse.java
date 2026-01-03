package com.baccalaureat.ai;

/**
 * Response object from AI client queries.
 * Contains the validation result and confidence level.
 */
public class AIResponse {
    
    private final boolean valid;
    private final double confidence;
    private final String reasoning;
    
    /**
     * Create an AI response with validation result and confidence.
     * 
     * @param valid true if the word is valid for the category
     * @param confidence confidence level between 0.0 and 1.0
     */
    public AIResponse(boolean valid, double confidence) {
        this(valid, confidence, null);
    }
    
    /**
     * Create an AI response with validation result, confidence, and reasoning.
     * 
     * @param valid true if the word is valid for the category
     * @param confidence confidence level between 0.0 and 1.0
     * @param reasoning optional explanation from the AI
     */
    public AIResponse(boolean valid, double confidence, String reasoning) {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0, got: " + confidence);
        }
        
        this.valid = valid;
        this.confidence = confidence;
        this.reasoning = reasoning;
    }
    
    /**
     * @return true if the AI considers the word valid for the category
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * @return confidence level between 0.0 (no confidence) and 1.0 (completely confident)
     */
    public double getConfidence() {
        return confidence;
    }
    
    /**
     * @return optional reasoning/explanation from the AI, may be null
     */
    public String getReasoning() {
        return reasoning;
    }
    
    @Override
    public String toString() {
        return String.format("AIResponse{valid=%s, confidence=%.2f%s}", 
            valid, confidence, reasoning != null ? ", reasoning='" + reasoning + "'" : "");
    }
}