package com.charles.recettesfrigo.dto;

import jakarta.validation.constraints.NotBlank;

public record FridgeItemCreateDto(@NotBlank String name, String quantity, String category) {}
