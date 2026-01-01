package com.baccalaureat.service;

import com.baccalaureat.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CacheService - database operations for validated words cache.
 */
class CacheServiceTest {
    
    private CacheService cacheService;
    private String testDatabaseUrl;
    
    @BeforeEach
    void setUp(@TempDir Path tempDir) throws SQLException {
        // Use in-memory SQLite database for testing
        testDatabaseUrl = "jdbc:sqlite::memory:";
        
        // Mock DatabaseManager to use test database
        try (MockedStatic<com.baccalaureat.dao.DatabaseManager> mockedDbManager = 
             Mockito.mockStatic(com.baccalaureat.dao.DatabaseManager.class)) {
            
            // Create test database connection
            Connection testConnection = DriverManager.getConnection(testDatabaseUrl);
            mockedDbManager.when(com.baccalaureat.dao.DatabaseManager::getConnection)
                          .thenReturn(testConnection);
            
            // Initialize test database schema
            initializeTestDatabase(testConnection);
            
            cacheService = new CacheService();
        }
    }
    
    private void initializeTestDatabase(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "CREATE TABLE validated_words (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "word TEXT NOT NULL, " +
            "category TEXT NOT NULL, " +
            "validated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "UNIQUE(word, category)" +
            ")")) {
            stmt.execute();
        }
    }
    
    @Test
    void testIsWordValidated_CacheHit() throws SQLException {
        // Given: Insert a word into cache
        try (Connection conn = DriverManager.getConnection(testDatabaseUrl);
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO validated_words (word, category) VALUES (?, ?)")) {
            stmt.setString(1, "cat");
            stmt.setString(2, "ANIMAL");
            stmt.execute();
        }
        
        // When: Check if word is validated
        boolean result = cacheService.isWordValidated("cat", Category.ANIMAL);
        
        // Then: Should return true (cache hit)
        assertTrue(result, "Should find 'cat' in ANIMAL category cache");
    }
    
    @Test
    void testIsWordValidated_CacheMiss() {
        // When: Check for non-existent word
        boolean result = cacheService.isWordValidated("zqxw", Category.ANIMAL);
        
        // Then: Should return false (cache miss)
        assertFalse(result, "Should not find 'zqxw' in cache");
    }
    
    @Test
    void testIsWordValidated_CategorySpecific() throws SQLException {
        // Given: Insert word for ANIMAL category only
        try (Connection conn = DriverManager.getConnection(testDatabaseUrl);
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO validated_words (word, category) VALUES (?, ?)")) {
            stmt.setString(1, "cat");
            stmt.setString(2, "ANIMAL");
            stmt.execute();
        }
        
        // When/Then: Check same word in different categories
        assertTrue(cacheService.isWordValidated("cat", Category.ANIMAL), 
                  "Should find 'cat' in ANIMAL category");
        assertFalse(cacheService.isWordValidated("cat", Category.FRUIT), 
                   "Should not find 'cat' in FRUIT category");
    }
    
    @Test
    void testSaveValidatedWord() throws SQLException {
        // When: Save a word
        cacheService.saveValidatedWord("dog", Category.ANIMAL);
        
        // Then: Word should be retrievable
        assertTrue(cacheService.isWordValidated("dog", Category.ANIMAL),
                  "Saved word should be found in cache");
    }
    
    @Test
    void testSaveValidatedWord_Normalization() throws SQLException {
        // When: Save word with different casing and accents
        cacheService.saveValidatedWord("  DOG  ", Category.ANIMAL);
        
        // Then: Should be found with normalized input
        assertTrue(cacheService.isWordValidated("dog", Category.ANIMAL),
                  "Should find normalized word");
        assertTrue(cacheService.isWordValidated("DOG", Category.ANIMAL),
                  "Should find word regardless of case");
        assertTrue(cacheService.isWordValidated("  dog  ", Category.ANIMAL),
                  "Should find word regardless of whitespace");
    }
    
    @Test
    void testSaveValidatedWord_UniqueConstraint() throws SQLException {
        // When: Save same word twice
        cacheService.saveValidatedWord("cat", Category.ANIMAL);
        cacheService.saveValidatedWord("cat", Category.ANIMAL); // Duplicate
        
        // Then: Should not throw exception (INSERT OR IGNORE)
        assertTrue(cacheService.isWordValidated("cat", Category.ANIMAL),
                  "Word should still be in cache after duplicate insert");
    }
}