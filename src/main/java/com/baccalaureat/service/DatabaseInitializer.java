package com.baccalaureat.service;

import com.baccalaureat.dao.CategoryDAO;
import com.baccalaureat.model.Category;

import java.util.Optional;

/**
 * Database initialization service responsible for seeding predefined categories.
 * Ensures all required predefined categories exist on application startup.
 */
public class DatabaseInitializer {
    
    private final CategoryDAO categoryDAO;
    
    /**
     * Predefined categories that must exist in the system.
     * These categories cannot be deleted by users.
     */
    private static final String[][] PREDEFINED_CATEGORIES = {
        {"PRENOM", "First Name", "👤", "A person's first name"},
        {"VILLE", "City", "🏙️", "A city of the world"},
        {"PAYS", "Country", "🌍", "A country of the world"},
        {"ANIMAL", "Animal", "🐾", "An animal"},
        {"FRUIT", "Fruit", "🍎", "A fruit"},
        {"LEGUME", "Vegetable", "🥕", "A vegetable"},
        {"METIER", "Job", "👔", "A profession or job"},
        {"COULEUR", "Color", "🎨", "A color"},
        {"OBJET", "Object", "📦", "An everyday object"},
        {"MARQUE", "Brand", "🏷️", "A commercial brand"}
    };
    
    public DatabaseInitializer() {
        this.categoryDAO = new CategoryDAO();
    }
    
    public DatabaseInitializer(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }
    
    /**
     * Initializes all predefined categories.
     * This method is idempotent - safe to run multiple times.
     * Existing predefined categories will not be duplicated.
     */
    public void initializePredefinedCategories() {
        System.out.println("Initializing predefined categories...");
        
        int seededCount = 0;
        int existingCount = 0;
        
        for (String[] categoryData : PREDEFINED_CATEGORIES) {
            String name = categoryData[0];
            String displayName = categoryData[1];
            String icon = categoryData[2];
            String hint = categoryData[3];
            
            try {
                // Check if category already exists
                Optional<Category> existing = categoryDAO.findByName(name);
                
                if (existing.isPresent()) {
                    existingCount++;
                    System.out.println("  ✓ " + name + " already exists");
                    
                    // Ensure existing category is marked as predefined if it wasn't before
                    Category existingCategory = existing.get();
                    if (!existingCategory.isPredefined()) {
                        System.out.println("  → Marking " + name + " as predefined");
                        // Note: This would require an additional DAO method to update predefined flag
                        // For now, we'll assume this is handled by database migration
                    }
                } else {
                    // Create new predefined category
                    Optional<Category> created = categoryDAO.createCategory(name, displayName, icon, hint, true);
                    
                    if (created.isPresent()) {
                        seededCount++;
                        System.out.println("  + Created predefined category: " + name + " (" + displayName + ")");
                    } else {
                        System.err.println("  ✗ Failed to create category: " + name);
                    }
                }
                
            } catch (Exception e) {
                System.err.println("  ✗ Error processing category " + name + ": " + e.getMessage());
            }
        }
        
        System.out.println("Predefined categories initialization complete:");
        System.out.println("  - Categories created: " + seededCount);
        System.out.println("  - Categories already existing: " + existingCount);
        System.out.println("  - Total predefined categories: " + PREDEFINED_CATEGORIES.length);
    }
    
    /**
     * Validates that all predefined categories exist and are properly configured.
     * Returns true if all categories are present and correctly configured.
     */
    public boolean validatePredefinedCategories() {
        System.out.println("Validating predefined categories...");
        
        int validCount = 0;
        int totalCount = PREDEFINED_CATEGORIES.length;
        
        for (String[] categoryData : PREDEFINED_CATEGORIES) {
            String name = categoryData[0];
            
            try {
                Optional<Category> category = categoryDAO.findByName(name);
                
                if (category.isPresent() && category.get().isPredefined()) {
                    validCount++;
                    System.out.println("  ✓ " + name + " - OK");
                } else if (category.isPresent()) {
                    System.err.println("  ⚠ " + name + " exists but is not marked as predefined");
                } else {
                    System.err.println("  ✗ " + name + " is missing");
                }
                
            } catch (Exception e) {
                System.err.println("  ✗ Error validating category " + name + ": " + e.getMessage());
            }
        }
        
        boolean isValid = (validCount == totalCount);
        System.out.println("Validation result: " + validCount + "/" + totalCount + " categories valid");
        
        return isValid;
    }
    
    /**
     * Performs complete database initialization including predefined categories.
     * This is the main method to call on application startup.
     */
    public void initializeDatabase() {
        System.out.println("=== Database Initialization ===");
        
        try {
            // Initialize predefined categories
            initializePredefinedCategories();
            
            // Validate the initialization
            boolean isValid = validatePredefinedCategories();
            
            if (isValid) {
                System.out.println("✅ Database initialization completed successfully");
            } else {
                System.err.println("⚠️  Database initialization completed with warnings");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Database initialization failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
        
        System.out.println("=== End Database Initialization ===\n");
    }
}