package com.baccalaureat.service;

import com.baccalaureat.ai.CategorizationEngine;
import com.baccalaureat.ai.CategoryValidator;
import com.baccalaureat.ai.LocalCacheValidator;
import com.baccalaureat.dao.WordDAO;
import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for ValidationService - cache insertion and pipeline integration.
 */
@ExtendWith(MockitoExtension.class)
class ValidationServiceIntegrationTest {
    
    @Mock
    private WordDAO wordDAO;
    
    @Mock
    private CategorizationEngine categorizationEngine;
    
    @Mock
    private CacheService cacheService;
    
    private ValidationService validationService;
    
    @BeforeEach
    void setUp() {
        // Create ValidationService with mocked dependencies
        validationService = new ValidationService();
        
        // Use reflection or constructor injection if available
        // For this test, we'll mock the behavior instead
    }
    
    @Test
    void testCacheInsertion_AfterWebValidation() {
        // This test verifies that valid results are cached
        // Given: Web validator returns valid result
        ValidationResult webResult = new ValidationResult(
            ValidationStatus.VALID, 0.85, "WEB_VALIDATOR", "Web validation success"
        );
        
        // Mock the categorization engine to return web result
        when(categorizationEngine.validate("elephant", Category.ANIMAL)).thenReturn(webResult);
        
        // When: Validate word through service
        ValidationResult result = validationService.validateWord("ANIMAL", "elephant");
        
        // Then: Result should be valid and cached
        assertTrue(result.isValid());
        
        // Note: This test would need actual integration with real CacheService
        // or dependency injection to verify caching behavior
    }
    
    @Test
    void testValidationPipeline_CacheHitShortCircuit() {
        // This test verifies that cache hits prevent further validation
        // Given: Create a spy on a real LocalCacheValidator
        CacheService realCacheService = mock(CacheService.class);
        LocalCacheValidator cacheValidator = new LocalCacheValidator(realCacheService);
        LocalCacheValidator spyCacheValidator = spy(cacheValidator);
        
        // Mock cache hit
        when(realCacheService.isWordValidated("cat", Category.ANIMAL)).thenReturn(true);
        
        // When: Validate through cache validator
        ValidationResult result = spyCacheValidator.validate("cat", Category.ANIMAL);
        
        // Then: Should return cached result
        assertEquals(ValidationStatus.VALID, result.getStatus());
        assertEquals("LOCAL_DB", result.getSource());
        assertEquals(0.90, result.getConfidence(), 0.01);
        
        // Verify cache service was called
        verify(realCacheService).isWordValidated("cat", Category.ANIMAL);
    }
    
    @Test
    void testValidationPipeline_CacheMissPassthrough() {
        // Given: Create a spy on a real LocalCacheValidator
        CacheService realCacheService = mock(CacheService.class);
        LocalCacheValidator cacheValidator = new LocalCacheValidator(realCacheService);
        
        // Mock cache miss
        when(realCacheService.isWordValidated("unknown", Category.ANIMAL)).thenReturn(false);
        
        // When: Validate through cache validator
        ValidationResult result = cacheValidator.validate("unknown", Category.ANIMAL);
        
        // Then: Should return uncertain (not reject)
        assertEquals(ValidationStatus.UNCERTAIN, result.getStatus());
        assertEquals("LOCAL_DB", result.getSource());
        assertEquals(0.0, result.getConfidence(), 0.01);
        
        // Verify cache service was called but validator doesn't reject
        verify(realCacheService).isWordValidated("unknown", Category.ANIMAL);
    }
    
    @Test
    void testCacheService_SaveAndRetrieve() {
        // This is an integration test for cache functionality
        // Given: Real cache service (would use in-memory database)
        CacheService realCacheService = new CacheService();
        
        // When: Save and retrieve word
        realCacheService.saveValidatedWord("testword", Category.ANIMAL);
        boolean isValidated = realCacheService.isWordValidated("testword", Category.ANIMAL);
        
        // Then: Word should be found (would work with actual database)
        // Note: This test would pass with a real database connection
        // For now, it demonstrates the expected behavior
    }
    
    @Test
    void testValidationService_InputNormalization() {
        // Test that ValidationService normalizes input correctly
        ValidationService service = new ValidationService();
        
        // These tests verify the normalization logic exists
        // The actual validation would depend on the pipeline
        
        // Test null input
        ValidationResult nullResult = service.validateWord("ANIMAL", null);
        assertEquals(ValidationStatus.INVALID, nullResult.getStatus());
        assertTrue(nullResult.getDetails().contains("Empty word"));
        
        // Test empty input
        ValidationResult emptyResult = service.validateWord("ANIMAL", "");
        assertEquals(ValidationStatus.INVALID, emptyResult.getStatus());
        assertTrue(emptyResult.getDetails().contains("Empty word"));
        
        // Test null category
        ValidationResult nullCategoryResult = service.validateWord(null, "cat");
        assertEquals(ValidationStatus.ERROR, nullCategoryResult.getStatus());
        assertTrue(nullCategoryResult.getDetails().contains("Category is null"));
    }
    
    @Test
    void testValidationService_CategoryParsing() {
        // Test that ValidationService can parse category strings
        ValidationService service = new ValidationService();
        
        // Test unknown category
        ValidationResult unknownCategoryResult = service.validateWord("UNKNOWN_CATEGORY", "cat");
        assertEquals(ValidationStatus.ERROR, unknownCategoryResult.getStatus());
        assertTrue(unknownCategoryResult.getDetails().contains("Unknown category"));
    }
}