package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.service.ValidationService;

/**
 * Example demonstrating the new N8n webhook-based AI validation.
 * This replaces the previous OpenAI GPT integration with a simple HTTP webhook approach.
 */
public class N8nValidationExample {
    
    public static void main(String[] args) {
        System.out.println("🎯 N8n Webhook AI Validation Example");
        System.out.println("=====================================");
        System.out.println("Testing deterministic AI validation via n8n webhook...\n");
        
        try {
            // === Step 1: Test direct N8n client ===
            System.out.println("=== DIRECT N8N CLIENT TEST ===");
            N8nAIClient n8nClient = new N8nAIClient();
            System.out.println("✅ N8n client created");
            System.out.println("   Client name: " + n8nClient.getClientName());
            System.out.println("   Client healthy: " + n8nClient.isHealthy());
            
            // Test direct webhook call
            Category testCategory = new Category(1, "FRUIT", "Fruit", "🍎", "Name a fruit", true, true, null);
            String testPrompt = "Is 'apple' a valid example of the category 'Fruit'?";
            
            System.out.println("\n🌐 Testing direct webhook call...");
            System.out.println("   Prompt: " + testPrompt);
            
            AIResponse directResponse = n8nClient.query(testPrompt);
            
            System.out.println("✅ N8n webhook response received:");
            System.out.println("   Valid: " + directResponse.isValid());
            System.out.println("   Confidence: " + directResponse.getConfidence());
            System.out.println("   Reasoning: " + directResponse.getReasoning());
            
            // === Step 2: Test via AICategoryValidator ===
            System.out.println("\n=== AI CATEGORY VALIDATOR TEST ===");
            AICategoryValidator aiValidator = new AICategoryValidator(n8nClient, 0.7, true);
            
            ValidationResult validatorResult = aiValidator.validate("apple", testCategory);
            
            System.out.println("✅ AICategoryValidator result:");
            System.out.println("   Status: " + validatorResult.getStatus());
            System.out.println("   Confidence: " + validatorResult.getConfidence());
            System.out.println("   Source: " + validatorResult.getSource());
            System.out.println("   Details: " + validatorResult.getDetails());
            
            // === Step 3: Test via full validation pipeline ===
            System.out.println("\n=== FULL VALIDATION PIPELINE TEST ===");
            ValidationService validationService = new ValidationService();
            
            // Test multiple words to show the n8n integration in action
            String[] testWords = {"pomme", "banana", "car", "elephant"};
            
            for (String word : testWords) {
                System.out.printf("\nTesting: '%s' in FRUIT category%n", word);
                
                // === THIS USES THE FULL PIPELINE: FixedList → N8n AI → WebAPI ===
                ValidationResult result = validationService.validateWord("FRUIT", word);
                
                String statusIcon = getStatusIcon(result.getStatus());
                System.out.printf("  Result: %s %-9s | Confidence: %.2f | Source: %-15s%n", 
                                statusIcon, result.getStatus(), result.getConfidence(), result.getSource());
                
                // Highlight when N8n AI was used
                if (result.getSource().contains("AI")) {
                    System.out.println("  🎯 VALIDATED BY N8N WEBHOOK! 🎯");
                    System.out.printf("  💭 Details: %s%n", result.getDetails());
                }
            }
            
            System.out.println("\n=== N8N AI VALIDATION COMPLETE ===");
            System.out.println("🎉 N8n webhook integration is working!");
            System.out.println("   • Deterministic true/false responses (confidence 1.0)");
            System.out.println("   • No API keys required");
            System.out.println("   • Simple HTTP POST to webhook");
            System.out.println("   • Fast and reliable validation");
            
        } catch (AIClientException e) {
            System.err.println("❌ N8n AI validation error:");
            System.err.println("   Error type: " + e.getErrorType());
            System.err.println("   Message: " + e.getMessage());
            
            switch (e.getErrorType()) {
                case NETWORK_ERROR:
                    System.err.println("💡 Check internet connection and n8n webhook URL");
                    break;
                case TIMEOUT:
                    System.err.println("💡 N8n webhook took too long to respond");
                    break;
                case API_ERROR:
                    System.err.println("💡 N8n webhook returned an error");
                    break;
                default:
                    System.err.println("💡 Check n8n workflow status");
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
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