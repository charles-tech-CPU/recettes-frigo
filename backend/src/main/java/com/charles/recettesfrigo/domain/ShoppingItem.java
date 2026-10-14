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
 * Un article de la liste de courses : ingredient manquant d'une recette (recipeTitle)
 * ou ajoute a la main, coche au magasin puis range dans le frigo.
 */
@Entity
@Table(name = "shopping_item")
@Getter
@Setter
@NoArgsConstructor
public class ShoppingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    /** Quantite libre (ex: "200 g", "2 boîtes"), purement informative. */
    @Column(length = 80)
    private String quantity;

    /** Recette pour laquelle l'article a ete ajoute, null si ajoute a la main. */
    @Column(name = "recipe_title", length = 300)
    private String recipeTitle;

    @Column(nullable = false)
    private boolean checked;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
