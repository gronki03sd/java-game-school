package com.baccalaureat.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Dynamic Category model representing game categories stored in database.
 * Replaces the previous enum-based approach with flexible, user-customizable categories.
 */
public class Category {
    private final int id;
    private final String name;
    private final String displayName;
    private final String icon;
    private final String hint;
    private final boolean enabled;
    private final boolean predefined;
    private final LocalDateTime createdAt;

    public Category(int id, String name, String displayName, String icon, String hint, boolean enabled, boolean predefined, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.icon = icon;
        this.hint = hint;
        this.enabled = enabled;
        this.predefined = predefined;
        this.createdAt = createdAt;
    }
    
    // Constructor for DAO mapping (without timestamp)
    public Category(int id, String name, String displayName, String icon, String hint, boolean enabled, boolean predefined) {
        this(id, name, displayName, icon, hint, enabled, predefined, LocalDateTime.now());
    }
    
    // Constructor for creating new categories (without id and timestamp)
    public Category(String name, String displayName, String icon, String hint) {
        this(0, name, displayName, icon, hint, true, false, LocalDateTime.now());
    }
    
    // Constructor for creating predefined categories
    public Category(String name, String displayName, String icon, String hint, boolean predefined) {
        this(0, name, displayName, icon, hint, true, predefined, LocalDateTime.now());
    }
    
    // Constructor for creating categories with enabled/predefined flags
    public Category(String name, String displayName, String icon, String hint, boolean enabled, boolean predefined) {
        this(0, name, displayName, icon, hint, enabled, predefined, LocalDateTime.now());
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    // Legacy compatibility method
    public String displayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getHint() {
        return hint;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPredefined() {
        return predefined;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id || Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return displayName;
    }
    
    /**
     * Legacy compatibility method for enum-style name() calls.
     * Returns the internal name identifier.
     */
    public String name() {
        return name;
    }
}
