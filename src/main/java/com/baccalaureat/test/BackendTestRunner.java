package com.baccalaureat.test;

import com.baccalaureat.ai.CategorizationEngine;
import com.baccalaureat.ai.FixedListValidator;
import com.baccalaureat.ai.WebConfigurableValidator;
import com.baccalaureat.ai.SemanticAiValidator;
import com.baccalaureat.ai.CategoryValidator;
import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Backend test runner for the validation system.
 * Tests the CategorizationEngine with multiple validators without UI dependency.
 * 
 * This class demonstrates:
 * - FixedListValidator for deterministic words
 * - WebConfigurableValidator for web-based validation via DictionaryAPI.dev
 * - Validation pipeline orchestration
 * - Multi-round game simulation
 * 
 * Future extensions:
 * - Add SemanticAiValidator when AI implementation is ready
 * - Add performance benchmarking
 * - Add batch validation testing
 */
public class BackendTestRunner {
    
    private final CategorizationEngine engine;
    
    // Test data: mix of words that should be found in different validators
    private static final TestCase[] TEST_CASES = {
        // Words that should be found in FixedListValidator (high confidence, FIXED_LIST source)
        new TestCase(Category.ANIMAL, "chat", "Expected in FixedList"),
        new TestCase(Category.ANIMAL, "chien", "Expected in FixedList"),
        new TestCase(Category.PAYS, "france", "Expected in FixedList"),
        new TestCase(Category.VILLE, "paris", "Expected in FixedList"),
        new TestCase(Category.FRUIT, "pomme", "Expected in FixedList"),
        
        // Words that should NOT be in FixedList but exist in English (API validation)
        new TestCase(Category.ANIMAL, "elephant", "Should fallback to API"),
        new TestCase(Category.ANIMAL, "butterfly", "Should fallback to API"),
        new TestCase(Category.PAYS, "australia", "Should fallback to API"),
        new TestCase(Category.VILLE, "london", "Should fallback to API"),
        new TestCase(Category.FRUIT, "pineapple", "Should fallback to API"),
        
        // Invalid/non-existent words (should be rejected by both validators)
        new TestCase(Category.ANIMAL, "xyzabc123", "Invalid word test"),
        new TestCase(Category.PAYS, "fakecountry", "Invalid word test"),
        new TestCase(Category.VILLE, "nonexistentcity", "Invalid word test"),
        
        // Edge cases
        new TestCase(Category.ANIMAL, "", "Empty string test"),
        new TestCase(Category.ANIMAL, "   ", "Whitespace test"),
        new TestCase(Category.ANIMAL, "Cat", "Case sensitivity test")
    };
    
    public BackendTestRunner() {
        // Initialize validation pipeline
        // Order: FixedList → Web API → SemanticAI (when ready)
        List<CategoryValidator> validators = new ArrayList<>();
        validators.add(new FixedListValidator());
        validators.add(new WebConfigurableValidator());  // Replaced ApiCategoryValidator
        // TODO: Add SemanticAiValidator when AI implementation is ready
        // validators.add(new SemanticAiValidator());
        
        this.engine = new CategorizationEngine(validators);
    }
    
    /**
     * Main test execution method.
     */
    public static void main(String[] args) {
        BackendTestRunner runner = new BackendTestRunner();
        
        System.out.println("=".repeat(60));
        System.out.println("🧪 BACKEND VALIDATION SYSTEM TEST");
        System.out.println("=".repeat(60));
        
        runner.runSystemInfoTest();
        runner.runSingleWordTests();
        runner.runCategoryTests();
        runner.runMultiRoundSimulation();
        runner.runPerformanceTest();
        
        System.out.println("\\n" + "=".repeat(60));
        System.out.println("✅ ALL TESTS COMPLETED");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Display system information and available validators.
     */
    private void runSystemInfoTest() {
        System.out.println("\\n📋 SYSTEM INFORMATION:");
        System.out.println("-".repeat(40));
        System.out.println("Available validators: " + engine.getAvailableValidators());
        System.out.println("Confidence threshold: " + engine.getConfidenceThreshold());
        System.out.println("Categories to test: " + Arrays.toString(Category.values()));
    }
    
    /**
     * Test individual words across different validators.
     */
    private void runSingleWordTests() {
        System.out.println("\\n🔍 SINGLE WORD VALIDATION TESTS:");
        System.out.println("-".repeat(40));
        
        for (TestCase testCase : TEST_CASES) {
            System.out.printf("Testing: %-15s | Category: %-8s | %s\\n", 
                            "'" + testCase.word + "'", 
                            testCase.category.name(), 
                            testCase.description);
            
            ValidationResult result = engine.validate(testCase.word, testCase.category);
            
            System.out.printf("  → Status: %-9s | Confidence: %4.2f | Source: %-12s | Details: %s\\n\\n",
                            result.getStatus(),
                            result.getConfidence(),
                            result.getSource(),
                            result.getDetails());
        }
    }
    
    /**
     * Test validation across all categories with sample words.
     */
    private void runCategoryTests() {
        System.out.println("\\n📂 CATEGORY-BASED TESTS:");
        System.out.println("-".repeat(40));
        
        for (Category category : Category.values()) {
            System.out.println("\\nCategory: " + category.displayName() + " (" + category.name() + ")");
            System.out.println("Hint: " + category.getHint());
            
            // Test one known good word and one edge case per category
            String[] testWords = getTestWordsForCategory(category);
            
            for (String word : testWords) {
                ValidationResult result = engine.validate(word, category);
                System.out.printf("  %-12s → %-7s (%.2f confidence, %s)\\n",
                                word, 
                                result.getStatus(), 
                                result.getConfidence(),
                                result.getSource());
            }
        }
    }
    
    /**
     * Simulate multiple rounds of the game with different letters.
     */
    private void runMultiRoundSimulation() {
        System.out.println("\\n🎮 MULTI-ROUND GAME SIMULATION:");
        System.out.println("-".repeat(40));
        
        String[] gameLetters = {"A", "B", "C", "M", "P"};
        Category[] gameCategories = {Category.ANIMAL, Category.PAYS, Category.VILLE, Category.FRUIT};
        
        for (String letter : gameLetters) {
            System.out.println("\\n🎯 Round with letter: " + letter);
            int validAnswers = 0;
            int totalAnswers = 0;
            
            for (Category category : gameCategories) {
                String testWord = generateTestWordForLetter(letter, category);
                ValidationResult result = engine.validate(testWord, category);
                totalAnswers++;
                
                if (result.getStatus() == ValidationStatus.VALID) {
                    validAnswers++;
                }
                
                System.out.printf("  %-8s: %-12s → %s (%.2f)\\n",
                                category.name(),
                                testWord,
                                result.getStatus(),
                                result.getConfidence());
            }
            
            System.out.printf("  Round Score: %d/%d valid answers\\n", validAnswers, totalAnswers);
        }
    }
    
    /**
     * Basic performance test to measure validation speed.
     */
    private void runPerformanceTest() {
        System.out.println("\\n⚡ PERFORMANCE TEST:");
        System.out.println("-".repeat(40));
        
        String[] testWords = {"chat", "dog", "france", "paris", "apple"};
        int iterations = 50; // Reduced to avoid too many API calls
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            for (String word : testWords) {
                engine.validate(word, Category.ANIMAL);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.printf("Validated %d words in %d ms\\n", iterations * testWords.length, totalTime);
        System.out.printf("Average time per validation: %.2f ms\\n", (double) totalTime / (iterations * testWords.length));
    }
    
    /**
     * Get test words specific to a category.
     */
    private String[] getTestWordsForCategory(Category category) {
        return switch (category) {
            case ANIMAL -> new String[]{"chat", "elephant"};
            case PAYS -> new String[]{"france", "canada"};
            case VILLE -> new String[]{"paris", "london"};
            case FRUIT -> new String[]{"pomme", "apple"};
            case METIER -> new String[]{"medecin", "teacher"};
            case PRENOM -> new String[]{"pierre", "john"};
            case OBJET -> new String[]{"table", "computer"};
            case CELEBRITE -> new String[]{"napoleon", "einstein"};
        };
    }
    
    /**
     * Generate a test word starting with the specified letter for a category.
     * In a real game, this would be user input.
     */
    private String generateTestWordForLetter(String letter, Category category) {
        // Simple test word generation - in real game this comes from user
        return switch (category) {
            case ANIMAL -> letter.equals("A") ? "ant" : 
                          letter.equals("B") ? "bear" :
                          letter.equals("C") ? "cat" :
                          letter.equals("M") ? "mouse" :
                          letter.equals("P") ? "pig" : "animal";
            case PAYS -> letter.equals("A") ? "australia" :
                        letter.equals("B") ? "brazil" :
                        letter.equals("C") ? "canada" :
                        letter.equals("M") ? "mexico" :
                        letter.equals("P") ? "poland" : "country";
            case VILLE -> letter.equals("A") ? "amsterdam" :
                         letter.equals("B") ? "berlin" :
                         letter.equals("C") ? "chicago" :
                         letter.equals("M") ? "madrid" :
                         letter.equals("P") ? "paris" : "city";
            case FRUIT -> letter.equals("A") ? "apple" :
                         letter.equals("B") ? "banana" :
                         letter.equals("C") ? "cherry" :
                         letter.equals("M") ? "mango" :
                         letter.equals("P") ? "peach" : "fruit";
            default -> "test" + letter.toLowerCase();
        };
    }
    
    /**
     * Helper class to structure test cases.
     */
    private static class TestCase {
        final Category category;
        final String word;
        final String description;
        
        TestCase(Category category, String word, String description) {
            this.category = category;
            this.word = word;
            this.description = description;
        }
    }
}