package com.charles.recettesfrigo.dto;

import java.util.List;

public record RecipeDetailDto(
        String mealId,
        String title,
        String thumbnailUrl,
        String category,
        String area,
        String instructions,
        String videoUrl,
        String sourceUrl,
        List<IngredientLine> ingredients) {
    public record IngredientLine(String ingredient, String measure) {}
}
