package com.charles.recettesfrigo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Recette perso, en lecture comme en ecriture (id ignore a la creation). Les champs
 * tm* d'une etape portent son reglage Thermomix, vides pour une etape classique.
 */
public record CustomRecipeDto(
        Long id,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 80) String category,
        @Positive Integer servings,
        @Positive Integer totalTimeMinutes,
        boolean thermomix,
        @Size(max = 1000) String imageUrl,
        @Size(max = 1000) String sourceUrl,
        @Valid List<Ingredient> ingredients,
        @Valid List<Step> steps) {

    public record Ingredient(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 80) String quantity) {}

    public record Step(
            @NotBlank String instruction,
            @Size(max = 20) String tmTime,
            @Size(max = 20) String tmTemperature,
            @Size(max = 20) String tmSpeed,
            boolean tmReverse) {}
}
