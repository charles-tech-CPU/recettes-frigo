package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.dto.RecipeDetailDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TheMealDB renvoie les ingredients d'une recette "a plat" : jusqu'a 20
 * couples de champs strIngredient1/strMeasure1 .. strIngredient20/strMeasure20
 * sur l'objet recette lui-meme, la plupart vides pour une recette donnee.
 * Cette classe les regroupe en une liste propre.
 */
final class MealIngredientParser {

    private MealIngredientParser() {}

    static List<RecipeDetailDto.IngredientLine> extract(Map<String, Object> meal) {
        List<RecipeDetailDto.IngredientLine> lines = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String ingredient = clean(meal.get("strIngredient" + i));
            String measure = clean(meal.get("strMeasure" + i));
            if (ingredient != null && !ingredient.isBlank()) {
                lines.add(new RecipeDetailDto.IngredientLine(ingredient, measure == null ? "" : measure));
            }
        }
        return lines;
    }

    private static String clean(Object raw) {
        if (raw == null) return null;
        String s = String.valueOf(raw).trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return null;
        return s;
    }
}
