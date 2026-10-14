package com.charles.recettesfrigo.dto;

import java.time.LocalDate;

public record FridgeItemDto(Long id, String name, String quantity, String category, LocalDate expiresOn) {}
