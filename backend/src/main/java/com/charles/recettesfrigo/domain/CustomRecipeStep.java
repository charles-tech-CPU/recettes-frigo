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

/**
 * Etape d'une recette perso. Les champs tm* portent le reglage Thermomix de l'etape
 * (notation "10 min / 100°C / sens inverse / vitesse 1"), vides pour une etape classique.
 */
@Entity
@Table(name = "custom_recipe_step")
@Getter
@Setter
@NoArgsConstructor
public class CustomRecipeStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private CustomRecipe recipe;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, columnDefinition = "text")
    private String instruction;

    /** Ex: "5 min", "30 s". */
    @Column(name = "tm_time", length = 20)
    private String tmTime;

    /** Ex: "100°C", "Varoma". */
    @Column(name = "tm_temperature", length = 20)
    private String tmTemperature;

    /** Ex: "1", "2.5", "cuillère", "Turbo". */
    @Column(name = "tm_speed", length = 20)
    private String tmSpeed;

    @Column(name = "tm_reverse", nullable = false)
    private boolean tmReverse;
}
