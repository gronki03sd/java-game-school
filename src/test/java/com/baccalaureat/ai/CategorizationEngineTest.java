package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategorizationEngine - validation pipeline orchestration.
 */
@ExtendWith(MockitoExtension.class)
class CategorizationEngineTest {
    
    @Mock
    private LocalCacheValidator localCacheValidator;
    
    @Mock
    private FixedListValidator fixedListValidator;
    
    @Mock
    private WebConfigurableValidator webValidator;
    
    @Mock
    private SemanticAiValidator semanticAiValidator;
    
    private CategorizationEngine engine;
    
    @BeforeEach
    void setUp() {
        // Create engine with mocked validators
        List<CategoryValidator> validators = Arrays.asList(
            localCacheValidator,
            fixedListValidator,
            webValidator,
            semanticAiValidator
        );
        engine = new CategorizationEngine(validators);
        
        // Set up validator availability
        when(localCacheValidator.isAvailable()).thenReturn(true);
        when(fixedListValidator.isAvailable()).thenReturn(true);
        when(webValidator.isAvailable()).thenReturn(true);
        when(semanticAiValidator.isAvailable()).thenReturn(false); // AI not available yet
        
        // Set up source names
        when(localCacheValidator.getSourceName()).thenReturn("LOCAL_DB");
        when(fixedListValidator.getSourceName()).thenReturn("FIXED_LIST");
        when(webValidator.getSourceName()).thenReturn("WEB_VALIDATOR");
        when(semanticAiValidator.getSourceName()).thenReturn("AI");
    }
    
    @Test
    void testValidationOrder_CacheHit() {
        // Given: Cache hit (should short-circuit pipeline)
        ValidationResult cacheResult = new ValidationResult(
            ValidationStatus.VALID, 0.90, "LOCAL_DB", "Cache hit"
        );
        when(localCacheValidator.validate("cat", Category.ANIMAL)).thenReturn(cacheResult);
        
        // When: Validate word
        ValidationResult result = engine.validate("cat", Category.ANIMAL);
        
        // Then: Should return cache result and not call other validators
        assertEquals(ValidationStatus.VALID, result.getStatus());
        assertEquals("LOCAL_DB", result.getSource());
        
        // Verify validation order and short-circuiting
        verify(localCacheValidator).validate("cat", Category.ANIMAL);
        verify(fixedListValidator, never()).validate(anyString(), any(Category.class));
        verify(webValidator, never()).validate(anyString(), any(Category.class));
        verify(semanticAiValidator, never()).validate(anyString(), any(Category.class));
    }
    
    @Test
    void testValidationOrder_CacheMissFixedListHit() {
        // Given: Cache miss, fixed list hit
        ValidationResult cacheMiss = new ValidationResult(
            ValidationStatus.UNCERTAIN, 0.0, "LOCAL_DB", "Cache miss"
        );
        ValidationResult fixedListHit = new ValidationResult(
            ValidationStatus.VALID, 1.0, "FIXED_LIST", "Fixed list hit"
        );
        
        when(localCacheValidator.validate("dog", Category.ANIMAL)).thenReturn(cacheMiss);
        when(fixedListValidator.validate("dog", Category.ANIMAL)).thenReturn(fixedListHit);
        
        // When: Validate word
        ValidationResult result = engine.validate("dog", Category.ANIMAL);
        
        // Then: Should return fixed list result and not call web validator
        assertEquals(ValidationStatus.VALID, result.getStatus());
        assertEquals("FIXED_LIST", result.getSource());
        
        // Verify validation order
        verify(localCacheValidator).validate("dog", Category.ANIMAL);
        verify(fixedListValidator).validate("dog", Category.ANIMAL);
        verify(webValidator, never()).validate(anyString(), any(Category.class));
    }
    
    @Test
    void testValidationOrder_CacheMissFixedListMissWebHit() {
        // Given: Cache miss, fixed list miss, web hit
        ValidationResult cacheMiss = new ValidationResult(
            ValidationStatus.UNCERTAIN, 0.0, "LOCAL_DB", "Cache miss"
        );
        ValidationResult fixedListMiss = new ValidationResult(
            ValidationStatus.UNCERTAIN, 0.0, "FIXED_LIST", "Not in fixed list"
        );
        ValidationResult webHit = new ValidationResult(
            ValidationStatus.VALID, 0.80, "WEB_VALIDATOR", "Web validation success"
        );
        
        when(localCacheValidator.validate("elephant", Category.ANIMAL)).thenReturn(cacheMiss);
        when(fixedListValidator.validate("elephant", Category.ANIMAL)).thenReturn(fixedListMiss);
        when(webValidator.validate("elephant", Category.ANIMAL)).thenReturn(webHit);
        
        // When: Validate word
        ValidationResult result = engine.validate("elephant", Category.ANIMAL);
        
        // Then: Should return web validator result
        assertEquals(ValidationStatus.VALID, result.getStatus());
        assertEquals("WEB_VALIDATOR", result.getSource());
        
        // Verify all available validators were called in order
        verify(localCacheValidator).validate("elephant", Category.ANIMAL);
        verify(fixedListValidator).validate("elephant", Category.ANIMAL);
        verify(webValidator).validate("elephant", Category.ANIMAL);
        verify(semanticAiValidator, never()).validate(anyString(), any(Category.class)); // Not available
    }
    
    @Test
    void testValidationOrder_LocalCacheFirst() {
        // Given: Any validation scenario
        when(localCacheValidator.validate(anyString(), any(Category.class)))
            .thenReturn(new ValidationResult(ValidationStatus.UNCERTAIN, 0.0, "LOCAL_DB", "Cache miss"));
        when(fixedListValidator.validate(anyString(), any(Category.class)))
            .thenReturn(new ValidationResult(ValidationStatus.VALID, 1.0, "FIXED_LIST", "Fixed list hit"));
        
        // When: Validate word
        engine.validate("test", Category.ANIMAL);
        
        // Then: Verify LocalCacheValidator is called first
        // This is verified by checking call order
        var inOrder = inOrder(localCacheValidator, fixedListValidator, webValidator);
        inOrder.verify(localCacheValidator).validate("test", Category.ANIMAL);
        inOrder.verify(fixedListValidator).validate("test", Category.ANIMAL);
    }
    
    @Test
    void testGetAvailableValidators() {
        // When: Get available validators
        List<String> available = engine.getAvailableValidators();
        
        // Then: Should include available validators in order
        assertTrue(available.contains("LOCAL_DB"));
        assertTrue(available.contains("FIXED_LIST"));
        assertTrue(available.contains("WEB_VALIDATOR"));
        assertFalse(available.contains("AI")); // Not available
        
        // Verify order: LOCAL_DB should be first
        assertEquals("LOCAL_DB", available.get(0));
    }
    
    @Test
    void testValidate_EmptyWord() {
        // When: Validate empty word
        ValidationResult result = engine.validate("", Category.ANIMAL);
        
        // Then: Should return invalid
        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertEquals("ENGINE", result.getSource());
        assertTrue(result.getDetails().contains("Empty word"));
        
        // Verify no validators were called
        verify(localCacheValidator, never()).validate(anyString(), any(Category.class));
        verify(fixedListValidator, never()).validate(anyString(), any(Category.class));
        verify(webValidator, never()).validate(anyString(), any(Category.class));
    }
    
    @Test
    void testValidate_NullWord() {
        // When: Validate null word
        ValidationResult result = engine.validate(null, Category.ANIMAL);
        
        // Then: Should return invalid
        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertEquals("ENGINE", result.getSource());
        
        // Verify no validators were called
        verify(localCacheValidator, never()).validate(anyString(), any(Category.class));
        verify(fixedListValidator, never()).validate(anyString(), any(Category.class));
        verify(webValidator, never()).validate(anyString(), any(Category.class));
    }
}