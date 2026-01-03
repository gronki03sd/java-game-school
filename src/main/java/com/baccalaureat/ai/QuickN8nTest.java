package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;

/**
 * Quick test to verify N8n webhook AI integration is working.
 * This bypasses all other validators to test the N8n webhook directly.
 */
public class QuickN8nTest {
    
    public static void main(String[] args) {
        System.out.println("🚀 Quick N8n Webhook Test");
        System.out.println("==========================");
        
        try {
            // Create N8n client and validator
            N8nAIClient n8nClient = new N8nAIClient();
            AICategoryValidator aiValidator = new AICategoryValidator(n8nClient, 0.7, true);
            
            // Create test category
            Category fruitCategory = new Category(1, "FRUIT", "Fruit", "🍎", "Name a fruit", true, true, null);
            
            // Test a few words
            String[] testWords = {"apple", "banana", "car", "dog"};
            
            System.out.println("Testing words with N8n webhook...\n");
            
            for (String word : testWords) {
                System.out.printf("Testing: '%s' → ", word);
                
                try {
                    ValidationResult result = aiValidator.validate(word, fruitCategory);
                    
                    String status = result.getStatus().toString();
                    String confidence = String.format("%.1f", result.getConfidence());
                    
                    System.out.printf("%s (confidence: %s)%n", status, confidence);
                    
                    // Show if this came from AI
                    if (result.getSource().contains("AI")) {
                        System.out.println("  ✅ Validated by N8n webhook!");
                    }
                    
                } catch (Exception e) {
                    System.out.printf("ERROR - %s%n", e.getMessage());
                }
            }
            
            System.out.println("\n✨ N8n integration test complete!");
            
        } catch (Exception e) {
            System.err.println("❌ N8n test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}