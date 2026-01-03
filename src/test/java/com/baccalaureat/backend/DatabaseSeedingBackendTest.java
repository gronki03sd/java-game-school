package com.baccalaureat.backend;

import com.baccalaureat.dao.CategoryDAO;
import com.baccalaureat.dao.DatabaseManager;
import com.baccalaureat.model.Category;
import com.baccalaureat.service.CategoryService;
import com.baccalaureat.service.DatabaseInitializer;
import org.junit.jupiter.api.*;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive backend tests for automatic database seeding and predefined categories.
 * Tests SQLite connectivity, schema creation, automatic seeding, and category protection.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseSeedingBackendTest {

    private static final String TEST_DB = "test_seeding.db";
    private CategoryDAO categoryDAO;
    private CategoryService categoryService;
    private DatabaseInitializer initializer;

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
        initializer = new DatabaseInitializer();
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
    @DisplayName("SQLite JDBC driver should load successfully")
    void testSqliteDriverLoading() {
        assertDoesNotThrow(() -> {
            Class.forName("org.sqlite.JDBC");
        }, "SQLite JDBC driver should be available on classpath");
    }

    @Test
    @Order(2)
    @DisplayName("Categories table should exist with correct schema")
    void testCategoriesTableSchema() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Check if table exists
            String checkTableSql = "SELECT name FROM sqlite_master WHERE type='table' AND name='categories'";
            try (PreparedStatement stmt = conn.prepareStatement(checkTableSql);
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Categories table should exist");
                assertEquals("categories", rs.getString("name"));
            }

            // Check table schema
            String schemaSql = "PRAGMA table_info(categories)";
            try (PreparedStatement stmt = conn.prepareStatement(schemaSql);
                 ResultSet rs = stmt.executeQuery()) {
                
                boolean hasId = false, hasName = false, hasPredefined = false;
                
                while (rs.next()) {
                    String columnName = rs.getString("name");
                    String columnType = rs.getString("type");
                    
                    switch (columnName) {
                        case "id" -> {
                            hasId = true;
                            assertTrue(columnType.contains("INTEGER"), "ID should be INTEGER");
                        }
                        case "name" -> {
                            hasName = true;
                            assertTrue(columnType.contains("TEXT"), "Name should be TEXT");
                        }
                        case "predefined" -> {
                            hasPredefined = true;
                            assertTrue(columnType.contains("BOOLEAN"), "Predefined should be BOOLEAN");
                        }
                    }
                }
                
                assertTrue(hasId, "Categories table should have 'id' column");
                assertTrue(hasName, "Categories table should have 'name' column");
                assertTrue(hasPredefined, "Categories table should have 'predefined' column");
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("Database should automatically seed exactly 10 predefined categories")
    void testAutomaticSeeding() throws Exception {
        // Categories should be seeded automatically during database initialization
        List<Category> allCategories = categoryDAO.getAllCategories();
        
        // Filter predefined categories
        List<Category> predefinedCategories = allCategories.stream()
            .filter(Category::isPredefined)
            .toList();
        
        assertEquals(10, predefinedCategories.size(), "Should have exactly 10 predefined categories");
        
        // Check all required predefined categories exist
        String[] expectedCategories = {
            "PRENOM", "VILLE", "PAYS", "ANIMAL", "FRUIT",
            "LEGUME", "METIER", "COULEUR", "OBJET", "MARQUE"
        };
        
        for (String expected : expectedCategories) {
            boolean found = predefinedCategories.stream()
                .anyMatch(cat -> cat.getName().equals(expected));
            assertTrue(found, "Predefined category '" + expected + "' should exist");
        }
        
        // Verify they are marked as predefined
        for (Category category : predefinedCategories) {
            assertTrue(category.isPredefined(), 
                "Category '" + category.getName() + "' should be marked as predefined");
            assertNotNull(category.getDisplayName(), 
                "Predefined category should have display name");
            assertNotNull(category.getIcon(), 
                "Predefined category should have icon");
        }
    }

    @Test
    @Order(4)
    @DisplayName("Seeding should be idempotent - no duplicates on multiple runs")
    void testIdempotentSeeding() throws Exception {
        // Get initial count
        List<Category> initialCategories = categoryDAO.getAllCategories();
        int initialCount = initialCategories.size();
        long initialPredefinedCount = initialCategories.stream()
            .filter(Category::isPredefined)
            .count();
        
        // Run initialization again
        initializer.initializeDatabase();
        
        // Check counts remain the same
        List<Category> afterSecondRun = categoryDAO.getAllCategories();
        assertEquals(initialCount, afterSecondRun.size(), 
            "Total category count should not change after second initialization");
        
        long finalPredefinedCount = afterSecondRun.stream()
            .filter(Category::isPredefined)
            .count();
        assertEquals(initialPredefinedCount, finalPredefinedCount,
            "Predefined category count should not change after second initialization");
    }

    @Test
    @Order(5)
    @DisplayName("Predefined categories cannot be deleted")
    void testPredefinedCategoryProtection() throws Exception {
        // Get a predefined category
        List<Category> predefinedCategories = categoryDAO.getAllCategories().stream()
            .filter(Category::isPredefined)
            .toList();
        
        assertFalse(predefinedCategories.isEmpty(), "Should have predefined categories for testing");
        
        Category predefinedCategory = predefinedCategories.get(0);
        
        // Attempt to delete should fail
        assertThrows(IllegalArgumentException.class, () -> {
            categoryDAO.deleteCategory(predefinedCategory.getId());
        }, "Deleting predefined category should throw IllegalArgumentException");
        
        // Verify category still exists
        Category stillExists = categoryDAO.getCategoryById(predefinedCategory.getId());
        assertNotNull(stillExists, "Predefined category should still exist after failed deletion");
    }

    @Test
    @Order(6)
    @DisplayName("Predefined categories cannot be renamed")
    void testPredefinedCategoryRenameProtection() throws Exception {
        // Get a predefined category
        List<Category> predefinedCategories = categoryDAO.getAllCategories().stream()
            .filter(Category::isPredefined)
            .toList();
        
        assertFalse(predefinedCategories.isEmpty(), "Should have predefined categories for testing");
        
        Category predefinedCategory = predefinedCategories.get(0);
        String originalName = predefinedCategory.getName();
        
        // Create modified category
        Category modifiedCategory = new Category(
            predefinedCategory.getId(),
            "MODIFIED_NAME",  // Different name
            predefinedCategory.getDisplayName(),
            predefinedCategory.getIcon(),
            predefinedCategory.getHint(),
            predefinedCategory.isEnabled(),
            predefinedCategory.isPredefined(),
            predefinedCategory.getCreatedAt()
        );
        
        // Attempt to update should fail
        assertThrows(IllegalArgumentException.class, () -> {
            categoryDAO.updateCategory(modifiedCategory);
        }, "Updating predefined category should throw IllegalArgumentException");
        
        // Verify category name unchanged
        Category unchanged = categoryDAO.getCategoryById(predefinedCategory.getId());
        assertEquals(originalName, unchanged.getName(), 
            "Predefined category name should remain unchanged");
    }

    @Test
    @Order(7)
    @DisplayName("Custom categories can be created, updated, and deleted")
    void testCustomCategoryOperations() throws Exception {
        // Create a custom category
        Category customCategory = new Category(
            "CUSTOM_TEST",
            "Custom Test",
            "🧪",
            "A test category",
            true,
            false  // Not predefined
        );
        
        // Create category
        Category created = categoryDAO.createCategory(customCategory);
        assertNotNull(created, "Custom category should be created");
        assertNotNull(created.getId(), "Created category should have ID");
        assertFalse(created.isPredefined(), "Custom category should not be marked as predefined");
        
        // Update category
        Category updated = new Category(
            created.getId(),
            "CUSTOM_UPDATED",
            "Updated Custom",
            "🔄",
            "Updated test category",
            true,
            false,
            created.getCreatedAt()
        );
        
        assertDoesNotThrow(() -> {
            categoryDAO.updateCategory(updated);
        }, "Updating custom category should succeed");
        
        // Verify update
        Category retrieved = categoryDAO.getCategoryById(created.getId());
        assertEquals("CUSTOM_UPDATED", retrieved.getName(), "Custom category name should be updated");
        assertEquals("Updated Custom", retrieved.getDisplayName(), "Display name should be updated");
        
        // Delete category (soft delete - sets enabled = false)
        assertDoesNotThrow(() -> {
            categoryDAO.deleteCategory(created.getId());
        }, "Deleting custom category should succeed");
        
        // Verify soft deletion (category still exists but is disabled)
        Category softDeleted = categoryDAO.getCategoryById(created.getId());
        assertNotNull(softDeleted, "Soft-deleted custom category should still exist in database");
        assertFalse(softDeleted.isEnabled(), "Soft-deleted custom category should be disabled");
        
        // Verify it doesn't appear in enabled categories list
        List<Category> enabledCategories = categoryDAO.getAllEnabledCategories();
        boolean foundInEnabled = enabledCategories.stream()
            .anyMatch(cat -> cat.getId() == created.getId());
        assertFalse(foundInEnabled, "Soft-deleted category should not appear in enabled categories");
    }

    @Test
    @Order(8)
    @DisplayName("CategoryService should provide safe operations")
    void testCategoryServiceSafety() throws Exception {
        // Test predefined category creation (system use)
        Category predefinedCategory = new Category(
            "SYSTEM_TEST",
            "System Test",
            "⚙️",
            "System test category",
            true,
            true  // Predefined
        );
        
        assertDoesNotThrow(() -> {
            categoryService.createPredefinedCategory(predefinedCategory);
        }, "CategoryService should allow predefined category creation");
        
        // Test regular category creation (user use)
        Category userCategory = new Category(
            "USER_TEST",
            "User Test",
            "👤",
            "User test category",
            true,
            false  // Not predefined
        );
        
        assertDoesNotThrow(() -> {
            categoryService.createCategory(userCategory);
        }, "CategoryService should allow user category creation");
        
        // Verify both exist
        List<Category> allCategories = categoryService.getAllCategories();
        
        boolean foundSystem = allCategories.stream()
            .anyMatch(cat -> cat.getName().equals("SYSTEM_TEST") && cat.isPredefined());
        boolean foundUser = allCategories.stream()
            .anyMatch(cat -> cat.getName().equals("USER_TEST") && !cat.isPredefined());
        
        assertTrue(foundSystem, "System-created predefined category should exist");
        assertTrue(foundUser, "User-created category should exist and not be predefined");
    }

    @Test
    @Order(9)
    @DisplayName("Database state should be consistent after all operations")
    void testDatabaseConsistency() throws Exception {
        List<Category> allCategories = categoryDAO.getAllCategories();
        
        // Should have at least 10 predefined categories
        long predefinedCount = allCategories.stream()
            .filter(Category::isPredefined)
            .count();
        assertTrue(predefinedCount >= 10, 
            "Should have at least 10 predefined categories after all tests");
        
        // All categories should have required fields
        for (Category category : allCategories) {
            assertNotNull(category.getId(), "Category should have ID");
            assertNotNull(category.getName(), "Category should have name");
            assertFalse(category.getName().trim().isEmpty(), "Category name should not be empty");
            assertNotNull(category.getDisplayName(), "Category should have display name");
            assertNotNull(category.getCreatedAt(), "Category should have creation timestamp");
        }
        
        // Predefined categories should have all required metadata
        List<Category> predefined = allCategories.stream()
            .filter(Category::isPredefined)
            .toList();
        
        for (Category category : predefined) {
            assertNotNull(category.getIcon(), "Predefined category should have icon");
            assertNotNull(category.getHint(), "Predefined category should have hint");
            assertTrue(category.isEnabled(), "Predefined category should be enabled");
        }
    }
}