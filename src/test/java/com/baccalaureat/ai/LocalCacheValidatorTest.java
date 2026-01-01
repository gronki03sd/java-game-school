package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LocalCacheValidator - STEP 1 of validation pipeline.
 */
@ExtendWith(MockitoExtension.class)
class LocalCacheValidatorTest {
    
    @Mock
    private CacheService cacheService;
    
    private LocalCacheValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new LocalCacheValidator(cacheService);
    }
    
    @Test
    void testValidate_CacheHit() {
        // Given: Word exists in cache
        when(cacheService.isWordValidated("cat", Category.ANIMAL)).thenReturn(true);
        
        // When: Validate the word
        ValidationResult result = validator.validate("cat", Category.ANIMAL);
        
        // Then: Should return VALID from LOCAL_DB
        assertEquals(ValidationStatus.VALID, result.getStatus());
        assertEquals(0.90, result.getConfidence(), 0.01);
        assertEquals("LOCAL_DB", result.getSource());
        assertEquals("Previously validated word (local cache)", result.getDetails());
        
        // Verify cache was checked
        verify(cacheService).isWordValidated("cat", Category.ANIMAL);
    }
    
    @Test
    void testValidate_CacheMiss() {
        // Given: Word does not exist in cache
        when(cacheService.isWordValidated("zqxw", Category.ANIMAL)).thenReturn(false);
        
        // When: Validate the word
        ValidationResult result = validator.validate("zqxw", Category.ANIMAL);
        
        // Then: Should return UNCERTAIN (never reject)
        assertEquals(ValidationStatus.UNCERTAIN, result.getStatus());
        assertEquals(0.0, result.getConfidence(), 0.01);
        assertEquals("LOCAL_DB", result.getSource());
        assertEquals("Word not found in cache", result.getDetails());
        
        // Verify cache was checked
        verify(cacheService).isWordValidated("zqxw", Category.ANIMAL);
    }
    
    @Test
    void testValidate_EmptyWord() {
        // When: Validate empty word
        ValidationResult result = validator.validate("", Category.ANIMAL);
        
        // Then: Should return UNCERTAIN without checking cache
        assertEquals(ValidationStatus.UNCERTAIN, result.getStatus());
        assertEquals("LOCAL_DB", result.getSource());
        assertTrue(result.getDetails().contains("Empty word"));
        
        // Verify cache was not checked for empty word
        verify(cacheService, never()).isWordValidated(anyString(), any(Category.class));
    }
    
    @Test
    void testValidate_NullWord() {
        // When: Validate null word
        ValidationResult result = validator.validate(null, Category.ANIMAL);
        
        // Then: Should return UNCERTAIN without checking cache
        assertEquals(ValidationStatus.UNCERTAIN, result.getStatus());
        assertEquals("LOCAL_DB", result.getSource());
        assertTrue(result.getDetails().contains("Empty word"));
        
        // Verify cache was not checked for null word
        verify(cacheService, never()).isWordValidated(anyString(), any(Category.class));
    }
    
    @Test
    void testIsAvailable() {
        // When/Then: LocalCacheValidator should always be available
        assertTrue(validator.isAvailable(), "LocalCacheValidator should always be available");
    }
    
    @Test
    void testGetSourceName() {
        // When/Then: Source name should be LOCAL_DB
        assertEquals("LOCAL_DB", validator.getSourceName());
    }
    
    @Test
    void testValidate_NeverRejects() {
        // Given: Various cache miss scenarios
        when(cacheService.isWordValidated(anyString(), any(Category.class))).thenReturn(false);
        
        // When: Validate different words
        ValidationResult result1 = validator.validate("nonexistent", Category.ANIMAL);
        ValidationResult result2 = validator.validate("garbage123", Category.FRUIT);
        ValidationResult result3 = validator.validate("zxcvbnm", Category.PAYS);
        
        // Then: LocalCacheValidator should NEVER return INVALID (only VALID or UNCERTAIN)
        assertNotEquals(ValidationStatus.INVALID, result1.getStatus());
        assertNotEquals(ValidationStatus.INVALID, result2.getStatus());
        assertNotEquals(ValidationStatus.INVALID, result3.getStatus());
        
        // All should be UNCERTAIN for cache misses
        assertEquals(ValidationStatus.UNCERTAIN, result1.getStatus());
        assertEquals(ValidationStatus.UNCERTAIN, result2.getStatus());
        assertEquals(ValidationStatus.UNCERTAIN, result3.getStatus());
    }
}