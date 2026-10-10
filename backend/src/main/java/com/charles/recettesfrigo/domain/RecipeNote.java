package com.charles.recettesfrigo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Note libre sur une recette (ex: "cuisson 200°C, 25 min au lieu de 35"). recipeKey
 * designe la recette : "mealdb:<idMeal>" pour TheMealDB, "perso:<id>" pour une recette perso.
 */
@Entity
@Table(name = "recipe_note")
@Getter
@Setter
@NoArgsConstructor
public class RecipeNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_key", nullable = false, length = 40)
    private String recipeKey;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
