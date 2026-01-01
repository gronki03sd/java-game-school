package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;

/**
 * Comprehensive test of the complete validation pipeline.
 * Tests FixedListValidator → ApiCategoryValidator → SemanticAiValidator orchestration.
 */
public class ValidationPipelineTest {
    
    public static void main(String[] args) {
        System.out.println("🔧 VALIDATION PIPELINE TEST");
        System.out.println("==================================================");
        System.out.println("Testing complete validation pipeline with CategorizationEngine...\n");
        
        // Initialize validation engine
        CategorizationEngine engine = new CategorizationEngine();
        
        // Test various scenarios
        testCategory(engine, Category.ANIMAL, new String[]{
            "chien", "chat", "elephant", "lion",    // Should be found by FixedListValidator
            "dog", "cat", "tiger",                  // Might need API/AI validation
            "apple", "car", "invalidword123"       // Should be invalid
        });
        
        testCategory(engine, Category.FRUIT, new String[]{
            "pomme", "banane", "orange", "fraise",  // Should be found by FixedListValidator
            "apple", "mango", "kiwi",               // Might need API/AI validation
            "dog", "house", "invalidword123"        // Should be invalid
        });
        
        testCategory(engine, Category.PAYS, new String[]{
            "france", "allemagne", "espagne",      // Should be found by FixedListValidator
            "germany", "spain", "italy",           // Might need API/AI validation
            "dog", "apple", "invalidword123"       // Should be invalid
        });
        
        testCategory(engine, Category.VILLE, new String[]{
            "paris", "lyon", "marseille",          // Should be found by FixedListValidator
            "london", "berlin", "madrid",          // Might need API/AI validation
            "dog", "apple", "invalidword123"       // Should be invalid
        });
        
        System.out.println("==================================================");
        System.out.println("✅ Validation pipeline test completed");
        System.out.println("\nPipeline order: FixedListValidator → ApiCategoryValidator → SemanticAiValidator");
        System.out.println("Note: API validation results depend on external service availability.");
    }
    
    private static void testCategory(CategorizationEngine engine, Category category, String[] words) {
        System.out.printf("--- Testing Category: %s ---%n", category.name());
        
        for (String word : words) {
            ValidationResult result = engine.validate(word, category);
            String status = getStatusEmoji(result.getStatus());
            String confidence = String.format("%.2f", result.getConfidence());
            
            System.out.printf("  %s %-15s | %-8s | Conf: %s | Source: %-12s", 
                            status, "'" + word + "'", result.getStatus(), confidence, result.getSource());
            
            if (result.getDetails() != null && !result.getDetails().isEmpty()) {
                String details = result.getDetails();
                if (details.length() > 60) {
                    details = details.substring(0, 57) + "...";
                }
                System.out.printf(" | %s", details);
            }
            
            System.out.println();
        }
        
        System.out.println();
    }
    
    private static String getStatusEmoji(ValidationStatus status) {
        switch (status) {
            case VALID: return "✅";
            case INVALID: return "❌";
            case UNCERTAIN: return "❓";
            case ERROR: return "⚠️";
            default: return "❓";
        }
    }
}