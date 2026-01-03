package com.baccalaureat.service;

import com.baccalaureat.dao.CategoryDAO;
import com.baccalaureat.dao.DatabaseManager;
import com.baccalaureat.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for category management.
 * Handles business logic and validation for dynamic categories.
 * Controllers should interact with this service, not the DAO directly.
 */
public class CategoryService {
    
    private final CategoryDAO categoryDAO;
    
    public CategoryService() {
        this.categoryDAO = new CategoryDAO();
    }
    
    /**
     * Constructor for dependency injection (testing).
     */
    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }
    
    /**
     * Gets all enabled categories for game use.
     */
    public List<Category> getEnabledCategories() {
        return categoryDAO.getAllEnabledCategories();
    }
    
    /**
     * Gets all categories (enabled and disabled) for management UI.
     */
    public List<Category> getAllCategories() {
        return categoryDAO.getAllCategories();
    }
    
    /**
     * Finds a category by its internal name.
     * Used by validation system for category resolution.
     */
    public Optional<Category> findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return categoryDAO.findByName(name.trim().toUpperCase());
    }
    
    /**
     * Finds a category by ID.
     */
    public Optional<Category> findById(int id) {
        return categoryDAO.findById(id);
    }
    
    /**
     * Creates a new category with validation.
     */
    public CategoryCreationResult createCategory(String name, String displayName, String icon, String hint) {
        // Validate input
        if (name == null || name.trim().isEmpty()) {
            return CategoryCreationResult.error("Category name cannot be empty");
        }
        
        if (displayName == null || displayName.trim().isEmpty()) {
            return CategoryCreationResult.error("Display name cannot be empty");
        }
        
        // Normalize name (uppercase, alphanumeric only)
        String normalizedName = normalizeInternalName(name);
        if (normalizedName.isEmpty()) {
            return CategoryCreationResult.error("Category name must contain at least one letter or number");
        }
        
        // Check for duplicates
        if (categoryDAO.findByName(normalizedName).isPresent()) {
            return CategoryCreationResult.error("A category with this name already exists");
        }
        
        // Create category
        Optional<Category> created = categoryDAO.createCategory(
            normalizedName, 
            displayName.trim(), 
            icon != null ? icon.trim() : "📝", 
            hint != null ? hint.trim() : "Une catégorie personnalisée"
        );
        
        if (created.isPresent()) {
            return CategoryCreationResult.success(created.get());
        } else {
            return CategoryCreationResult.error("Failed to create category in database");
        }
    }
    
    /**
     * Creates a new predefined category (for system initialization).
     * Predefined categories cannot be deleted by users.
     */
    public CategoryCreationResult createPredefinedCategory(String name, String displayName, String icon, String hint) {
        // Validate input
        if (name == null || name.trim().isEmpty()) {
            return CategoryCreationResult.error("Category name cannot be empty");
        }
        
        if (displayName == null || displayName.trim().isEmpty()) {
            return CategoryCreationResult.error("Display name cannot be empty");
        }
        
        // Normalize name (uppercase, alphanumeric only)
        String normalizedName = normalizeInternalName(name);
        if (normalizedName.isEmpty()) {
            return CategoryCreationResult.error("Category name must contain at least one letter or number");
        }
        
        // Check for duplicates
        if (categoryDAO.findByName(normalizedName).isPresent()) {
            return CategoryCreationResult.error("A category with this name already exists");
        }
        
        // Create predefined category
        Optional<Category> created = categoryDAO.createCategory(
            normalizedName, 
            displayName.trim(), 
            icon != null ? icon.trim() : "📝", 
            hint != null ? hint.trim() : "Une catégorie prédéfinie",
            true  // Mark as predefined
        );
        
        if (created.isPresent()) {
            return CategoryCreationResult.success(created.get());
        } else {
            return CategoryCreationResult.error("Failed to create predefined category in database");
        }
    }
    
    /**
     * Create a category from Category object (for user categories).
     */
    public CategoryCreationResult createCategory(Category category) {
        return createCategory(
            category.getName(),
            category.getDisplayName(),
            category.getIcon(),
            category.getHint()
        );
    }
    
    /**
     * Create a predefined category from Category object (for system use).
     */
    public CategoryCreationResult createPredefinedCategory(Category category) {
        return createPredefinedCategory(
            category.getName(),
            category.getDisplayName(),
            category.getIcon(),
            category.getHint()
        );
    }
    
    /**
     * Updates an existing category.
     */
    public CategoryUpdateResult updateCategory(int id, String displayName, String icon, String hint) {
        System.out.println("[CategoryService] Updating category ID " + id + ": displayName='" + displayName + "'");
        
        // Validate input
        if (displayName == null || displayName.trim().isEmpty()) {
            System.err.println("[CategoryService] Update failed: empty display name");
            return CategoryUpdateResult.error("Display name cannot be empty");
        }
        
        // Check if category exists
        if (categoryDAO.findById(id).isEmpty()) {
            System.err.println("[CategoryService] Update failed: category not found (ID: " + id + ")");
            return CategoryUpdateResult.error("Category not found");
        }
        
        // Update category
        boolean updated = categoryDAO.updateCategory(
            id, 
            displayName.trim(), 
            icon != null ? icon.trim() : "📝", 
            hint != null ? hint.trim() : "Une catégorie personnalisée"
        );
        
        if (updated) {
            Optional<Category> updatedCategory = categoryDAO.findById(id);
            return updatedCategory.map(CategoryUpdateResult::success)
                                 .orElse(CategoryUpdateResult.error("Failed to retrieve updated category"));
        } else {
            return CategoryUpdateResult.error("Failed to update category in database");
        }
    }
    
    /**
     * Renames a category (updates internal name).
     * This is more complex as it affects validation references.
     */
    public CategoryUpdateResult renameCategory(int id, String newName, String displayName, String icon, String hint) {
        // Validate input
        if (newName == null || newName.trim().isEmpty()) {
            return CategoryUpdateResult.error("Category name cannot be empty");
        }
        
        if (displayName == null || displayName.trim().isEmpty()) {
            return CategoryUpdateResult.error("Display name cannot be empty");
        }
        
        // Normalize new name
        String normalizedName = normalizeInternalName(newName);
        if (normalizedName.isEmpty()) {
            return CategoryUpdateResult.error("Category name must contain at least one letter or number");
        }
        
        // Check if category exists
        Optional<Category> existing = categoryDAO.findById(id);
        if (existing.isEmpty()) {
            return CategoryUpdateResult.error("Category not found");
        }
        
        // Check for name conflicts (unless it's the same name)
        if (!existing.get().getName().equals(normalizedName)) {
            if (categoryDAO.findByName(normalizedName).isPresent()) {
                return CategoryUpdateResult.error("A category with this name already exists");
            }
        }
        
        // Update category with new name
        boolean updated = categoryDAO.updateCategoryWithName(
            id, 
            normalizedName, 
            displayName.trim(), 
            icon != null ? icon.trim() : "📝", 
            hint != null ? hint.trim() : "Une catégorie personnalisée"
        );
        
        if (updated) {
            Optional<Category> updatedCategory = categoryDAO.findById(id);
            return updatedCategory.map(CategoryUpdateResult::success)
                                 .orElse(CategoryUpdateResult.error("Failed to retrieve updated category"));
        } else {
            return CategoryUpdateResult.error("Failed to rename category in database");
        }
    }
    
    /**
     * Enables a category (sets enabled = true).
     */
    public boolean enableCategory(int id) {
        System.out.println("[CategoryService] Enabling category ID: " + id);
        
        // Check current state before
        Optional<Category> before = categoryDAO.findById(id);
        System.out.println("[CategoryService] Before enable - Category enabled: " + 
            (before.isPresent() ? before.get().isEnabled() : "NOT_FOUND"));
        
        boolean result = categoryDAO.restoreCategory(id);
        
        // Check state after
        if (result) {
            Optional<Category> after = categoryDAO.findById(id);
            System.out.println("[CategoryService] After enable - Category enabled: " + 
                (after.isPresent() ? after.get().isEnabled() : "NOT_FOUND"));
        }
        
        System.out.println("[CategoryService] Enable result: " + (result ? "SUCCESS" : "FAILED"));
        return result;
    }
    
    /**
     * Disables a category (sets enabled = false).
     * This is NOT a delete - the category remains in the database.
     */
    public boolean disableCategory(int id) {
        System.out.println("[CategoryService] Disabling category ID: " + id);
        
        // Check current state before
        Optional<Category> before = categoryDAO.findById(id);
        System.out.println("[CategoryService] Before disable - Category enabled: " + 
            (before.isPresent() ? before.get().isEnabled() : "NOT_FOUND"));
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE categories SET enabled = false WHERE id = ?")) {
            
            stmt.setInt(1, id);
            int rowsUpdated = stmt.executeUpdate();
            boolean result = rowsUpdated > 0;
            System.out.println("[CategoryService] Rows updated: " + rowsUpdated);
            
            // Check state after
            if (result) {
                Optional<Category> after = categoryDAO.findById(id);
                System.out.println("[CategoryService] After disable - Category enabled: " + 
                    (after.isPresent() ? after.get().isEnabled() : "NOT_FOUND"));
            }
            
            System.out.println("[CategoryService] Disable result: " + (result ? "SUCCESS" : "FAILED"));
            return result;
            
        } catch (SQLException e) {
            System.err.println("[CategoryService] Error disabling category: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Permanently deletes a custom category from the database.
     * Predefined categories cannot be deleted.
     */
    public boolean deleteCategory(int id) {
        System.out.println("[CategoryService] Deleting category ID: " + id);
        boolean result = categoryDAO.deleteCategory(id);
        System.out.println("[CategoryService] Delete result: " + (result ? "SUCCESS" : "FAILED"));
        return result;
    }
    
    /**
     * Restores a previously deleted category.
     */
    public boolean restoreCategory(int id) {
        System.out.println("[CategoryService] Restoring category ID: " + id);
        boolean result = categoryDAO.restoreCategory(id);
        System.out.println("[CategoryService] Restore result: " + (result ? "SUCCESS" : "FAILED"));
        return result;
    }
    
    /**
     * Normalizes internal category name for consistency.
     */
    private String normalizeInternalName(String name) {
        if (name == null) return "";
        
        // Convert to uppercase, remove spaces and special characters, keep only alphanumeric
        return name.trim()
                  .toUpperCase()
                  .replaceAll("[^A-Z0-9]", "_")
                  .replaceAll("_+", "_")
                  .replaceAll("^_|_$", "");
    }
    
    // Result classes for operation feedback
    public static class CategoryCreationResult {
        private final boolean success;
        private final Category category;
        private final String errorMessage;
        
        private CategoryCreationResult(boolean success, Category category, String errorMessage) {
            this.success = success;
            this.category = category;
            this.errorMessage = errorMessage;
        }
        
        public static CategoryCreationResult success(Category category) {
            return new CategoryCreationResult(true, category, null);
        }
        
        public static CategoryCreationResult error(String message) {
            return new CategoryCreationResult(false, null, message);
        }
        
        public boolean isSuccess() { return success; }
        public Category getCategory() { return category; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class CategoryUpdateResult {
        private final boolean success;
        private final Category category;
        private final String errorMessage;
        
        private CategoryUpdateResult(boolean success, Category category, String errorMessage) {
            this.success = success;
            this.category = category;
            this.errorMessage = errorMessage;
        }
        
        public static CategoryUpdateResult success(Category category) {
            return new CategoryUpdateResult(true, category, null);
        }
        
        public static CategoryUpdateResult error(String message) {
            return new CategoryUpdateResult(false, null, message);
        }
        
        public boolean isSuccess() { return success; }
        public Category getCategory() { return category; }
        public String getErrorMessage() { return errorMessage; }
    }
}