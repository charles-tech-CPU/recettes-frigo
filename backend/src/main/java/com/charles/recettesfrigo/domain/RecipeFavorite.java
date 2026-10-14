package com.charles.recettesfrigo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Recette mise en favori. recipeKey designe la recette : "mealdb:<idMeal>" pour TheMealDB,
 * "perso:<id>" pour une recette perso ; titre et photo sont gardes pour afficher la liste.
 */
@Entity
@Table(name = "recipe_favorite")
@Getter
@Setter
@NoArgsConstructor
public class RecipeFavorite {

    @Id
    @Column(name = "recipe_key", length = 40)
    private String recipeKey;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
