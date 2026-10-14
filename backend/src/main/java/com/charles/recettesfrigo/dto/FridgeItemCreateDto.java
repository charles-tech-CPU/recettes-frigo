package com.charles.recettesfrigo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Tailles alignees sur les colonnes de fridge_item (V1__init.sql). */
public record FridgeItemCreateDto(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 40) String quantity,
        @Size(max = 40) String category) {}
