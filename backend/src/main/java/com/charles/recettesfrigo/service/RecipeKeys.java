package com.charles.recettesfrigo.service;

import java.util.regex.Pattern;

/** Cle d'une recette, partagee par les notes, les favoris... : "mealdb:<idMeal>" ou "perso:<id>". */
final class RecipeKeys {

    private static final Pattern RECIPE_KEY = Pattern.compile("(mealdb|perso):\\d{1,18}");

    private RecipeKeys() {}

    static void check(String recipeKey) {
        if (recipeKey == null || !RECIPE_KEY.matcher(recipeKey).matches()) {
            throw new IllegalArgumentException("Recette inconnue: " + recipeKey);
        }
    }

    static boolean isCustom(String recipeKey) {
        return recipeKey.startsWith("perso:");
    }

    /** "perso:12" -> "12" */
    static String id(String recipeKey) {
        return recipeKey.substring(recipeKey.indexOf(':') + 1);
    }
}
