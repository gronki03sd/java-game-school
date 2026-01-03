package com.baccalaureat.ai;

import com.baccalaureat.model.Category;
import com.baccalaureat.model.ValidationResult;
import com.baccalaureat.model.ValidationStatus;
import com.baccalaureat.service.CategoryService;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Fixed list validator for deterministic word validation.
 * Contains predefined word lists for each category.
 * This validator provides high confidence for known valid words.
 */
public class FixedListValidator implements CategoryValidator {
    
    private static final Map<String, Set<String>> VALID_WORDS = new HashMap<>();
    private CategoryService categoryService;
    
    static {
        // Initialize with some known valid words for each category
        // These are deterministic lists that can be expanded
        VALID_WORDS.put("PAYS", Set.of(
            "france", "allemagne", "italie", "espagne", "angleterre", "portugal", 
            "suisse", "belgique", "canada", "bresil", "argentine", "japon", "chine", "russie"
        ));
        
        VALID_WORDS.put("VILLE", Set.of(
            "paris", "lyon", "marseille", "toulouse", "nice", "strasbourg", "bordeaux", 
            "lille", "rennes", "reims", "montpellier", "dijon", "angers", "nimes"
        ));
        
        VALID_WORDS.put("ANIMAL", Set.of(
            "chien", "chat", "cheval", "vache", "porc", "mouton", "lapin", "souris", 
            "lion", "tigre", "elephant", "girafe", "zebre", "renard", "loup", "ours"
        ));
        
        VALID_WORDS.put("METIER", Set.of(
            "medecin", "infirmier", "professeur", "ingenieur", "avocat", "comptable", 
            "architecte", "plombier", "electricien", "boulanger", "coiffeur", "dentiste"
        ));
        
        VALID_WORDS.put("PRENOM", Set.of(
            "pierre", "paul", "jean", "marie", "anne", "sophie", "claire", "julien", 
            "nicolas", "thomas", "antoine", "matthieu", "alexandre", "francois"
        ));
        
        VALID_WORDS.put("FRUIT", Set.of(
            "pomme", "poire", "banane", "orange", "citron", "fraise", "cerise", "peche", 
            "abricot", "prune", "raisin", "melon", "pasteque", "ananas", "kiwi", "mangue",
            "tomate", "carotte", "pomme de terre", "salade", "radis", "navet"
        ));
        
        VALID_WORDS.put("OBJET", Set.of(
            "table", "chaise", "lit", "armoire", "television", "telephone", "ordinateur", 
            "livre", "stylo", "crayon", "voiture", "velo", "montre", "lunettes"
        ));
        
        VALID_WORDS.put("CELEBRITE", Set.of(
            "napoleon", "de gaulle", "moliere", "voltaire", "hugo", "balzac", "zola", 
            "monet", "renoir", "picasso", "dali", "rodin", "pasteur", "curie"
        ));
    }
    
    @Override
    public ValidationResult validate(String word, Category category) {
        if (word == null || word.trim().isEmpty()) {
            return new ValidationResult(ValidationStatus.INVALID, 0.0, getSourceName(), "Empty word");
        }
        
        String normalizedWord = normalizeWord(word);
        Set<String> categoryWords = VALID_WORDS.get(category);
        
        if (categoryWords == null) {
            return new ValidationResult(ValidationStatus.UNCERTAIN, 0.0, getSourceName(), "Category not supported");
        }
        
        // Fixed lists are deterministic sources, therefore confidence is always 1.0 when matched.
        if (categoryWords.contains(normalizedWord)) {
            return new ValidationResult(ValidationStatus.VALID, 1.0, getSourceName(), "Found in fixed word list");
        }
        
        return new ValidationResult(ValidationStatus.UNCERTAIN, 0.0, getSourceName(), "Not found in fixed word list");
    }
    
    @Override
    public String getSourceName() {
        return "FIXED_LIST";
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Always available
    }
    
    /**
     * Normalizes a word for comparison (lowercase, trim, remove accents if needed).
     */
    private String normalizeWord(String word) {
        if (word == null) return "";
        
        return word.trim()
                  .toLowerCase()
                  .replace("à", "a")
                  .replace("é", "e")
                  .replace("è", "e")
                  .replace("ê", "e")
                  .replace("ë", "e")
                  .replace("î", "i")
                  .replace("ï", "i")
                  .replace("ô", "o")
                  .replace("ö", "o")
                  .replace("ù", "u")
                  .replace("û", "u")
                  .replace("ü", "u")
                  .replace("ÿ", "y")
                  .replace("ç", "c");
    }
    
    /**
     * Adds a word to the fixed list for a category (for dynamic expansion).
     */
    public static void addValidWord(Category category, String word) {
        Set<String> existingWords = VALID_WORDS.get(category.getName());
        if (existingWords != null) {
            // Create a new mutable set since Set.of() creates immutable sets
            Set<String> newWords = new HashSet<>(existingWords);
            newWords.add(word.toLowerCase().trim());
            VALID_WORDS.put(category.getName(), newWords);
        }
    }
}