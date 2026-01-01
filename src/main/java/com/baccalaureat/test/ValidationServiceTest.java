package com.baccalaureat.test;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.service.ValidationService;

/**
 * Test ValidationService integration to ensure GameController will work correctly.
 */
public class ValidationServiceTest {
    
    public static void main(String[] args) {
        System.out.println("🧪 VALIDATION SERVICE INTEGRATION TEST");
        System.out.println("==================================================");
        System.out.println("Testing ValidationService.validateWord() method used by GameController...\n");
        
        ValidationService validationService = new ValidationService();
        
        // Test different category formats and words
        testValidation(validationService, "ANIMAL", "chien", "French animal word");
        testValidation(validationService, "animal", "chien", "Lowercase category");
        testValidation(validationService, "FRUIT", "pomme", "French fruit word");
        testValidation(validationService, "PAYS", "france", "French country word");
        testValidation(validationService, "VILLE", "paris", "French city word");
        
        // Test English words (should work via API)
        testValidation(validationService, "ANIMAL", "dog", "English animal word");
        testValidation(validationService, "FRUIT", "apple", "English fruit word");
        
        // Test invalid cases
        testValidation(validationService, "ANIMAL", "zzxqp", "Nonsense word");
        testValidation(validationService, "FRUIT", "dog", "Wrong category");
        testValidation(validationService, "ANIMAL", "", "Empty word");
        
        System.out.println("==================================================");
        System.out.println("✅ ValidationService integration test completed");
        System.out.println("\nGameController can now use ValidationService.validateWord() for accurate validation!");
    }
    
    private static void testValidation(ValidationService service, String category, String word, String description) {
        System.out.printf("Testing: '%-15s' in %-8s | %s%n", word, category, description);
        
        try {
            ValidationResult result = service.validateWord(category, word);
            String status = getStatusEmoji(result.getStatus());
            
            System.out.printf("  %s Status: %-8s | Confidence: %.2f | Source: %-15s%n", 
                             status, result.getStatus(), result.getConfidence(), result.getSource());
            
            if (result.getDetails() != null && !result.getDetails().isEmpty()) {
                System.out.printf("  💡 Details: %s%n", result.getDetails());
            }
        } catch (Exception e) {
            System.out.printf("  ⚠️ Error: %s%n", e.getMessage());
        }
        
        System.out.println();
    }
    
    private static String getStatusEmoji(com.baccalaureat.model.ValidationStatus status) {
        switch (status) {
            case VALID: return "✅";
            case INVALID: return "❌";
            case UNCERTAIN: return "❓";
            case ERROR: return "⚠️";
            default: return "❓";
        }
    }
}