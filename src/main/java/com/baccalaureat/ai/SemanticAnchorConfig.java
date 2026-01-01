package com.baccalaureat.ai;

import com.baccalaureat.model.Category;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Configuration for semantic anchor words used by AI validation.
 * Contains representative words for each category that define semantic spaces.
 * 
 * PLACEHOLDER IMPLEMENTATION - FOR FUTURE AI EXPANSION
 */
public class SemanticAnchorConfig {
    
    private static final Map<Category, Set<String>> CATEGORY_ANCHORS = new HashMap<>();
    
    static {
        // TODO: AI/API implementation in later phase
        // These anchor words would be used to create semantic embeddings
        // for each category, enabling AI-based similarity matching
        
        CATEGORY_ANCHORS.put(Category.PAYS, Set.of(
            "nation", "territoire", "etat", "republique", "royaume", "continent", "frontiere", "capitale"
        ));
        
        CATEGORY_ANCHORS.put(Category.VILLE, Set.of(
            "metropole", "commune", "agglomeration", "banlieue", "centre-ville", "quartier", "avenue", "place"
        ));
        
        CATEGORY_ANCHORS.put(Category.ANIMAL, Set.of(
            "mammifere", "oiseau", "reptile", "poisson", "insecte", "domestique", "sauvage", "predateur", "herbivore"
        ));
        
        CATEGORY_ANCHORS.put(Category.METIER, Set.of(
            "profession", "travail", "emploi", "carriere", "competence", "salaire", "formation", "expertise"
        ));
        
        CATEGORY_ANCHORS.put(Category.PRENOM, Set.of(
            "nom", "identite", "bapteme", "naissance", "masculin", "feminin", "traditionnel", "moderne"
        ));
        
        CATEGORY_ANCHORS.put(Category.FRUIT, Set.of(
            "nutrition", "vitamine", "sucre", "jus", "verger", "recolte", "saveur", "legume", "potager"
        ));
        
        CATEGORY_ANCHORS.put(Category.OBJET, Set.of(
            "materiel", "outil", "utile", "fabrique", "plastique", "metal", "bois", "quotidien", "maison"
        ));
        
        CATEGORY_ANCHORS.put(Category.CELEBRITE, Set.of(
            "celebre", "connu", "personnalite", "star", "artiste", "histoire", "media", "renommee"
        ));
    }
    
    /**
     * Gets semantic anchor words for a category.
     * 
     * @param category the category
     * @return set of anchor words for semantic similarity
     */
    public static Set<String> getAnchors(Category category) {
        return new HashSet<>(CATEGORY_ANCHORS.getOrDefault(category, Set.of()));
    }
    
    /**
     * Adds an anchor word to a category.
     * 
     * @param category the target category
     * @param anchor the anchor word to add
     */
    public static void addAnchor(Category category, String anchor) {
        CATEGORY_ANCHORS.computeIfAbsent(category, k -> new HashSet<>()).add(anchor.toLowerCase().trim());
    }
    
    /**
     * Removes an anchor word from a category.
     * 
     * @param category the target category
     * @param anchor the anchor word to remove
     */
    public static void removeAnchor(Category category, String anchor) {
        Set<String> anchors = CATEGORY_ANCHORS.get(category);
        if (anchors != null) {
            anchors.remove(anchor.toLowerCase().trim());
        }
    }
    
    /**
     * Gets all categories that have anchor configurations.
     * 
     * @return set of configured categories
     */
    public static Set<Category> getConfiguredCategories() {
        return new HashSet<>(CATEGORY_ANCHORS.keySet());
    }
    
    // TODO: AI/API implementation in later phase
    // Future methods for AI integration:
    // - generateEmbeddings(Set<String> anchors)
    // - calculateCategoryVector(Category category)
    // - updateAnchorsFromTrainingData(Category category, List<String> examples)
    // - saveAnchorConfig(String filePath)
    // - loadAnchorConfig(String filePath)
}