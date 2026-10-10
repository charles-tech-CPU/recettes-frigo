package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IngredientTranslationsTest {

    @Test
    void unIngredientConnuEstTraduitSansTenirCompteDeLaCasseNiDesEspaces() {
        assertThat(IngredientTranslations.toEnglishQueryTerm("  Poulet ")).isEqualTo("chicken");
        assertThat(IngredientTranslations.toEnglishQueryTerm("Huile d'olive")).isEqualTo("olive_oil");
        assertThat(IngredientTranslations.isKnown("TOMATE")).isTrue();
    }

    @Test
    void lesAccentsEtLigaturesSontIgnores() {
        assertThat(IngredientTranslations.toEnglishQueryTerm("Bœuf haché")).isEqualTo("ground_beef");
        assertThat(IngredientTranslations.toEnglishQueryTerm("Épinards")).isEqualTo("spinach");
        assertThat(IngredientTranslations.toEnglishQueryTerm("Pâtes")).isEqualTo("pasta");
        assertThat(IngredientTranslations.toEnglishQueryTerm("Huile d’olive")).isEqualTo("olive_oil");
        assertThat(IngredientTranslations.toEnglishQueryTerm("Œufs")).isEqualTo("eggs");
    }

    @Test
    void unIngredientInconnuEstRenvoyeNormaliseAvecDesUnderscores() {
        assertThat(IngredientTranslations.toEnglishQueryTerm("Fruit du Dragon")).isEqualTo("fruit_du_dragon");
        assertThat(IngredientTranslations.isKnown("fruit du dragon")).isFalse();
    }
}
