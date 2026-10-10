package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FrenchIngredientMatcherTest {

    @Test
    void correspondanceSansAccentsMajusculesNiPluriel() {
        assertThat(FrenchIngredientMatcher.matches("Carotte râpée", "Carottes")).isTrue();
        assertThat(FrenchIngredientMatcher.matches("oeufs", "Œuf")).isTrue();
        assertThat(FrenchIngredientMatcher.matches("Huile d'olive", "huile")).isTrue();
        assertThat(FrenchIngredientMatcher.matches("Poulet", "Blanc de poulet")).isTrue();
    }

    @Test
    void correspondanceParMotsEntiers() {
        assertThat(FrenchIngredientMatcher.matches("Échalote de taille moyenne", "Ail"))
                .isFalse();
        assertThat(FrenchIngredientMatcher.matches("Pommes de terre", "Riz")).isFalse();
        assertThat(FrenchIngredientMatcher.matches("Crème", "  ")).isFalse();
    }
}
