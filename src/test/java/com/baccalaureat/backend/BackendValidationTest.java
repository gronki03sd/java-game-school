package com.baccalaureat.backend;

import com.baccalaureat.dao.CategoryDAO;
import com.baccalaureat.dao.DatabaseManager;
import com.baccalaureat.model.Category;
import com.baccalaureat.service.CategoryService;
import org.junit.jupiter.api.*;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Backend validation tests for automatic database seeding.
 * Tests verify SQLite connectivity, schema creation, automatic seeding,
 * and predefined vs custom category management.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BackendValidationTest {

    private static final String TEST_DB_PATH = "test_validation.db";
    private CategoryDAO categoryDAO;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        // Set test database
        System.setProperty("database.url", "jdbc:sqlite:" + TEST_DB_PATH);
        
        // Initialize database (this should trigger seeding)
        DatabaseManager.initializeDatabase();
        
        // Initialize DAO and Service
        categoryDAO = new CategoryDAO();
        categoryService = new CategoryService();
    }

    @AfterEach
    void tearDown() {
        // Close any open connections
        try {
            DatabaseManager.getConnection().close();
        } catch (Exception e) {
            // Ignore
        }
        
        // Clean up test database
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            // Wait a bit for file locks to release
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            dbFile.delete();
        }
        System.clearProperty("database.url");
    }

    @Test
    @Order(1)
    @DisplayName("1. SQLite JDBC driver should load successfully")
    void testSqliteDriverLoads() {
        assertDoesNotThrow(() -> {
            Class.forName("org.sqlite.JDBC");
        }, "SQLite JDBC driver should be available");
    }

    @Test
    @Order(2)
    @DisplayName("2. Categories table should exist with correct schema")
    void testCategoriesTableSchema() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Check table exists
            ResultSet tables = conn.getMetaData().getTables(null, null, "categories", null);
            assertTrue(tables.next(), "Categories table should exist");

            // Check required columns
            ResultSet columns = conn.getMetaData().getColumns(null, null, "categories", null);
            
            boolean hasId = false;
            boolean hasName = false;
            boolean hasPredefined = false;
            
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                switch (columnName.toLowerCase()) {
                    case "id":
                        hasId = true;
                        break;
                    case "name":
                        hasName = true;
                        break;
                    case "predefined":
                        hasPredefined = true;
                        break;
                }
            }
            
            assertTrue(hasId, "Table should have 'id' column");
            assertTrue(hasName, "Table should have 'name' column");
            assertTrue(hasPredefined, "Table should have 'predefined' column");
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. Database should automatically seed exactly 10 predefined categories")
    void testAutomaticSeeding() {
        List<Category> allCategories = categoryService.getAllCategories();
        
        // Count predefined categories
        long predefinedCount = allCategories.stream()
                .filter(Category::isPredefined)
                .count();
        
        assertEquals(10, predefinedCount, "Should have exactly 10 predefined categories");
        
        // Verify specific categories exist
        List<String> expectedCategories = Arrays.asList(
            "PRENOM", "VILLE", "PAYS", "ANIMAL", 
            "FRUIT", "LEGUME", "METIER", "COULEUR", 
            "OBJET", "MARQUE"
        );
        
        for (String expected : expectedCategories) {
            boolean found = allCategories.stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(expected) && c.isPredefined());
            assertTrue(found, "Predefined category '" + expected + "' should exist");
        }
    }

    @Test
    @Order(4)
    @DisplayName("4. Seeding should be idempotent - no duplicates on multiple initializations")
    void testIdempotentSeeding() {
        // Get initial count
        List<Category> initialCategories = categoryService.getAllCategories();
        int initialCount = initialCategories.size();
        long initialPredefinedCount = initialCategories.stream()
                .filter(Category::isPredefined)
                .count();
        
        // Re-initialize database (simulates app restart)
        DatabaseManager.initializeDatabase();
        
        // Check counts haven't changed
        List<Category> afterReinit = categoryService.getAllCategories();
        assertEquals(initialCount, afterReinit.size(), 
                "Category count should not change after re-initialization");
        
        long afterPredefinedCount = afterReinit.stream()
                .filter(Category::isPredefined)
                .count();
        assertEquals(initialPredefinedCount, afterPredefinedCount,
                "Predefined category count should remain 10 after re-initialization");
    }

    @Test
    @Order(5)
    @DisplayName("5. Predefined categories cannot be deleted")
    void testPredefinedCategoriesCannotBeDeleted() {
        // Get a predefined category
        Category predefined = categoryService.getAllCategories().stream()
                .filter(Category::isPredefined)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Should have predefined categories"));
        
        // Attempt to delete should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            categoryDAO.deleteCategory(predefined.getId());
        }, "Deleting predefined category should throw IllegalArgumentException");
        
        // Verify category still exists
        Category stillExists = categoryDAO.getCategoryById(predefined.getId());
        assertNotNull(stillExists, "Predefined category should still exist after deletion attempt");
    }

    @Test
    @Order(6)
    @DisplayName("6. Predefined categories cannot be renamed")
    void testPredefinedCategoriesCannotBeRenamed() {
        // Get a predefined category
        Category predefined = categoryService.getAllCategories().stream()
                .filter(Category::isPredefined)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Should have predefined categories"));
        
        String originalName = predefined.getName();
        
        // Create modified category with new name
        Category modified = new Category(
                predefined.getId(),
                "MODIFIED_NAME",
                "Modified Display",
                predefined.getIcon(),
                predefined.getHint(),
                predefined.isEnabled(),
                predefined.isPredefined(),
                predefined.getCreatedAt()
        );
        
        // Attempt to rename should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            categoryDAO.updateCategory(modified);
        }, "Renaming predefined category should throw IllegalArgumentException");
        
        // Verify name hasn't changed
        Category unchanged = categoryDAO.getCategoryById(predefined.getId());
        assertEquals(originalName, unchanged.getName(), 
                "Predefined category name should remain unchanged");
    }

    @Test
    @Order(7)
    @DisplayName("7. Custom categories can be added, edited, and deleted")
    void testCustomCategoryOperations() {
        // CREATE: Add custom category with unique name
        String uniqueName = "CUSTOM_" + System.currentTimeMillis();
        Category newCategory = new Category(
                0,
                uniqueName,
                "Custom Test Category",
                "🧪",
                "Test hint",
                true,
                false,
                null
        );
        
        Category created = categoryDAO.createCategory(newCategory);
        assertNotNull(created, "Custom category should be created");
        assertFalse(created.isPredefined(), "Custom category should not be predefined");
        assertTrue(created.getId() > 0, "Created category should have valid ID");
        
        // UPDATE: Edit custom category (display name, icon, hint - not internal name)
        assertDoesNotThrow(() -> {
            categoryDAO.updateCategory(created.getId(), "Updated Display", "🔄", "Updated hint");
        }, "Updating custom category should succeed");
        
        Category retrieved = categoryDAO.getCategoryById(created.getId());
        assertEquals("Updated Display", retrieved.getDisplayName(), "Display name should be updated");
        assertEquals("🔄", retrieved.getIcon(), "Icon should be updated");
        
        // DELETE: Remove custom category
        assertDoesNotThrow(() -> {
            categoryDAO.deleteCategory(created.getId());
        }, "Deleting custom category should succeed");
        
        // Verify deletion (soft delete means it still exists but disabled)
        Category afterDelete = categoryDAO.getCategoryById(created.getId());
        if (afterDelete != null) {
            assertFalse(afterDelete.isEnabled(), "Deleted category should be disabled");
        }
    }

    @Test
    @Order(8)
    @DisplayName("8. CategoryDAO enforces predefined vs custom rules")
    void testCategoryDAOEnforcement() {
        // Get one predefined and verify it can't be modified
        Category predefined = categoryService.getAllCategories().stream()
                .filter(Category::isPredefined)
                .findFirst()
                .orElseThrow();
        
        // Test delete enforcement
        Exception deleteEx = assertThrows(IllegalArgumentException.class, () -> {
            categoryDAO.deleteCategory(predefined.getId());
        });
        assertTrue(deleteEx.getMessage().contains("predefined") || 
                   deleteEx.getMessage().contains("cannot be deleted"),
                "Exception should mention predefined protection");
        
        // Test rename enforcement
        Category renamed = new Category(
                predefined.getId(),
                "NEW_NAME",
                predefined.getDisplayName(),
                predefined.getIcon(),
                predefined.getHint(),
                predefined.isEnabled(),
                predefined.isPredefined(),
                predefined.getCreatedAt()
        );
        
        Exception renameEx = assertThrows(IllegalArgumentException.class, () -> {
            categoryDAO.updateCategory(renamed);
        });
        assertTrue(renameEx.getMessage().contains("predefined") || 
                   renameEx.getMessage().contains("cannot be renamed"),
                "Exception should mention predefined protection");
    }

    @Test
    @Order(9)
    @DisplayName("9. CategoryService exposes only safe operations")
    void testCategoryServiceSafety() {
        // Verify service returns all categories including predefined
        List<Category> allCategories = categoryService.getAllCategories();
        assertTrue(allCategories.size() >= 10, "Should have at least 10 categories");
        
        // Verify predefined categories are included
        long predefinedCount = allCategories.stream()
                .filter(Category::isPredefined)
                .count();
        assertEquals(10, predefinedCount, "Service should return all 10 predefined categories");
        
        // Verify service can create custom categories safely
        // Use timestamp to ensure unique name
        String uniqueName = "SVC_" + System.currentTimeMillis();
        CategoryService.CategoryCreationResult result = categoryService.createCategory(
                uniqueName, "Service Test", "🔧", "Test");
        
        if (!result.isSuccess()) {
            System.out.println("Creation failed: " + result.getErrorMessage());
        }
        assertTrue(result.isSuccess(), "Service should allow creating custom categories: " + result.getErrorMessage());
        
        // Verify the custom category was created correctly
        Category serviceCreated = categoryService.getAllCategories().stream()
                .filter(c -> c.getName().equals(uniqueName))
                .findFirst()
                .orElse(null);
        
        assertNotNull(serviceCreated, "Service-created category should exist");
        assertFalse(serviceCreated.isPredefined(), "Service-created category should not be predefined");
    }

    @Test
    @Order(10)
    @DisplayName("10. Database state is consistent after all operations")
    void testDatabaseConsistency() {
        List<Category> allCategories = categoryService.getAllCategories();
        
        // Verify all categories have required fields
        for (Category category : allCategories) {
            assertNotNull(category.getId(), "Category should have ID");
            assertNotNull(category.getName(), "Category should have name");
            assertFalse(category.getName().trim().isEmpty(), "Category name should not be empty");
            assertNotNull(category.getDisplayName(), "Category should have display name");
            assertNotNull(category.getCreatedAt(), "Category should have creation timestamp");
        }
        
        // Verify predefined categories have full metadata
        allCategories.stream()
                .filter(Category::isPredefined)
                .forEach(category -> {
                    assertNotNull(category.getIcon(), "Predefined category should have icon");
                    assertNotNull(category.getHint(), "Predefined category should have hint");
                    assertTrue(category.isEnabled(), "Predefined category should be enabled");
                });
        
        // Verify exactly 10 predefined categories
        long predefinedCount = allCategories.stream()
                .filter(Category::isPredefined)
                .count();
        assertEquals(10, predefinedCount, "Should maintain exactly 10 predefined categories");
    }
}
