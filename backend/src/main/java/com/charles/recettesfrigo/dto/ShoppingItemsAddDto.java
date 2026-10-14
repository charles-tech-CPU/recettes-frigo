package com.charles.recettesfrigo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Articles a ajouter d'un coup a la liste de courses (les manquants d'une recette, ou un
 * seul saisi a la main). Tailles alignees sur les colonnes de shopping_item.
 */
public record ShoppingItemsAddDto(@NotEmpty @Valid List<Item> items) {

    public record Item(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 80) String quantity,
            @Size(max = 300) String recipeTitle) {}
}
