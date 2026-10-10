package com.charles.recettesfrigo.dto;

import java.util.List;

/**
 * Une recette suggeree a partir de l'inventaire du frigo, classee par nombre
 * d'ingredients du frigo qu'elle utilise (matchedIngredientCount). Le titre
 * et les ingredients manquants restent en anglais (langue de TheMealDB) -
 * voir README.
 */
public record RecipeSuggestionDto(
        String mealId,
        String title,
        String thumbnailUrl,
        int matchedIngredientCount,
        int totalIngredientCount,
        List<String> missingIngredients) {}
