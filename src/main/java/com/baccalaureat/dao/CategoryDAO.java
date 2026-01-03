package com.baccalaureat.dao;

import com.baccalaureat.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Category operations.
 * Handles all database interactions for dynamic categories.
 */
public class CategoryDAO {
    
    private static final String SELECT_ALL_ENABLED = 
        "SELECT id, name, display_name, icon, hint, enabled, predefined, created_at " +
        "FROM categories WHERE enabled = true ORDER BY name";
    
    private static final String SELECT_ALL = 
        "SELECT id, name, display_name, icon, hint, enabled, predefined, created_at " +
        "FROM categories ORDER BY name";
    
    private static final String SELECT_BY_NAME = 
        "SELECT id, name, display_name, icon, hint, enabled, predefined, created_at " +
        "FROM categories WHERE name = ?";
    
    private static final String SELECT_BY_ID = 
        "SELECT id, name, display_name, icon, hint, enabled, predefined, created_at " +
        "FROM categories WHERE id = ?";
    
    private static final String INSERT_CATEGORY = 
        "INSERT INTO categories (name, display_name, icon, hint, predefined) VALUES (?, ?, ?, ?, ?)";
    
    private static final String UPDATE_CATEGORY = 
        "UPDATE categories SET display_name = ?, icon = ?, hint = ? WHERE id = ? AND predefined = false";
    
    private static final String UPDATE_CATEGORY_NAME = 
        "UPDATE categories SET name = ?, display_name = ?, icon = ?, hint = ? WHERE id = ? AND predefined = false";
    
    private static final String DELETE_CATEGORY = 
        "DELETE FROM categories WHERE id = ? AND predefined = false";
    
    private static final String RESTORE_CATEGORY = 
        "UPDATE categories SET enabled = true WHERE id = ?";
    
    /**
     * Gets all enabled categories from database.
     */
    public List<Category> getAllEnabledCategories() {
        List<Category> categories = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_ENABLED);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error retrieving enabled categories: " + e.getMessage());
        }
        
        return categories;
    }
    
    /**
     * Gets all categories (enabled and disabled) from database.
     */
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error retrieving all categories: " + e.getMessage());
        }
        
        return categories;
    }
    
    /**
     * Finds a category by its internal name.
     */
    public Optional<Category> findByName(String name) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_NAME)) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCategory(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding category by name: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Finds a category by its ID.
     */
    public Optional<Category> findById(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCategory(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding category by ID: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Get category by ID, returning null if not found (for backwards compatibility).
     */
    public Category getCategoryById(int id) {
        return findById(id).orElse(null);
    }
    
    /**
     * Creates a new category in the database.
     */
    public Optional<Category> createCategory(String name, String displayName, String icon, String hint) {
        return createCategory(name, displayName, icon, hint, false);
    }
    
    /**
     * Creates a new category in the database with predefined flag.
     */
    public Optional<Category> createCategory(String name, String displayName, String icon, String hint, boolean predefined) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_CATEGORY, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, name);
            stmt.setString(2, displayName);
            stmt.setString(3, icon);
            stmt.setString(4, hint);
            stmt.setBoolean(5, predefined);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return Optional.empty();
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return findById(id);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating category: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Create category from Category object.
     */
    public Category createCategory(Category category) {
        return createCategory(
            category.getName(),
            category.getDisplayName(),
            category.getIcon(),
            category.getHint(),
            category.isPredefined()
        ).orElse(null);
    }
    
    /**
     * Updates an existing category (without changing name).
     */
    public boolean updateCategory(int id, String displayName, String icon, String hint) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_CATEGORY)) {
            
            stmt.setString(1, displayName);
            stmt.setString(2, icon);
            stmt.setString(3, hint);
            stmt.setInt(4, id);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating category: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Updates category including name change.
     */
    public boolean updateCategoryWithName(int id, String name, String displayName, String icon, String hint) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_CATEGORY_NAME)) {
            
            stmt.setString(1, name);
            stmt.setString(2, displayName);
            stmt.setString(3, icon);
            stmt.setString(4, hint);
            stmt.setInt(5, id);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating category with name: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Permanently deletes a custom category from the database.
     * Predefined categories cannot be deleted.
     */
    public boolean deleteCategory(int id) {
        // Check if category is predefined
        Category category = getCategoryById(id);
        if (category != null && category.isPredefined()) {
            throw new IllegalArgumentException("Cannot delete predefined category: " + category.getName());
        }
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_CATEGORY)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting category: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Restores a soft-deleted category.
     */
    public boolean restoreCategory(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(RESTORE_CATEGORY)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error restoring category: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Updates a category object with predefined category protection.
     */
    public boolean updateCategory(Category category) {
        // Check if original category is predefined
        Category original = getCategoryById(category.getId());
        if (original != null && original.isPredefined()) {
            throw new IllegalArgumentException("Cannot update predefined category: " + original.getName());
        }
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_CATEGORY_NAME)) {
            
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getDisplayName());
            stmt.setString(3, category.getIcon());
            stmt.setString(4, category.getHint());
            stmt.setInt(5, category.getId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating category: " + e.getMessage());
            return false;
        }
    }

    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        return new Category(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("display_name"),
            rs.getString("icon"),
            rs.getString("hint"),
            rs.getBoolean("enabled"),
            rs.getBoolean("predefined"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}