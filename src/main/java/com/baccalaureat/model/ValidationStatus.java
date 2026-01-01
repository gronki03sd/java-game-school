package com.baccalaureat.model;

/**
 * Enumeration of possible validation outcomes.
 */
public enum ValidationStatus {
    /**
     * The word is definitively valid for the category.
     */
    VALID,
    
    /**
     * The word is definitively invalid for the category.
     */
    INVALID,
    
    /**
     * The validation result is uncertain or inconclusive.
     * May require human verification or additional validation attempts.
     */
    UNCERTAIN,
    
    /**
     * An error occurred during validation (network, API, etc.).
     */
    ERROR
}
