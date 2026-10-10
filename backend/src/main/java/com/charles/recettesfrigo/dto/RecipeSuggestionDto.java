package com.charles.recettesfrigo.dto;

import java.util.List;

/**
 * Une recette suggeree a partir de l'inventaire du frigo, classee par nombre
 * d'ingredients du frigo qu'elle utilise (matchedIngredientCount).
 * source = "mealdb" (TheMealDB, traduite par MyMemory) ou "perso"
 * (recette perso) ; id = idMeal ou id de la recette perso.
 */
public record RecipeSuggestionDto(
        String id,
        String source,
        String title,
        String thumbnailUrl,
        boolean thermomix,
        int matchedIngredientCount,
        int totalIngredientCount,
        List<String> missingIngredients) {}
