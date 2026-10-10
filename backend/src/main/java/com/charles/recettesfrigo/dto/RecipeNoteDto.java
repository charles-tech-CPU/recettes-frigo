package com.charles.recettesfrigo.dto;

import java.time.OffsetDateTime;

public record RecipeNoteDto(Long id, String recipeKey, String content, OffsetDateTime createdAt) {}
