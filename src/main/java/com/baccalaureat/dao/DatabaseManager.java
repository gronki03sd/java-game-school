package com.baccalaureat.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:baccalaureat.db";

    static {
        initializeDatabase();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void initializeDatabase() {
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
            
            // Create index for faster lookups
            String indexDdl = "CREATE INDEX IF NOT EXISTS idx_word_category " +
                    "ON validated_words(word, category)";
            stmt.execute(indexDdl);
            
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }
}
