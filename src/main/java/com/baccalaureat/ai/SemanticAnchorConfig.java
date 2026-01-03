package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.service.CategoryService;

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
    
    private static final Map<String, Set<String>> CATEGORY_ANCHORS = new HashMap<>();
    
    static {
        // TODO: AI/API implementation in later phase
        // These anchor words would be used to create semantic embeddings
        // for each category, enabling AI-based similarity matching
        
        CATEGORY_ANCHORS.put("PAYS", Set.of(
            "nation", "territoire", "etat", "republique", "royaume", "continent", "frontiere", "capitale"
        ));
        
        CATEGORY_ANCHORS.put("VILLE", Set.of(
            "metropole", "commune", "agglomeration", "banlieue", "centre-ville", "quartier", "avenue", "place"
        ));
        
        CATEGORY_ANCHORS.put("ANIMAL", Set.of(
            "mammifere", "oiseau", "reptile", "poisson", "insecte", "domestique", "sauvage", "predateur", "herbivore"
        ));
        
        CATEGORY_ANCHORS.put("METIER", Set.of(
            "profession", "travail", "emploi", "carriere", "competence", "salaire", "formation", "expertise"
        ));
        
        CATEGORY_ANCHORS.put("PRENOM", Set.of(
            "nom", "identite", "bapteme", "naissance", "masculin", "feminin", "traditionnel", "moderne"
        ));
        
        CATEGORY_ANCHORS.put("FRUIT", Set.of(
            "nutrition", "vitamine", "sucre", "jus", "verger", "recolte", "saveur", "legume", "potager"
        ));
        
        CATEGORY_ANCHORS.put("OBJET", Set.of(
            "materiel", "outil", "utile", "fabrique", "plastique", "metal", "bois", "quotidien", "maison"
        ));
        
        CATEGORY_ANCHORS.put("CELEBRITE", Set.of(
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
        if (category == null) {
            return new HashSet<>();
        }
        return new HashSet<>(CATEGORY_ANCHORS.getOrDefault(category.name(), Set.of()));
    }
    
    /**
     * Adds an anchor word to a category.
     * 
     * @param category the target category
     * @param anchor the anchor word to add
     */
    public static void addAnchor(Category category, String anchor) {
        if (category != null && anchor != null) {
            CATEGORY_ANCHORS.computeIfAbsent(category.name(), k -> new HashSet<>()).add(anchor.toLowerCase().trim());
        }
    }
    
    /**
     * Removes an anchor word from a category.
     * 
     * @param category the target category
     * @param anchor the anchor word to remove
     */
    public static void removeAnchor(Category category, String anchor) {
        if (category != null && anchor != null) {
            Set<String> anchors = CATEGORY_ANCHORS.get(category.name());
            if (anchors != null) {
                anchors.remove(anchor.toLowerCase().trim());
            }
        }
    }
    
    /**
     * Gets all configured category names.
     * 
     * @return set of configured category names
     */
    public static Set<String> getConfiguredCategoryNames() {
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