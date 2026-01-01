package com.baccalaureat.model;

/**
 * Encapsulates the result of word validation against a category.
 * Contains status, confidence score, source validator, and optional details.
 */
public class ValidationResult {
    private final ValidationStatus status;
    private final double confidence;
    private final String source;
    private final String details;
    
    /**
     * Creates a validation result with all information.
     * 
     * @param status the validation outcome (VALID, INVALID, UNCERTAIN, ERROR)
     * @param confidence confidence score between 0.0 and 1.0
     * @param source the validator source (FIXED_LIST, API, AI, etc.)
     * @param details additional information about the validation
     */
    public ValidationResult(ValidationStatus status, double confidence, String source, String details) {
        this.status = status;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to [0.0, 1.0]
        this.source = source != null ? source : "UNKNOWN";
        this.details = details != null ? details : "";
    }
    
    /**
     * Creates a validation result with minimal information.
     * 
     * @param status the validation outcome
     * @param confidence confidence score between 0.0 and 1.0
     * @param source the validator source
     */
    public ValidationResult(ValidationStatus status, double confidence, String source) {
        this(status, confidence, source, "");
    }
    
    /**
     * Gets the validation status.
     */
    public ValidationStatus getStatus() {
        return status;
    }
    
    /**
     * Gets the confidence score (0.0 to 1.0).
     * Higher values indicate more confidence in the validation result.
     */
    public double getConfidence() {
        return confidence;
    }
    
    /**
     * Gets the source validator name (e.g., FIXED_LIST, API, AI).
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Gets additional details about the validation.
     */
    public String getDetails() {
        return details;
    }
    
    /**
     * Checks if the result indicates a valid word.
     */
    public boolean isValid() {
        return status == ValidationStatus.VALID;
    }
    
    /**
     * Checks if the result indicates an invalid word.
     */
    public boolean isInvalid() {
        return status == ValidationStatus.INVALID;
    }
    
    /**
     * Checks if the result is uncertain or inconclusive.
     */
    public boolean isUncertain() {
        return status == ValidationStatus.UNCERTAIN;
    }
    
    /**
     * Checks if there was an error during validation.
     */
    public boolean hasError() {
        return status == ValidationStatus.ERROR;
    }
    
    /**
     * Checks if the confidence meets a given threshold.
     */
    public boolean isConfident(double threshold) {
        return confidence >= threshold;
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult{status=%s, confidence=%.2f, source=%s, details='%s'}", 
                           status, confidence, source, details);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ValidationResult that = (ValidationResult) obj;
        return Double.compare(that.confidence, confidence) == 0 &&
               status == that.status &&
               source.equals(that.source) &&
               details.equals(that.details);
    }
    
    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + Double.hashCode(confidence);
        result = 31 * result + source.hashCode();
        result = 31 * result + details.hashCode();
        return result;
    }
}
