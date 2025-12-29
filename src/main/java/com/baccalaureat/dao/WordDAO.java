package com.baccalaureat.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WordDAO {
    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public boolean isWordInLocalDb(String category, String word) {
        if (category == null || word == null)
            return false;
        String sql = "SELECT COUNT(1) FROM validated_words WHERE lower(category)=lower(?) AND lower(word)=lower(?)";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.trim());
            ps.setString(2, word.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("DB check failed: " + e.getMessage());
            return false;
        }
    }

    public void saveWord(String category, String word) {
        if (category == null || word == null)
            return;
        String sql = "INSERT INTO validated_words(category, word, timestamp) VALUES(?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.trim());
            ps.setString(2, word.trim());
            ps.setString(3, LocalDateTime.now().format(TS));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB insert failed: " + e.getMessage());
        }
    }
}
