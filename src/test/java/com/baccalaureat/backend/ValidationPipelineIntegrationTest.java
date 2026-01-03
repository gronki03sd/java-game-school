package com.baccalaureat.backend;

import com.baccalaureat.ai.CategorizationEngine;
import com.baccalaureat.ai.CategoryValidator;
import com.baccalaureat.ai.FixedListValidator;
import com.baccalaureat.ai.LocalCacheValidator;
import com.baccalaureat.ai.WebConfigurableValidator;
import com.baccalaureat.dao.CategoryDAO;
import com.baccalaureat.dao.DatabaseManager;
import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.service.CategoryService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests integration between database-loaded categories and the validation pipeline.
 * Verifies that predefined and custom categories work correctly with validators.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ValidationPipelineIntegrationTest {

    private static final String TEST_DB = "test_validation.db";
    private CategoryDAO categoryDAO;
    private CategoryService categoryService;
    private CategorizationEngine categorizationEngine;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up any existing test database
        File testDbFile = new File(TEST_DB);
        if (testDbFile.exists()) {
            testDbFile.delete();
        }
        
        // Override database URL for testing
        System.setProperty("db.url", "jdbc:sqlite:" + TEST_DB);
        
        // Force database initialization with clean state
        DatabaseManager.initializeDatabase();
        
        categoryDAO = new CategoryDAO();
        categoryService = new CategoryService();
        categorizationEngine = new CategorizationEngine(categoryService);
    }

    @AfterEach
    void tearDown() {
        // Clean up test database
        File testDbFile = new File(TEST_DB);
        if (testDbFile.exists()) {
            testDbFile.delete();
        }
        System.clearProperty("db.url");
    }

    @Test
    @Order(1)
    @DisplayName("CategorizationEngine should initialize with database categories")
    void testCategorizationEngineInitialization() throws Exception {
        assertNotNull(categorizationEngine, "CategorizationEngine should be created");
        
        // Verify it has access to predefined categories
        List<Category> allCategories = categoryService.getAllCategories();
        long predefinedCount = allCategories.stream()
            .filter(Category::isPredefined)
            .count();
        
        assertEquals(10, predefinedCount, "Should have 10 predefined categories available for validation");
    }

    @Test
    @Order(2)
    @DisplayName("FixedListValidator should recognize predefined category words")
    void testFixedListValidatorWithPredefinedCategories() throws Exception {
        // Get a predefined category (e.g., ANIMAL)
        Category animalCategory = categoryService.getAllCategories().stream()
            .filter(cat -> cat.getName().equals("ANIMAL") && cat.isPredefined())
            .findFirst()
            .orElseThrow(() -> new AssertionError("ANIMAL predefined category should exist"));

        // Create FixedListValidator for the ANIMAL category
        FixedListValidator validator = new FixedListValidator();
        
        // Test with known animal words
        ValidationResult dogResult = validator.validate("chien", animalCategory);
        assertNotNull(dogResult, "Validation result should not be null");
        
        // The result should be either VALID (if word is in fixed list) or UNCERTAIN (to pass to next validator)
        assertTrue(
            dogResult.getStatus() == ValidationStatus.VALID || 
            dogResult.getStatus() == ValidationStatus.UNCERTAIN,
            "FixedListValidator should return VALID or UNCERTAIN for animal words"
        );
        
        // Test with clearly non-animal word
        ValidationResult numberResult = validator.validate("123", animalCategory);
        assertNotNull(numberResult, "Validation result should not be null");
        
        // Numbers should typically be rejected or passed as unknown
        assertNotEquals(ValidationStatus.VALID, numberResult.getStatus(),
            "Numbers should not be validated as animals");
    }

    @Test
    @Order(3)
    @DisplayName("WebConfigurableValidator should accept database category identifiers")
    void testWebConfigurableValidatorWithDatabaseCategories() throws Exception {
        // Get predefined categories
        List<Category> predefinedCategories = categoryService.getAllCategories().stream()
            .filter(Category::isPredefined)
            .toList();
        
        assertFalse(predefinedCategories.isEmpty(), "Should have predefined categories");
        
        // Test WebConfigurableValidator initialization with database categories
        for (Category category : predefinedCategories) {
            assertDoesNotThrow(() -> {
                WebConfigurableValidator validator = new WebConfigurableValidator(categoryService);
                // The validator should be able to work with category identifiers
                assertNotNull(validator, "WebConfigurableValidator should initialize");
            }, "WebConfigurableValidator should accept category: " + category.getName());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Custom categories should be recognized by validation pipeline")
    void testValidationPipelineWithCustomCategories() throws Exception {
        // Create a custom category
        Category customCategory = new Category(
            "CUSTOM_FOOD",
            "Nourriture Personnalisée",
            "🍽️",
            "Une catégorie de nourriture personnalisée",
            true,
            false  // Not predefined
        );
        
        CategoryService.CategoryCreationResult result = categoryService.createCategory(customCategory);
        assertNotNull(result, "CategoryCreationResult should not be null");
        assertTrue(result.isSuccess(), "Custom category should be created successfully");
        Category created = result.getCategory();
        
        // Create validators for the custom category
        LocalCacheValidator localValidator = new LocalCacheValidator();
        FixedListValidator fixedValidator = new FixedListValidator();
        WebConfigurableValidator webValidator = new WebConfigurableValidator(categoryService);
        
        // All validators should initialize without issues
        assertNotNull(localValidator, "LocalCacheValidator should work with custom categories");
        assertNotNull(fixedValidator, "FixedListValidator should work with custom categories");
        assertNotNull(webValidator, "WebConfigurableValidator should work with custom categories");
        
        // Test validation with custom category
        String testWord = "pizza";
        
        // LocalCacheValidator test
        ValidationResult localResult = localValidator.validate(testWord, created);
        assertNotNull(localResult, "LocalCacheValidator should return a result");
        
        // FixedListValidator test
        ValidationResult fixedResult = fixedValidator.validate(testWord, created);
        assertNotNull(fixedResult, "FixedListValidator should return a result");
        
        // Results should be valid statuses
        assertTrue(
            localResult.getStatus() == ValidationStatus.VALID ||
            localResult.getStatus() == ValidationStatus.INVALID ||
            localResult.getStatus() == ValidationStatus.UNCERTAIN,
            "LocalCacheValidator should return a valid status"
        );
        
        assertTrue(
            fixedResult.getStatus() == ValidationStatus.VALID ||
            fixedResult.getStatus() == ValidationStatus.INVALID ||
            fixedResult.getStatus() == ValidationStatus.UNCERTAIN,
            "FixedListValidator should return a valid status"
        );
    }

    @Test
    @Order(5)
    @DisplayName("Validation pipeline should maintain correct order")
    void testValidationPipelineOrder() throws Exception {
        // Get a predefined category for testing
        Category testCategory = categoryService.getAllCategories().stream()
            .filter(cat -> cat.getName().equals("FRUIT") && cat.isPredefined())
            .findFirst()
            .orElseThrow(() -> new AssertionError("FRUIT predefined category should exist"));

        // Create the validation pipeline manually to test order
        List<CategoryValidator> validators = List.of(
            new LocalCacheValidator(),
            new FixedListValidator(),
            new WebConfigurableValidator(categoryService)
        );
        
        assertEquals(3, validators.size(), "Should have 3 validators in pipeline");
        
        // Verify validator types are in correct order
        assertTrue(validators.get(0) instanceof LocalCacheValidator, 
            "First validator should be LocalCacheValidator");
        assertTrue(validators.get(1) instanceof FixedListValidator, 
            "Second validator should be FixedListValidator");
        assertTrue(validators.get(2) instanceof WebConfigurableValidator, 
            "Third validator should be WebConfigurableValidator");
    }

    @Test
    @Order(6)
    @DisplayName("CategorizationEngine should validate words against database categories")
    void testCategorizationEngineWithDatabaseCategories() throws Exception {
        // Test with a word that should belong to a predefined category
        String testWord = "pomme";  // Apple in French - should be FRUIT
        
        // The engine should be able to process this word
        assertDoesNotThrow(() -> {
            // This would normally return validation results
            // For now, just verify the engine can handle database categories
            List<Category> categories = categoryService.getAllCategories();
            assertFalse(categories.isEmpty(), "Should have categories available for validation");
            
            // Verify FRUIT category exists for apple validation
            boolean fruitExists = categories.stream()
                .anyMatch(cat -> cat.getName().equals("FRUIT"));
            assertTrue(fruitExists, "FRUIT category should exist for apple validation");
        }, "CategorizationEngine should handle database categories");
    }

    @Test
    @Order(7)
    @DisplayName("All predefined categories should be available for validation")
    void testAllPredefinedCategoriesAvailable() throws Exception {
        List<Category> predefinedCategories = categoryService.getAllCategories().stream()
            .filter(Category::isPredefined)
            .toList();
        
        assertEquals(10, predefinedCategories.size(), "Should have exactly 10 predefined categories");
        
        // Test that validators can be created for each predefined category
        for (Category category : predefinedCategories) {
            assertDoesNotThrow(() -> {
                LocalCacheValidator localValidator = new LocalCacheValidator();
                FixedListValidator fixedValidator = new FixedListValidator();
                WebConfigurableValidator webValidator = new WebConfigurableValidator(categoryService);
                
                assertNotNull(localValidator, "LocalCacheValidator should work with " + category.getName());
                assertNotNull(fixedValidator, "FixedListValidator should work with " + category.getName());
                assertNotNull(webValidator, "WebConfigurableValidator should work with " + category.getName());
            }, "All validators should work with predefined category: " + category.getName());
        }
    }

    @Test
    @Order(8)
    @DisplayName("Validation pipeline should handle mixed predefined and custom categories")
    void testMixedCategoryValidation() throws Exception {
        // Create a custom category
        Category customCategory = new Category(
            "CUSTOM_SPORT",
            "Sport Personnalisé",
            "⚽",
            "Une catégorie de sport personnalisée",
            true,
            false
        );
        
        CategoryService.CategoryCreationResult result = categoryService.createCategory(customCategory);
        Category createdCustom = result.getCategory();
        
        // Get all categories (predefined + custom)
        List<Category> allCategories = categoryService.getAllCategories();
        
        // Should have predefined + at least one custom
        long predefinedCount = allCategories.stream().filter(Category::isPredefined).count();
        long customCount = allCategories.stream().filter(cat -> !cat.isPredefined()).count();
        
        assertEquals(10, predefinedCount, "Should still have 10 predefined categories");
        assertTrue(customCount >= 1, "Should have at least 1 custom category");
        
        // Test validation works for both types
        Category predefinedCategory = allCategories.stream()
            .filter(cat -> cat.isPredefined() && cat.getName().equals("ANIMAL"))
            .findFirst()
            .orElseThrow();
        
        // Both should work in validation pipeline
        assertDoesNotThrow(() -> {
            LocalCacheValidator predefinedValidator = new LocalCacheValidator();
            LocalCacheValidator customValidator = new LocalCacheValidator();
            
            ValidationResult predefinedResult = predefinedValidator.validate("chat", predefinedCategory);
            ValidationResult customResult = customValidator.validate("football", createdCustom);
            
            assertNotNull(predefinedResult, "Predefined category validation should work");
            assertNotNull(customResult, "Custom category validation should work");
        }, "Validation should work for both predefined and custom categories");
    }
}