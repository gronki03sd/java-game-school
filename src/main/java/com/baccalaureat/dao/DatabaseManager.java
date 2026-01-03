package com.baccalaureat.dao;

import com.baccalaureat.service.DatabaseInitializer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:baccalaureat.db";

    static {
        initializeDatabase();
    }

    public static Connection getConnection() throws SQLException {
        String dbUrl = System.getProperty("db.url", DEFAULT_DB_URL);
        return DriverManager.getConnection(dbUrl);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create validated_words table per specification
            String ddl = "CREATE TABLE IF NOT EXISTS validated_words (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "word TEXT NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "validated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(word, category)" +
                    ")";
            stmt.execute(ddl);
            
            // Create categories table for dynamic categories
            String categoriesDdl = "CREATE TABLE IF NOT EXISTS categories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL UNIQUE, " +
                    "display_name TEXT NOT NULL, " +
                    "icon TEXT, " +
                    "hint TEXT, " +
                    "enabled BOOLEAN DEFAULT true, " +
                    "predefined BOOLEAN DEFAULT false, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(categoriesDdl);
            
            // Create indexes
            String indexDdl = "CREATE INDEX IF NOT EXISTS idx_word_category " +
                    "ON validated_words(word, category)";
            stmt.execute(indexDdl);
            
            String categoriesIndexDdl = "CREATE INDEX IF NOT EXISTS idx_categories_enabled " +
                    "ON categories(enabled, name)";
            stmt.execute(categoriesIndexDdl);
            
        } catch (SQLException e) {
            System.err.println("Failed to initialize database schema: " + e.getMessage());
        }
        
        // Initialize predefined categories using DatabaseInitializer
        try {
            DatabaseInitializer initializer = new DatabaseInitializer();
            initializer.initializeDatabase();
        } catch (Exception e) {
            System.err.println("Failed to initialize predefined categories: " + e.getMessage());
        }
    }
    
}
