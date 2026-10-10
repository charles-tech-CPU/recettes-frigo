package com.charles.recettesfrigo.dto;

import jakarta.validation.constraints.NotBlank;

public record RecipeImportRequestDto(@NotBlank String url) {}
