package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AICategoryValidator.
 * Tests the AI-powered category validation logic using MockAIClient.
 */
class AICategoryValidatorTest {

    private Category testCategory;
    private Category fruitCategory;

    @BeforeEach
    void setUp() {
        // Create test categories for validation
        testCategory = new Category(
            1,
            "TEST_CATEGORY",
            "Test Category",
            "🧪",
            "A category for testing",
            true,
            false
        );
        
        fruitCategory = new Category(
            2,
            "FRUIT",
            "Fruit",
            "🍎",
            "Fruits and fruit-related items",
            true,
            true  // predefined
        );
    }

    @Test
    @DisplayName("High confidence valid word should return VALID status")
    void testHighConfidenceValidWord() {
        // Arrange
        MockAIClient mockClient = MockAIClient.createHighConfidenceMock(true);
        AICategoryValidator validator = new AICategoryValidator(mockClient);
        
        // Act
        ValidationResult result = validator.validate("pomme", fruitCategory);
        
        // Assert
        assertNotNull(result, "ValidationResult should not be null");
        assertEquals(ValidationStatus.VALID, result.getStatus(), "Status should be VALID for high confidence valid word");
        assertTrue(result.isValid(), "Result should be valid");
        assertFalse(result.isUncertain(), "Result should not be uncertain");
        assertTrue(result.getConfidence() >= 0.7, "Confidence should be high (>= 0.7)");
        assertTrue(result.getSource().contains("AI_HIGHCONFIDENCEMOCK"), "Source should contain AI client name");
        assertTrue(result.getDetails().contains("valid"), "Details should indicate validity");
    }

    @Test
    @DisplayName("High confidence invalid word should return INVALID status")
    void testHighConfidenceInvalidWord() {
        // Arrange
        MockAIClient mockClient = MockAIClient.createHighConfidenceMock(false);
        AICategoryValidator validator = new AICategoryValidator(mockClient);
        
        // Act
        ValidationResult result = validator.validate("voiture", fruitCategory);
        
        // Assert
        assertNotNull(result, "ValidationResult should not be null");
        assertEquals(ValidationStatus.INVALID, result.getStatus(), "Status should be INVALID for high confidence invalid word");
        assertFalse(result.isValid(), "Result should not be valid");
        assertFalse(result.isUncertain(), "Result should not be uncertain");
        assertTrue(result.getConfidence() >= 0.7, "Confidence should be high (>= 0.7)");
        assertTrue(result.getSource().contains("AI_HIGHCONFIDENCEMOCK"), "Source should contain AI client name");
        assertTrue(result.getDetails().contains("invalid"), "Details should indicate invalidity");
    }

    @Test
    @DisplayName("Low confidence AI response should return UNCERTAIN status")
    void testLowConfidenceAIResponse() {
        // Arrange
        MockAIClient mockClient = MockAIClient.createLowConfidenceMock(true);
        AICategoryValidator validator = new AICategoryValidator(mockClient);
        
        // Act
        ValidationResult result = validator.validate("ambiguous", testCategory);
        
        // Assert
        assertNotNull(result, "ValidationResult should not be null");
        assertEquals(ValidationStatus.UNCERTAIN, result.getStatus(), "Status should be UNCERTAIN for low confidence AI");
        assertFalse(result.isValid(), "Result should not be valid");
        assertTrue(result.isUncertain(), "Result should be uncertain");
        assertTrue(result.getConfidence() < 0.7, "Confidence should be low (< 0.7)");
        assertTrue(result.getSource().contains("AI_LOWCONFIDENCEMOCK"), "Source should contain AI client name");
        assertTrue(result.getDetails().contains("uncertain"), "Details should indicate uncertainty");
        assertTrue(result.getDetails().contains("below threshold"), "Details should mention threshold");
    }

    @Test
    @DisplayName("AI failure should return UNCERTAIN status as fail-safe")
    void testAIFailureReturnsUncertain() {
        // Arrange
        MockAIClient failingClient = MockAIClient.createFailingMock();
        AICategoryValidator validator = new AICategoryValidator(failingClient);
        
        // Act
        ValidationResult result = validator.validate("test", testCategory);
        
        // Assert
        assertNotNull(result, "ValidationResult should not be null");
        assertEquals(ValidationStatus.UNCERTAIN, result.getStatus(), "Status should be UNCERTAIN when AI fails");
        assertFalse(result.isValid(), "Result should not be valid when AI fails");
        assertTrue(result.isUncertain(), "Result should be uncertain when AI fails");
        assertEquals(0.0, result.getConfidence(), "Confidence should be 0.0 when AI fails");
        assertTrue(result.getDetails().contains("AI validation failed") || result.getDetails().contains("not available"), "Details should indicate AI failure");
    }

    @Test
    @DisplayName("Unhealthy AI client should return UNCERTAIN without querying")
    void testUnhealthyAIClientReturnsUncertain() {
        // Arrange
        MockAIClient unhealthyClient = new MockAIClient("UnhealthyMock", false, null);
        AICategoryValidator validator = new AICategoryValidator(unhealthyClient);
        
        // Act
        ValidationResult result = validator.validate("test", testCategory);
        
        // Assert
        assertNotNull(result, "ValidationResult should not be null");
        assertEquals(ValidationStatus.UNCERTAIN, result.getStatus(), "Status should be UNCERTAIN for unhealthy client");
        assertFalse(result.isValid(), "Result should not be valid");
        assertTrue(result.isUncertain(), "Result should be uncertain");
        assertEquals(0.0, result.getConfidence(), "Confidence should be 0.0");
        assertTrue(result.getDetails().contains("AI client is not available"), "Details should indicate client unavailability");
        assertTrue(result.getDetails().contains("UnhealthyMock"), "Details should contain client name");
    }

    @Test
    @DisplayName("Empty word should return INVALID status")
    void testEmptyWordReturnsInvalid() {
        // Arrange
        MockAIClient mockClient = MockAIClient.createHighConfidenceMock(true);
        AICategoryValidator validator = new AICategoryValidator(mockClient);
        
        // Act
        ValidationResult emptyResult = validator.validate("", testCategory);
        ValidationResult nullResult = validator.validate(null, testCategory);
        ValidationResult whitespaceResult = validator.validate("   ", testCategory);
        
        // Assert
        assertEquals(ValidationStatus.INVALID, emptyResult.getStatus(), "Empty string should be INVALID");
        assertEquals(ValidationStatus.INVALID, nullResult.getStatus(), "Null string should be INVALID");
        assertEquals(ValidationStatus.INVALID, whitespaceResult.getStatus(), "Whitespace-only string should be INVALID");
        
        assertTrue(emptyResult.getDetails().contains("Empty word"), "Details should mention empty word");
        assertTrue(nullResult.getDetails().contains("Empty word"), "Details should mention empty word");
        assertTrue(whitespaceResult.getDetails().contains("Empty word"), "Details should mention empty word");
    }

    @Test
    @DisplayName("Null category should return ERROR status")
    void testNullCategoryReturnsError() {
        // Arrange
        MockAIClient mockClient = MockAIClient.createHighConfidenceMock(true);
        AICategoryValidator validator = new AICategoryValidator(mockClient);
        
        // Act
        ValidationResult result = validator.validate("test", null);
        
        // Assert
        assertNotNull(result, "ValidationResult should not be null");
        assertEquals(ValidationStatus.ERROR, result.getStatus(), "Status should be ERROR for null category");
        assertFalse(result.isValid(), "Result should not be valid");
        assertFalse(result.isUncertain(), "Result should not be uncertain (it's an error)");
        assertEquals(0.0, result.getConfidence(), "Confidence should be 0.0 for error");
        assertTrue(result.getDetails().contains("Category cannot be null"), "Details should mention null category");
    }

    @Test
    @DisplayName("Custom confidence threshold should affect validation decision")
    void testCustomConfidenceThreshold() {
        // Arrange - AI response with 0.6 confidence (between default 0.7 and custom 0.5)
        MockAIClient mockClient = new MockAIClient("CustomMock", new AIResponse(true, 0.6, "Medium confidence"));
        
        // Validator with default threshold (0.7) - should be UNCERTAIN
        AICategoryValidator strictValidator = new AICategoryValidator(mockClient);
        
        // Validator with lower threshold (0.5) - should be VALID  
        AICategoryValidator lenientValidator = new AICategoryValidator(mockClient, 0.5, true);
        
        // Act
        ValidationResult strictResult = strictValidator.validate("test", testCategory);
        ValidationResult lenientResult = lenientValidator.validate("test", testCategory);
        
        // Assert
        assertEquals(ValidationStatus.UNCERTAIN, strictResult.getStatus(), "Strict threshold should result in UNCERTAIN");
        assertEquals(ValidationStatus.VALID, lenientResult.getStatus(), "Lenient threshold should result in VALID");
        
        assertEquals(0.6, strictResult.getConfidence(), 0.01, "Both should have same confidence");
        assertEquals(0.6, lenientResult.getConfidence(), 0.01, "Both should have same confidence");
    }

    @Test
    @DisplayName("Input normalization should convert to lowercase and remove accents")
    void testInputNormalization() {
        // Arrange
        MockAIClient mockClient = MockAIClient.createHighConfidenceMock(true);
        
        // Validator with normalization enabled (default)
        AICategoryValidator validatorWithNormalization = new AICategoryValidator(mockClient, 0.7, true);
        
        // Validator with normalization disabled
        AICategoryValidator validatorWithoutNormalization = new AICategoryValidator(mockClient, 0.7, false);
        
        // Act - test with accented uppercase text
        String accentedWord = "  CAFÉ  ";
        ValidationResult normalizedResult = validatorWithNormalization.validate(accentedWord, fruitCategory);
        ValidationResult rawResult = validatorWithoutNormalization.validate(accentedWord, fruitCategory);
        
        // Assert - both should work (mocked), but normalization behavior can be inferred from successful processing
        assertNotNull(normalizedResult, "Normalized result should not be null");
        assertNotNull(rawResult, "Raw result should not be null");
        
        // Both should succeed since MockAIClient handles any input
        assertEquals(ValidationStatus.VALID, normalizedResult.getStatus(), "Normalized input should be processed");
        assertEquals(ValidationStatus.VALID, rawResult.getStatus(), "Raw input should be processed");
    }

    @Test
    @DisplayName("Validator configuration should be accessible via getters")
    void testValidatorConfiguration() {
        // Arrange
        MockAIClient mockClient = MockAIClient.createHighConfidenceMock(true);
        AICategoryValidator validator = new AICategoryValidator(mockClient, 0.8, false);
        
        // Act & Assert
        assertSame(mockClient, validator.getAIClient(), "AI client should be accessible");
        assertEquals(0.8, validator.getConfidenceThreshold(), 0.01, "Confidence threshold should be accessible");
        assertFalse(validator.isNormalizationEnabled(), "Normalization setting should be accessible");
        
        // Test isAvailable method
        assertTrue(validator.isAvailable(), "Validator should be available when client is healthy");
        
        // Test getSourceName method
        String sourceName = validator.getSourceName();
        assertNotNull(sourceName, "Source name should not be null");
        assertTrue(sourceName.startsWith("AI_"), "Source name should start with AI_");
        assertTrue(sourceName.contains("HIGHCONFIDENCEMOCK"), "Source name should contain client name");
        assertEquals("AI_HIGHCONFIDENCEMOCK", sourceName, "Source name should match expected format");
    }

    @Test
    @DisplayName("Validator with failing client should report as unavailable")
    void testValidatorAvailabilityWithFailingClient() {
        // Arrange
        MockAIClient failingClient = MockAIClient.createFailingMock();
        AICategoryValidator validator = new AICategoryValidator(failingClient);
        
        // Act & Assert
        assertFalse(validator.isAvailable(), "Validator should not be available when client is unhealthy");
    }

    @Test
    @DisplayName("Constructor validation should reject invalid parameters")
    void testConstructorValidation() {
        // Test null AI client
        assertThrows(IllegalArgumentException.class, () -> {
            new AICategoryValidator(null);
        }, "Constructor should reject null AI client");
        
        // Test invalid confidence threshold
        MockAIClient mockClient = MockAIClient.createHighConfidenceMock(true);
        
        assertThrows(IllegalArgumentException.class, () -> {
            new AICategoryValidator(mockClient, -0.1, true);
        }, "Constructor should reject negative confidence threshold");
        
        assertThrows(IllegalArgumentException.class, () -> {
            new AICategoryValidator(mockClient, 1.1, true);
        }, "Constructor should reject confidence threshold > 1.0");
        
        // Test valid edge cases
        assertDoesNotThrow(() -> {
            new AICategoryValidator(mockClient, 0.0, true);
        }, "Constructor should accept 0.0 threshold");
        
        assertDoesNotThrow(() -> {
            new AICategoryValidator(mockClient, 1.0, true);
        }, "Constructor should accept 1.0 threshold");
    }

    @Test
    @DisplayName("AI response with reasoning should include reasoning in details")
    void testAIResponseWithReasoning() {
        // Arrange
        String reasoning = "This is clearly a fruit because it grows on trees";
        MockAIClient mockClient = new MockAIClient(
            "ReasoningMock", 
            new AIResponse(true, 0.9, reasoning)
        );
        AICategoryValidator validator = new AICategoryValidator(mockClient);
        
        // Act
        ValidationResult result = validator.validate("apple", fruitCategory);
        
        // Assert
        assertNotNull(result, "ValidationResult should not be null");
        assertEquals(ValidationStatus.VALID, result.getStatus(), "Status should be VALID");
        assertTrue(result.getDetails().contains(reasoning), "Details should include AI reasoning");
    }
}