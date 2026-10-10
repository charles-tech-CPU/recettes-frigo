package com.charles.recettesfrigo.service;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Correspondance entre un ingredient du frigo et un ingredient d'une recette perso,
 * tous deux en francais : sans accents, majuscules ni pluriel, et par mots entiers
 * ("Carottes" correspond a "carotte rapee", mais "ail" ne correspond pas a "taille").
 */
final class FrenchIngredientMatcher {

    private FrenchIngredientMatcher() {}

    static boolean matches(String recipeIngredient, String fridgeName) {
        String recipe = " " + simplify(recipeIngredient) + " ";
        String fridge = " " + simplify(fridgeName) + " ";
        if (recipe.isBlank() || fridge.isBlank()) {
            return false;
        }
        return recipe.contains(fridge) || fridge.contains(recipe);
    }

    /** "Huile d'Olive" -> "huile d olive", "Carottes" -> "carotte". */
    static String simplify(String name) {
        String normalized = IngredientTranslations.normalize(name).replaceAll("[^a-z0-9]+", " ");
        return Arrays.stream(normalized.trim().split(" "))
                .filter(word -> !word.isEmpty())
                .map(word -> word.length() > 3 ? word.replaceAll("[sx]$", "") : word)
                .collect(Collectors.joining(" "));
    }
}
