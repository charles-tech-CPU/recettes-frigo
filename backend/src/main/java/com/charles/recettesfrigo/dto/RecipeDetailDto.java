package com.charles.recettesfrigo.dto;

import java.util.List;

/** Detail d'une recette TheMealDB ; translated = false si elle est restee en anglais (DeepL absent ou en panne). */
public record RecipeDetailDto(
        String mealId,
        String title,
        String thumbnailUrl,
        String category,
        String area,
        String instructions,
        String videoUrl,
        String sourceUrl,
        List<IngredientLine> ingredients,
        boolean translated) {
    public record IngredientLine(String ingredient, String measure) {}
}
