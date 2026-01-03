package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.service.ValidationService;
import com.baccalaureat.service.CategoryService;

/**
 * Integration test to verify that the N8n webhook AI client is working properly
 * in the production validation pipeline.
 * 
 * This test demonstrates real AI validation with N8n webhook.
 * 
 * SETUP REQUIRED:
 * Ensure the N8n webhook at https://kirrimimi.app.n8n.cloud/webhook/chat is accessible
 */
public class AIIntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("🤖 AI INTEGRATION TEST");
        System.out.println("======================");
        System.out.println("Testing N8n webhook AI integration in validation pipeline...\n");
        
        // No API key needed for N8n webhook
        System.out.println("✅ N8n webhook - no API key required");
        
        try {
            // Initialize validation services 
            ValidationService validationService = new ValidationService();
            CategoryService categoryService = new CategoryService();
            
            // Get categories for testing
            Category fruitCategory = categoryService.findByName("FRUIT").orElse(null);
            Category animalCategory = categoryService.findByName("ANIMAL").orElse(null);
            
            if (fruitCategory == null || animalCategory == null) {
                System.err.println("❌ ERROR: Required categories not found. Make sure database is initialized.");
                return;
            }
            
            System.out.println("✅ Validation services initialized");
            System.out.println("✅ Categories loaded: FRUIT, ANIMAL\n");
            
            // === TEST 1: Clear positive cases that should be found by earlier validators ===
            System.out.println("=== TEST 1: Testing words likely validated by FixedList/Web validators ===");
            testWord(validationService, fruitCategory, "pomme", "French for apple - should be VALID");
            testWord(validationService, animalCategory, "chien", "French for dog - should be VALID");
            
            // === TEST 2: Edge cases that specifically require AI validation ===
            System.out.println("\n=== TEST 2: Testing edge cases requiring N8n AI validation ===");
            // These are specifically chosen to likely reach the AI validator
            testWord(validationService, fruitCategory, "tomato", "Botanically fruit, culinarily vegetable - N8n decides");
            testWord(validationService, fruitCategory, "rhubarb", "Vegetable used like fruit - tests N8n reasoning");
            testWord(validationService, animalCategory, "seahorse", "Unusual animal - tests N8n knowledge");
            
            // === TEST 3: Clear negative cases ===
            System.out.println("\n=== TEST 3: Testing clear negative cases ===");
            testWord(validationService, fruitCategory, "automobile", "Vehicle - should be INVALID");
            testWord(validationService, animalCategory, "television", "Electronic device - should be INVALID");
            
            // === TEST 4: Non-existent/gibberish words ===
            System.out.println("\n=== TEST 4: Testing non-existent words ===");
            testWord(validationService, fruitCategory, "xyzabc123", "Gibberish - should be INVALID");
            
            System.out.println("\n=== AI INTEGRATION TEST COMPLETE ===");
            System.out.println("🎯 If you see 'Source: AI' in any results above, the N8n webhook integration is working!");
            
        } catch (Exception e) {
            System.err.println("❌ Error during N8n AI integration test: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Tests a single word validation and prints detailed results.
     * This method specifically shows which validator was used and highlights AI results.
     */
    private static void testWord(ValidationService validationService, Category category, String word, String description) {
        try {
            System.out.printf("Testing: '%-12s' in %-6s | %s%n", word, category.getName(), description);
            
            // === THIS LINE CALLS THE FULL VALIDATION PIPELINE INCLUDING AI ===
            ValidationResult result = validationService.validateWord(category.getName(), word);
            
            // Get status icon for visual feedback
            String statusIcon = getStatusIcon(result.getStatus());
            
            // === THESE LINES PRINT THE AI VALIDATION RESULTS ===
            System.out.printf("  Result: %s %-9s | Confidence: %.2f | Source: %-15s%n", 
                            statusIcon, 
                            result.getStatus(), 
                            result.getConfidence(), 
                            result.getSource());
            
            // === HIGHLIGHT AI-POWERED VALIDATIONS ===
            if (result.getSource().contains("AI")) {
                System.out.println("  🤖 VALIDATED BY AI! 🤖");
                
                // === PRINT AI REASONING IF AVAILABLE ===
                if (result.getDetails() != null && !result.getDetails().isEmpty()) {
                    String reasoning = result.getDetails();
                    // Truncate long AI responses for readability
                    if (reasoning.length() > 100) {
                        reasoning = reasoning.substring(0, 97) + "...";
                    }
                    System.out.printf("  💭 AI Reasoning: %s%n", reasoning);
                }
            }
            
            // === ANALYZE CONFIDENCE LEVELS ===
            if (result.getConfidence() >= 0.7) {
                System.out.println("  ✅ High confidence result");
            } else if (result.getConfidence() >= 0.4) {
                System.out.println("  ⚠️  Medium confidence - might need review");
            } else {
                System.out.println("  🤔 Low confidence - uncertain result");
            }
            
        } catch (Exception e) {
            // === GRACEFUL ERROR HANDLING FOR AI FAILURES ===
            System.err.printf("  ❌ ERROR validating '%s': %s%n", word, e.getMessage());
            
            // Print more detailed error information
            System.err.println("     Error type: " + e.getClass().getSimpleName());
            
            if (e instanceof com.baccalaureat.ai.AIClientException) {
                com.baccalaureat.ai.AIClientException ace = (com.baccalaureat.ai.AIClientException) e;
                System.err.println("     AI Error Type: " + ace.getErrorType());
                System.err.println("     AI Client: " + ace.getClientName());
            }
            
            // Check for common issues
            if (e.getMessage().contains("network") || e.getMessage().contains("timeout")) {
                System.err.println("   → Check your internet connection and N8n webhook availability");
            } else if (e.getMessage().contains("webhook") || e.getMessage().contains("n8n")) {
                System.err.println("   → Check N8n webhook status at https://kirrimimi.app.n8n.cloud/webhook/chat");
            }
        }
        
        System.out.println(); // Add spacing between tests
    }
    
    /**
     * Returns appropriate emoji for validation status
     */
    private static String getStatusIcon(ValidationStatus status) {
        return switch (status) {
            case VALID -> "✅";
            case INVALID -> "❌"; 
            case UNCERTAIN -> "🤔";
            case ERROR -> "⚠️";
        };
    }
}