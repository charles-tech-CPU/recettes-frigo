package com.charles.recettesfrigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecipeNoteCreateDto(
        @NotBlank String recipeKey,
        @NotBlank @Size(max = 2000) String content) {}
