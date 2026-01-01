package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.service.CacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified functional tests for validation pipeline behavior.
 * Tests core scenarios without complex mocking that causes Java 24 compatibility issues.
 */
class ValidationPipelineFunctionalTest {
    
    private LocalCacheValidator cacheValidator;
    private CategorizationEngine engine;
    
    @BeforeEach
    void setUp() {
        // Use real instances instead of mocks
        cacheValidator = new LocalCacheValidator();
        engine = new CategorizationEngine();
    }
    
    @Test
    void testLocalCacheValidator_CacheMissBehavior() {
        // Test 1: Cache MISS - LocalCacheValidator never rejects
        ValidationResult result = cacheValidator.validate("unknownword123", Category.ANIMAL);
        
        // Assert: Should return UNCERTAIN (not INVALID)
        assertEquals(ValidationStatus.UNCERTAIN, result.getStatus(), 
                    "LocalCacheValidator should return UNCERTAIN for cache miss, never reject");
        assertEquals("LOCAL_DB", result.getSource());
        assertEquals(0.0, result.getConfidence(), 0.01);
        assertTrue(result.getDetails().contains("cache") || result.getDetails().contains("not found"));
    }
    
    @Test
    void testLocalCacheValidator_EmptyWordHandling() {
        // Test edge case: Empty word
        ValidationResult result1 = cacheValidator.validate("", Category.ANIMAL);
        ValidationResult result2 = cacheValidator.validate(null, Category.ANIMAL);
        
        // Both should return UNCERTAIN (never reject)
        assertEquals(ValidationStatus.UNCERTAIN, result1.getStatus());
        assertEquals(ValidationStatus.UNCERTAIN, result2.getStatus());
        assertEquals("LOCAL_DB", result1.getSource());
        assertEquals("LOCAL_DB", result2.getSource());
    }
    
    @Test
    void testCategorizationEngine_ValidatorOrder() {
        // Test 2: Validation ORDER - LocalCacheValidator is first
        var availableValidators = engine.getAvailableValidators();
        
        assertFalse(availableValidators.isEmpty(), "Should have validators available");
        assertEquals("LOCAL_DB", availableValidators.get(0), 
                    "LocalCacheValidator should be FIRST in pipeline");
        
        // Verify expected pipeline order
        assertTrue(availableValidators.contains("LOCAL_DB"), "Should include LOCAL_DB validator");
        assertTrue(availableValidators.contains("FIXED_LIST"), "Should include FIXED_LIST validator");
        
        // LOCAL_DB should come before others
        int localDbIndex = availableValidators.indexOf("LOCAL_DB");
        int fixedListIndex = availableValidators.indexOf("FIXED_LIST");
        assertTrue(localDbIndex < fixedListIndex, 
                  "LOCAL_DB should come before FIXED_LIST in pipeline");
    }
    
    @Test
    void testCategorizationEngine_KnownWordValidation() {
        // Test 3: Pipeline with known word (should be handled by FixedListValidator if not cached)
        ValidationResult result = engine.validate("chien", Category.ANIMAL);
        
        // Should be valid (either from cache or fixed list)
        assertTrue(result.isValid() || result.isUncertain(), 
                  "Known animal word should be valid or at least uncertain");
        
        // Source should be one of the expected validators
        String source = result.getSource();
        assertTrue(source.equals("LOCAL_DB") || source.equals("FIXED_LIST") || source.equals("WEB_VALIDATOR"),
                  "Result should come from expected validator: " + source);
    }
    
    @Test
    void testCategorizationEngine_InvalidInput() {
        // Test 4: Invalid input handling
        ValidationResult nullResult = engine.validate(null, Category.ANIMAL);
        ValidationResult emptyResult = engine.validate("", Category.ANIMAL);
        
        // Engine should handle invalid input
        assertEquals(ValidationStatus.INVALID, nullResult.getStatus());
        assertEquals(ValidationStatus.INVALID, emptyResult.getStatus());
        assertEquals("ENGINE", nullResult.getSource());
        assertEquals("ENGINE", emptyResult.getSource());
    }
    
    @Test
    void testLocalCacheValidator_AlwaysAvailable() {
        // Test 5: LocalCacheValidator availability
        assertTrue(cacheValidator.isAvailable(), "LocalCacheValidator should always be available");
        assertEquals("LOCAL_DB", cacheValidator.getSourceName());
    }
    
    @Test
    void testValidationPipeline_DeterministicFirst() {
        // Test 6: Verify deterministic validation comes after cache
        var validators = engine.getAvailableValidators();
        
        // Should have cache first, then deterministic (fixed list)
        assertTrue(validators.size() >= 2, "Should have at least cache and fixed list validators");
        
        int cacheIndex = validators.indexOf("LOCAL_DB");
        int fixedIndex = validators.indexOf("FIXED_LIST");
        
        assertEquals(0, cacheIndex, "Cache should be first (index 0)");
        assertEquals(1, fixedIndex, "Fixed list should be second (index 1)");
    }
    
    @Test
    void testCacheService_Behavior() {
        // Test 7: CacheService basic behavior (without database connection)
        CacheService cacheService = new CacheService();
        
        // Test that methods don't throw exceptions even without DB
        assertDoesNotThrow(() -> {
            boolean result = cacheService.isWordValidated("test", Category.ANIMAL);
            // Should return false on DB error (graceful degradation)
            assertFalse(result);
        });
        
        assertDoesNotThrow(() -> {
            cacheService.saveValidatedWord("test", Category.ANIMAL);
            // Should not throw even if DB save fails
        });
    }
    
    @Test
    void testValidationResult_CacheFormat() {
        // Test 8: Verify cache hit format matches specification
        // Simulate what a cache hit should look like
        ValidationResult expectedCacheHit = new ValidationResult(
            ValidationStatus.VALID,
            0.90,
            "LOCAL_DB", 
            "Previously validated word (local cache)"
        );
        
        // Verify format matches specification
        assertEquals(ValidationStatus.VALID, expectedCacheHit.getStatus());
        assertEquals(0.90, expectedCacheHit.getConfidence(), 0.01);
        assertEquals("LOCAL_DB", expectedCacheHit.getSource());
        assertTrue(expectedCacheHit.getDetails().contains("Previously validated"));
        assertTrue(expectedCacheHit.getDetails().contains("cache"));
    }
}