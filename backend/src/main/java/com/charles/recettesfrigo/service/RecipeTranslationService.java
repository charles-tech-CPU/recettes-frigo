package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.domain.RecipeTranslation;
import com.charles.recettesfrigo.dto.RecipeDetailDto.IngredientLine;
import com.charles.recettesfrigo.repository.RecipeTranslationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Version francaise des recettes TheMealDB, traduites par MyMemory (gratuit, quota
 * quotidien limite) puis gardees dans recipe_translation : une recette n'est jamais
 * traduite deux fois. Pour menager le quota, la traduction se fait en deux temps :
 * <ul>
 *   <li>liste des suggestions : titre et noms d'ingredients seulement, toutes les
 *       recettes regroupees dans quelques requetes (translateSummaries) ;</li>
 *   <li>detail d'une recette : categorie, origine, instructions et quantites en plus
 *       (translateFull), seulement pour les recettes ouvertes.</li>
 * </ul>
 * Traduction impossible (quota du jour atteint, service en panne) : la recette reste
 * en anglais (translated = false) et sera retentee la fois suivante.
 */
@Service
public class RecipeTranslationService {

    private static final Logger log = LoggerFactory.getLogger(RecipeTranslationService.class);
    private static final TypeReference<List<IngredientLine>> INGREDIENTS = new TypeReference<>() {};

    private final RecipeTranslationRepository repository;
    private final MyMemoryClient translator;
    private final ObjectMapper objectMapper;

    public RecipeTranslationService(
            RecipeTranslationRepository repository, MyMemoryClient translator, ObjectMapper objectMapper) {
        this.repository = repository;
        this.translator = translator;
        this.objectMapper = objectMapper;
    }

    /** Ingredients dans l'ordre de TheMealDB ; translated = false si la recette est restee en anglais. */
    public record TranslatedMeal(
            String title,
            String category,
            String area,
            String instructions,
            List<IngredientLine> ingredients,
            boolean translated) {}

    /** Titres et noms d'ingredients en francais, pour la liste des suggestions (cle = idMeal). */
    public Map<String, TranslatedMeal> translateSummaries(Map<String, Map<String, Object>> mealsById) {
        Map<String, TranslatedMeal> result = new LinkedHashMap<>();
        Map<String, TranslatedMeal> toTranslate = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : mealsById.entrySet()) {
            Optional<RecipeTranslation> cached = repository.findById(entry.getKey());
            TranslatedMeal english = english(entry.getValue());
            result.put(entry.getKey(), cached.map(this::fromCache).orElse(english));
            if (cached.isEmpty()) {
                toTranslate.put(entry.getKey(), english);
            }
        }
        if (toTranslate.isEmpty()) {
            return result;
        }

        // Toutes les recettes a plat dans une seule liste : titre puis noms d'ingredients
        List<String> texts = new ArrayList<>();
        for (TranslatedMeal meal : toTranslate.values()) {
            texts.add(meal.title());
            meal.ingredients().forEach(line -> texts.add(line.ingredient()));
        }
        List<String> french;
        try {
            french = translator.translateToFrench(texts);
        } catch (RestClientException | IllegalStateException ex) {
            log.warn("Traduction des suggestions impossible : {}", ex.getMessage());
            return result;
        }

        int position = 0;
        for (Map.Entry<String, TranslatedMeal> entry : toTranslate.entrySet()) {
            TranslatedMeal english = entry.getValue();
            String title = capitalize(french.get(position++));
            List<IngredientLine> ingredients = new ArrayList<>();
            for (IngredientLine line : english.ingredients()) {
                // quantites gardees en anglais : traduites avec le detail
                ingredients.add(new IngredientLine(capitalize(french.get(position++)), line.measure()));
            }
            TranslatedMeal summary = new TranslatedMeal(title, null, null, null, ingredients, true);
            save(entry.getKey(), summary, false);
            result.put(entry.getKey(), summary);
        }
        return result;
    }

    /** Recette complete en francais, pour l'ecran de detail. */
    public TranslatedMeal translateFull(String mealId, Map<String, Object> meal) {
        Optional<RecipeTranslation> cached = repository.findById(mealId);
        if (cached.isPresent() && cached.get().isFullTranslation()) {
            return fromCache(cached.get());
        }
        TranslatedMeal english = english(meal);
        List<String> texts = new ArrayList<>(List.of(
                nullToEmpty(english.title()),
                nullToEmpty(english.category()),
                nullToEmpty(english.area()),
                nullToEmpty(english.instructions())));
        for (IngredientLine line : english.ingredients()) {
            texts.add(line.ingredient());
            texts.add(line.measure());
        }
        try {
            List<String> french = translator.translateToFrench(texts);
            List<IngredientLine> ingredients = new ArrayList<>();
            for (int i = 0; i < english.ingredients().size(); i++) {
                ingredients.add(
                        new IngredientLine(capitalize(french.get(4 + 2 * i)), nullToEmpty(french.get(5 + 2 * i))));
            }
            TranslatedMeal full = new TranslatedMeal(
                    capitalize(french.get(0)),
                    emptyToNull(french.get(1)),
                    emptyToNull(french.get(2)),
                    emptyToNull(french.get(3)),
                    ingredients,
                    true);
            save(mealId, full, true);
            return full;
        } catch (RestClientException | IllegalStateException ex) {
            log.warn("Traduction de la recette {} impossible : {}", mealId, ex.getMessage());
            // Titre et ingredients deja traduits pour la liste : mieux que tout en anglais
            return cached.map(this::fromCache)
                    .map(partial -> new TranslatedMeal(
                            partial.title(),
                            english.category(),
                            english.area(),
                            english.instructions(),
                            partial.ingredients(),
                            false))
                    .orElse(english);
        }
    }

    private static TranslatedMeal english(Map<String, Object> meal) {
        return new TranslatedMeal(
                str(meal.get("strMeal")),
                str(meal.get("strCategory")),
                str(meal.get("strArea")),
                str(meal.get("strInstructions")),
                MealIngredientParser.extract(meal),
                false);
    }

    private TranslatedMeal fromCache(RecipeTranslation entity) {
        List<IngredientLine> ingredients;
        try {
            ingredients = objectMapper.readValue(entity.getIngredientsJson(), INGREDIENTS);
        } catch (JsonProcessingException ex) {
            ingredients = List.of();
        }
        return new TranslatedMeal(
                entity.getTitle(), entity.getCategory(), entity.getArea(), entity.getInstructions(), ingredients, true);
    }

    private void save(String mealId, TranslatedMeal french, boolean full) {
        RecipeTranslation entity = repository.findById(mealId).orElseGet(RecipeTranslation::new);
        entity.setMealId(mealId);
        entity.setTitle(truncate(nullToEmpty(french.title()), 300));
        entity.setCategory(truncate(french.category(), 100));
        entity.setArea(truncate(french.area(), 100));
        entity.setInstructions(french.instructions());
        try {
            entity.setIngredientsJson(objectMapper.writeValueAsString(french.ingredients()));
        } catch (JsonProcessingException ex) {
            return; // pas de cache plutot qu'un cache incomplet
        }
        entity.setFullTranslation(full);
        entity.setCreatedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    /** MyMemory renvoie parfois le titre ou l'ingredient sans majuscule ("riz au poulet"). */
    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
