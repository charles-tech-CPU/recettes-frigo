package com.charles.recettesfrigo.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Recette personnelle, saisie a la main ou importee depuis un lien, eventuellement Thermomix. */
@Entity
@Table(name = "custom_recipe")
@Getter
@Setter
@NoArgsConstructor
public class CustomRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 80)
    private String category;

    private Integer servings;

    @Column(name = "total_time_minutes")
    private Integer totalTimeMinutes;

    @Column(nullable = false)
    private boolean thermomix;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<CustomRecipeIngredient> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<CustomRecipeStep> steps = new ArrayList<>();
}
