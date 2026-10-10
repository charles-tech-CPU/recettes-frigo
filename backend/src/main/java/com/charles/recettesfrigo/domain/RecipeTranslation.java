package com.charles.recettesfrigo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Traduction francaise d'une recette TheMealDB, faite une fois par DeepL puis relue en base. */
@Entity
@Table(name = "recipe_translation")
@Getter
@Setter
@NoArgsConstructor
public class RecipeTranslation {

    @Id
    @Column(name = "meal_id", length = 20)
    private String mealId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String area;

    @Column(columnDefinition = "text")
    private String instructions;

    /** JSON [{"ingredient": "...", "measure": "..."}], dans l'ordre de TheMealDB. */
    @Column(name = "ingredients", nullable = false, columnDefinition = "text")
    private String ingredientsJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
