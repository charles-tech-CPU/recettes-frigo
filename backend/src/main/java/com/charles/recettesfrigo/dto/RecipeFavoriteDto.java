package com.charles.recettesfrigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Recette favorite, en lecture comme en ecriture : recipeKey = "mealdb:<idMeal>" ou "perso:<id>". */
public record RecipeFavoriteDto(
        @NotBlank String recipeKey,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 1000) String imageUrl) {}
