package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.charles.recettesfrigo.dto.RecipeDetailDto.IngredientLine;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MealIngredientParserTest {

    @Test
    void seulsLesIngredientsRenseignesSontGardes() {
        Map<String, Object> meal = new HashMap<>();
        meal.put("strIngredient1", "Chicken");
        meal.put("strMeasure1", " 2 breasts ");
        meal.put("strIngredient2", "Salt");
        meal.put("strMeasure2", null);
        meal.put("strIngredient3", "  ");
        meal.put("strMeasure3", "1 tsp");
        meal.put("strIngredient4", "null");
        meal.put("strIngredient5", "Rice");
        meal.put("strMeasure5", "null");

        assertThat(MealIngredientParser.extract(meal))
                .containsExactly(
                        new IngredientLine("Chicken", "2 breasts"),
                        new IngredientLine("Salt", ""),
                        new IngredientLine("Rice", ""));
    }
}
