package com.baccalaureat.demo;

import com.baccalaureat.dao.CategoryDAO;
import com.baccalaureat.model.Category;

import java.util.List;

/**
 * Simple demo to verify automatic database seeding works correctly.
 * This can be run as a standalone Java application to test the predefined category seeding.
 */
public class DatabaseSeedingDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Baccalauréat+ Database Seeding Demo ===\n");
        
        try {
            // DatabaseManager static initialization will automatically trigger:
            // 1. Database schema creation
            // 2. Predefined category seeding via DatabaseInitializer
            System.out.println("Initializing database and seeding predefined categories...");
            
            // Create CategoryDAO to verify seeding worked
            CategoryDAO categoryDAO = new CategoryDAO();
            
            // Get all categories from database
            List<Category> allCategories = categoryDAO.getAllCategories();
            System.out.println("Total categories found: " + allCategories.size());
            
            // Filter predefined categories
            List<Category> predefinedCategories = allCategories.stream()
                .filter(Category::isPredefined)
                .toList();
            
            System.out.println("Predefined categories found: " + predefinedCategories.size());
            System.out.println("\nPredefined categories details:");
            System.out.println("=" + "=".repeat(60));
            
            predefinedCategories.forEach(category -> {
                System.out.printf("| %-12s | %-15s | %-5s | %s%n", 
                    category.getName(),
                    category.getDisplayName(),
                    category.getIcon(),
                    category.getHint());
            });
            System.out.println("=" + "=".repeat(60));
            
            // Expected predefined categories
            String[] expectedCategories = {
                "PRENOM", "VILLE", "PAYS", "ANIMAL", "FRUIT",
                "LEGUME", "METIER", "COULEUR", "OBJET", "MARQUE"
            };
            
            System.out.println("\nVerification:");
            boolean allPresent = true;
            for (String expectedCategory : expectedCategories) {
                boolean found = predefinedCategories.stream()
                    .anyMatch(cat -> cat.getName().equals(expectedCategory));
                System.out.println("✓ " + expectedCategory + ": " + (found ? "PRESENT" : "MISSING"));
                if (!found) allPresent = false;
            }
            
            System.out.println("\n" + (allPresent ? "✅ SUCCESS: All predefined categories are present!" : "❌ FAILURE: Some predefined categories are missing!"));
            
            // Test idempotent seeding by running initialization again
            System.out.println("\nTesting idempotent seeding...");
            com.baccalaureat.service.DatabaseInitializer initializer = new com.baccalaureat.service.DatabaseInitializer();
            initializer.initializeDatabase();
            
            List<Category> categoriesAfterSecondInit = categoryDAO.getAllCategories();
            List<Category> predefinedAfterSecondInit = categoriesAfterSecondInit.stream()
                .filter(Category::isPredefined)
                .toList();
            
            System.out.println("Categories after second initialization: " + predefinedAfterSecondInit.size());
            System.out.println(predefinedAfterSecondInit.size() == predefinedCategories.size() ? 
                "✅ SUCCESS: Idempotent seeding working correctly!" : 
                "❌ FAILURE: Duplicate categories created!");
            
        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== Demo Complete ===");
    }
}