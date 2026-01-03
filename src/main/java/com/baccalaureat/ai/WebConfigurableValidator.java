package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.service.HttpClientService;
import com.baccalaureat.service.CategoryService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * WebConfigurableValidator - Clean web API validator using DictionaryAPI.dev
 * 
 * This validator replaces the old ConceptNet implementation with a simpler,
 * more reliable approach using DictionaryAPI.dev for word existence validation
 * combined with category-specific keyword matching.
 * 
 * Features:
 * - Uses free DictionaryAPI.dev (no API key required)
 * - Category-aware validation through definition analysis
 * - Graceful error handling with UNCERTAIN status
 * - Clean ValidationResult with proper confidence scoring
 * - Configurable API endpoints and timeouts
 */
public class WebConfigurableValidator implements CategoryValidator {
    
    private boolean enabled = true;
    private CategoryService categoryService;
    private static final String DICTIONARY_API_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/%s";
    private static final int API_TIMEOUT_SECONDS = 8;
    
    public WebConfigurableValidator(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    
    /**
     * Category-specific keywords for definition analysis.
     * When DictionaryAPI.dev returns a definition, we check if it contains
     * category-relevant keywords to determine semantic category match.
     */
    private static final Map<String, Set<String>> CATEGORY_KEYWORDS = new HashMap<>();
    
    static {
        // ANIMAL category keywords
        Set<String> animalKeywords = new HashSet<>();
        animalKeywords.add("animal");
        animalKeywords.add("mammal");
        animalKeywords.add("bird");
        animalKeywords.add("fish");
        animalKeywords.add("reptile");
        animalKeywords.add("insect");
        animalKeywords.add("creature");
        animalKeywords.add("pet");
        animalKeywords.add("domesticated");
        animalKeywords.add("wildlife");
        animalKeywords.add("species");
        animalKeywords.add("vertebrate");
        animalKeywords.add("invertebrate");
        CATEGORY_KEYWORDS.put("ANIMAL", animalKeywords);
        
        // FRUIT category keywords
        Set<String> fruitKeywords = new HashSet<>();
        fruitKeywords.add("fruit");
        fruitKeywords.add("berry");
        fruitKeywords.add("citrus");
        fruitKeywords.add("tropical");
        fruitKeywords.add("edible");
        fruitKeywords.add("sweet");
        fruitKeywords.add("juicy");
        fruitKeywords.add("vitamin");
        fruitKeywords.add("nutritious");
        fruitKeywords.add("organic");
        fruitKeywords.add("fresh");
        fruitKeywords.add("ripe");
        CATEGORY_KEYWORDS.put("FRUIT", fruitKeywords);
        
        // PAYS (Country) category keywords
        Set<String> countryKeywords = new HashSet<>();
        countryKeywords.add("country");
        countryKeywords.add("nation");
        countryKeywords.add("republic");
        countryKeywords.add("kingdom");
        countryKeywords.add("state");
        countryKeywords.add("territory");
        countryKeywords.add("sovereign");
        countryKeywords.add("government");
        countryKeywords.add("capital");
        countryKeywords.add("continent");
        countryKeywords.add("border");
        countryKeywords.add("citizenship");
        CATEGORY_KEYWORDS.put("PAYS", countryKeywords);
        
        // VILLE (City) category keywords
        Set<String> cityKeywords = new HashSet<>();
        cityKeywords.add("city");
        cityKeywords.add("town");
        cityKeywords.add("municipality");
        cityKeywords.add("urban");
        cityKeywords.add("metropolitan");
        cityKeywords.add("capital");
        cityKeywords.add("district");
        cityKeywords.add("borough");
        cityKeywords.add("settlement");
        cityKeywords.add("population");
        cityKeywords.add("downtown");
        cityKeywords.add("suburb");
        CATEGORY_KEYWORDS.put("VILLE", cityKeywords);
        
        // Add other categories as needed
        CATEGORY_KEYWORDS.put("PRENOM", new HashSet<>());
        CATEGORY_KEYWORDS.put("METIER", new HashSet<>());
        CATEGORY_KEYWORDS.put("OBJET", new HashSet<>());
    }
    
    @Override
    public ValidationResult validate(String word, Category category) {
        if (!enabled) {
            return new ValidationResult(ValidationStatus.UNCERTAIN, 0.5, getSourceName(), "Validator disabled");
        }
        
        if (word == null || word.trim().isEmpty()) {
            return new ValidationResult(ValidationStatus.INVALID, 0.0, getSourceName(), "Empty word");
        }
        
        try {
            return validateWithDictionaryAPI(word.trim(), category);
        } catch (Exception e) {
            return new ValidationResult(ValidationStatus.UNCERTAIN, 0.5, getSourceName(), 
                "API validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Validates word using DictionaryAPI.dev and category keyword matching.
     */
    private ValidationResult validateWithDictionaryAPI(String word, Category category) throws IOException {
        String apiUrl = String.format(DICTIONARY_API_URL, word.toLowerCase());
        
        try {
            String response = HttpClientService.get(apiUrl, API_TIMEOUT_SECONDS);
            return analyzeAPIResponse(response, word, category);
            
        } catch (IOException e) {
            if (e.getMessage().contains("404")) {
                // Word not found in dictionary
                return new ValidationResult(ValidationStatus.INVALID, 0.0, getSourceName(), 
                    "Word '" + word + "' not found in English dictionary");
            } else {
                // Network or other API error
                return new ValidationResult(ValidationStatus.UNCERTAIN, 0.5, getSourceName(), 
                    "Dictionary API error: " + e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            return new ValidationResult(ValidationStatus.UNCERTAIN, 0.5, getSourceName(), 
                "API request interrupted: " + e.getMessage());
        }
    }
    
    /**
     * Analyzes DictionaryAPI.dev response to determine category match.
     */
    private ValidationResult analyzeAPIResponse(String response, String word, Category category) {
        if (response == null || response.trim().isEmpty()) {
            return new ValidationResult(ValidationStatus.UNCERTAIN, 0.5, getSourceName(), "Empty API response");
        }
        
        try {
            // Simple JSON parsing - look for definitions
            String lowerResponse = response.toLowerCase();
            
            // Check if word exists (successful API response means word exists)
            boolean wordExists = lowerResponse.contains("\"word\"") && lowerResponse.contains("\"meanings\"");
            
            if (!wordExists) {
                return new ValidationResult(ValidationStatus.INVALID, 0.0, getSourceName(), 
                    "Word not recognized by dictionary API");
            }
            
            // Check for category-specific keywords in definitions
            Set<String> categoryKeywords = CATEGORY_KEYWORDS.get(category.getName());
            if (categoryKeywords == null || categoryKeywords.isEmpty()) {
                // Category not supported by this validator
                return new ValidationResult(ValidationStatus.UNCERTAIN, 0.6, getSourceName(), 
                    "Category '" + category + "' validation not implemented");
            }
            
            // Count keyword matches in response
            int keywordMatches = 0;
            for (String keyword : categoryKeywords) {
                if (lowerResponse.contains(keyword.toLowerCase())) {
                    keywordMatches++;
                }
            }
            
            // Determine validation result based on keyword matches
            if (keywordMatches > 0) {
                double confidence = Math.min(0.85, 0.75 + (keywordMatches * 0.02)); // 0.75-0.85 range
                return new ValidationResult(ValidationStatus.VALID, confidence, getSourceName(), 
                    String.format("Word '%s' matches %s category (%d keyword matches)", 
                                word, category.displayName(), keywordMatches));
            } else {
                // Word exists but doesn't match category
                return new ValidationResult(ValidationStatus.INVALID, 0.0, getSourceName(), 
                    String.format("Word '%s' exists but doesn't match %s category", 
                                word, category.displayName()));
            }
            
        } catch (Exception e) {
            return new ValidationResult(ValidationStatus.UNCERTAIN, 0.5, getSourceName(), 
                "Response parsing error: " + e.getMessage());
        }
    }
    
    @Override
    public String getSourceName() {
        return "WEB_VALIDATOR";
    }
    
    @Override
    public boolean isAvailable() {
        return enabled;
    }
    
    /**
     * Enable/disable this validator
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Test method to verify WebConfigurableValidator functionality
     */
    public static void main(String[] args) {
        System.out.println("🌐 WEB CONFIGURABLE VALIDATOR TEST");
        System.out.println("==================================================");
        System.out.println("Testing WebConfigurableValidator with DictionaryAPI.dev...\n");
        
        // Initialize CategoryService for testing
        CategoryService categoryService = new CategoryService();
        WebConfigurableValidator validator = new WebConfigurableValidator(categoryService);
        
        // Test real words in correct categories
        System.out.println("--- Testing Real Words in Correct Categories ---");
        testValidation(validator, "dog", categoryService.findByName("ANIMAL").orElse(null), "Real animal word");
        testValidation(validator, "cat", categoryService.findByName("ANIMAL").orElse(null), "Real animal word");
        testValidation(validator, "apple", categoryService.findByName("FRUIT").orElse(null), "Real fruit word");
        testValidation(validator, "banana", categoryService.findByName("FRUIT").orElse(null), "Real fruit word");
        testValidation(validator, "france", categoryService.findByName("PAYS").orElse(null), "Real country word");
        testValidation(validator, "paris", categoryService.findByName("VILLE").orElse(null), "Real city word");
        
        // Test real words in wrong categories
        System.out.println("--- Testing Real Words in Wrong Categories ---");
        testValidation(validator, "dog", categoryService.findByName("FRUIT").orElse(null), "Animal word in fruit category");
        testValidation(validator, "apple", categoryService.findByName("ANIMAL").orElse(null), "Fruit word in animal category");
        testValidation(validator, "paris", categoryService.findByName("ANIMAL").orElse(null), "City word in animal category");
        
        // Test nonsense words
        System.out.println("--- Testing Nonsense Words ---");
        testValidation(validator, "zzxqp", categoryService.findByName("ANIMAL").orElse(null), "Nonsense word");
        testValidation(validator, "blahblah123", categoryService.findByName("FRUIT").orElse(null), "Nonsense word");
        testValidation(validator, "qwerty", categoryService.findByName("PAYS").orElse(null), "Common nonsense word");
        
        // Test empty strings
        System.out.println("--- Testing Edge Cases ---");
        testValidation(validator, "", categoryService.findByName("ANIMAL").orElse(null), "Empty string");
        testValidation(validator, "   ", categoryService.findByName("FRUIT").orElse(null), "Whitespace only");
        testValidation(validator, null, categoryService.findByName("VILLE").orElse(null), "Null input");
        
        System.out.println("==================================================");
        System.out.println("✅ WebConfigurableValidator test completed");
        System.out.println("\nNow integrated into CategorizationEngine pipeline:");
        System.out.println("FixedListValidator → WebConfigurableValidator → SemanticAiValidator");
    }
    
    private static void testValidation(WebConfigurableValidator validator, String word, Category category, String description) {
        System.out.printf("Testing: '%-15s' in %-8s | %s%n", 
                         word == null ? "null" : word, category.name(), description);
        
        try {
            ValidationResult result = validator.validate(word, category);
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