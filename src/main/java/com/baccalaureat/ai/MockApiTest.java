package com.baccalaureat.ai;

import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.model.Category;

/**
 * Mock test demonstrating the category validation logic without depending on external APIs.
 * This shows how the validation system would work when the APIs are available.
 */
public class MockApiTest {
    
    public static void main(String[] args) {
        System.out.println("🧪 MOCK CATEGORY VALIDATION TEST");
        System.out.println("==================================================");
        System.out.println("Demonstrating category-aware validation logic...\n");
        
        // Create a mock validator that simulates ConceptNet responses
        MockApiCategoryValidator mockValidator = new MockApiCategoryValidator();
        
        // Test cases
        testValidation(mockValidator, "dog", Category.ANIMAL, true, "Animal word in animal category");
        testValidation(mockValidator, "elephant", Category.ANIMAL, true, "Animal word in animal category");
        testValidation(mockValidator, "cat", Category.ANIMAL, true, "Animal word in animal category");
        testValidation(mockValidator, "dog", Category.FRUIT, false, "Animal word in fruit category - should be invalid");
        testValidation(mockValidator, "apple", Category.ANIMAL, false, "Fruit word in animal category - should be invalid");
        testValidation(mockValidator, "france", Category.PAYS, true, "Country word in country category");
        testValidation(mockValidator, "germany", Category.PAYS, true, "Country word in country category");
        testValidation(mockValidator, "paris", Category.VILLE, true, "City word in city category");
        testValidation(mockValidator, "london", Category.VILLE, true, "City word in city category");
        testValidation(mockValidator, "apple", Category.FRUIT, true, "Fruit word in fruit category");
        testValidation(mockValidator, "banana", Category.FRUIT, true, "Fruit word in fruit category");
        testValidation(mockValidator, "", Category.ANIMAL, false, "Empty string test");
        testValidation(mockValidator, "nonexistentword123", Category.ANIMAL, false, "Non-existent word test");
        
        System.out.println("==================================================");
        System.out.println("✅ Mock category validation test completed");
        System.out.println("\nThis demonstrates the expected behavior when ConceptNet API is available.");
    }
    
    private static void testValidation(MockApiCategoryValidator validator, String word, Category category, 
                                     boolean expectedValid, String description) {
        System.out.printf("Testing: '%-15s' in %-8s | %s%n", word, category.name(), description);
        
        ValidationResult result = validator.validate(word, category);
        String status = getStatusEmoji(result.getStatus());
        
        System.out.printf("  %s Status: %-8s | Confidence: %.2f | Source: %-10s%n", 
                         status, result.getStatus(), result.getConfidence(), result.getSource());
        
        if (result.getDetails() != null && !result.getDetails().isEmpty()) {
            System.out.printf("  💡 Details: %s%n", result.getDetails());
        }
        
        System.out.println();
    }
    
    private static String getStatusEmoji(ValidationStatus status) {
        switch (status) {
            case VALID: return "✅";
            case INVALID: return "❌";
            case ERROR: return "⚠️";
            default: return "❓";
        }
    }
}

/**
 * Mock implementation that simulates ConceptNet responses for demonstration purposes.
 */
class MockApiCategoryValidator implements CategoryValidator {
    
    @Override
    public ValidationResult validate(String word, Category category) {
        if (word == null || word.trim().isEmpty()) {
            return new ValidationResult(
                ValidationStatus.INVALID,
                0.0,
                getSourceName(),
                "Empty word"
            );
        }
        
        String normalizedWord = word.toLowerCase().trim();
        
        // Mock category mappings (simulating what ConceptNet would return)
        boolean isValid = false;
        double confidence = 0.0;
        String explanation = "";
        
        switch (category) {
            case ANIMAL:
                if (normalizedWord.equals("dog") || normalizedWord.equals("cat") || normalizedWord.equals("elephant")) {
                    isValid = true;
                    confidence = 0.95;
                    explanation = "Word identified as animal through ConceptNet relationships";
                } else if (normalizedWord.equals("apple") || normalizedWord.equals("banana")) {
                    isValid = false;
                    confidence = 0.0;
                    explanation = "Word identified as fruit, not animal";
                }
                break;
                
            case FRUIT:
                if (normalizedWord.equals("apple") || normalizedWord.equals("banana")) {
                    isValid = true;
                    confidence = 0.90;
                    explanation = "Word identified as fruit through ConceptNet relationships";
                } else if (normalizedWord.equals("dog") || normalizedWord.equals("cat")) {
                    isValid = false;
                    confidence = 0.0;
                    explanation = "Word identified as animal, not fruit";
                }
                break;
                
            case PAYS:
                if (normalizedWord.equals("france") || normalizedWord.equals("germany")) {
                    isValid = true;
                    confidence = 0.92;
                    explanation = "Word identified as country through ConceptNet relationships";
                }
                break;
                
            case VILLE:
                if (normalizedWord.equals("paris") || normalizedWord.equals("london")) {
                    isValid = true;
                    confidence = 0.88;
                    explanation = "Word identified as city through ConceptNet relationships";
                }
                break;
                
            default:
                // Handle other categories
                break;
        }
        
        // Handle unknown words
        if (!isValid && explanation.isEmpty()) {
            if (normalizedWord.equals("nonexistentword123")) {
                return new ValidationResult(
                    ValidationStatus.INVALID,
                    0.0,
                    getSourceName(),
                    "Word not found in ConceptNet knowledge base"
                );
            } else {
                explanation = "No matching category relationships found";
            }
        }
        
        ValidationStatus status = isValid ? ValidationStatus.VALID : ValidationStatus.INVALID;
        
        return new ValidationResult(
            status,
            confidence,
            getSourceName(),
            explanation
        );
    }
    
    @Override
    public String getSourceName() {
        return "MOCK_API";
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
}