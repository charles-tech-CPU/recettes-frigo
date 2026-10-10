package com.charles.recettesfrigo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "custom_recipe_ingredient")
@Getter
@Setter
@NoArgsConstructor
public class CustomRecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private CustomRecipe recipe;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 200)
    private String name;

    /** Quantite libre (ex: "200 g", "2 pièces"). */
    @Column(length = 80)
    private String quantity;
}
