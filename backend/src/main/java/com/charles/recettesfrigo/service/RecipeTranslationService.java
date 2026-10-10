package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.domain.RecipeTranslation;
import com.charles.recettesfrigo.dto.RecipeDetailDto.IngredientLine;
import com.charles.recettesfrigo.repository.RecipeTranslationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Version francaise d'une recette TheMealDB. Chaque recette n'est traduite qu'une
 * fois (une requete DeepL pour le titre, la categorie, l'origine, les instructions,
 * les ingredients et les quantites), puis relue dans recipe_translation : le quota
 * gratuit DeepL n'est consomme que pour les recettes jamais vues.
 * <p>
 * DeepL non configure ou en panne : la recette reste en anglais (translated = false)
 * plutot que de faire echouer l'affichage.
 */
@Service
public class RecipeTranslationService {

    private static final Logger log = LoggerFactory.getLogger(RecipeTranslationService.class);
    private static final TypeReference<List<IngredientLine>> INGREDIENTS = new TypeReference<>() {};

    private final RecipeTranslationRepository repository;
    private final DeepLClient deepL;
    private final ObjectMapper objectMapper;

    public RecipeTranslationService(
            RecipeTranslationRepository repository, DeepLClient deepL, ObjectMapper objectMapper) {
        this.repository = repository;
        this.deepL = deepL;
        this.objectMapper = objectMapper;
    }

    public record TranslatedMeal(
            String title,
            String category,
            String area,
            String instructions,
            List<IngredientLine> ingredients,
            boolean translated) {}

    public TranslatedMeal translate(String mealId, Map<String, Object> meal) {
        Optional<RecipeTranslation> cached = repository.findById(mealId);
        if (cached.isPresent()) {
            return fromCache(cached.get());
        }
        TranslatedMeal english = new TranslatedMeal(
                str(meal.get("strMeal")),
                str(meal.get("strCategory")),
                str(meal.get("strArea")),
                str(meal.get("strInstructions")),
                MealIngredientParser.extract(meal),
                false);
        if (!deepL.isEnabled()) {
            return english;
        }
        try {
            TranslatedMeal french = translateWithDeepL(english);
            repository.save(toEntity(mealId, french));
            return french;
        } catch (RestClientException | IllegalStateException | JsonProcessingException ex) {
            log.warn("Traduction DeepL impossible pour la recette {} : {}", mealId, ex.getMessage());
            return english;
        }
    }

    private TranslatedMeal translateWithDeepL(TranslatedMeal english) {
        // Textes a traduire, a plat : titre, categorie, origine, instructions, puis
        // chaque ingredient et chaque quantite. Les textes vides ne sont pas envoyes.
        List<String> texts = new ArrayList<>();
        texts.add(english.title());
        texts.add(english.category());
        texts.add(english.area());
        texts.add(english.instructions());
        for (IngredientLine line : english.ingredients()) {
            texts.add(line.ingredient());
            texts.add(line.measure());
        }
        List<Integer> toSend = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i) != null && !texts.get(i).isBlank()) {
                toSend.add(i);
            }
        }
        List<String> translated =
                deepL.translateToFrench(toSend.stream().map(texts::get).toList());
        List<String> result = new ArrayList<>(texts);
        for (int i = 0; i < toSend.size(); i++) {
            result.set(toSend.get(i), translated.get(i));
        }

        List<IngredientLine> ingredients = new ArrayList<>();
        for (int i = 0; i < english.ingredients().size(); i++) {
            String measure = result.get(5 + 2 * i);
            ingredients.add(new IngredientLine(result.get(4 + 2 * i), measure == null ? "" : measure));
        }
        return new TranslatedMeal(result.get(0), result.get(1), result.get(2), result.get(3), ingredients, true);
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

    private RecipeTranslation toEntity(String mealId, TranslatedMeal french) throws JsonProcessingException {
        RecipeTranslation entity = new RecipeTranslation();
        entity.setMealId(mealId);
        entity.setTitle(truncate(french.title() == null ? "" : french.title(), 300));
        entity.setCategory(truncate(french.category(), 100));
        entity.setArea(truncate(french.area(), 100));
        entity.setInstructions(french.instructions());
        entity.setIngredientsJson(objectMapper.writeValueAsString(french.ingredients()));
        entity.setCreatedAt(OffsetDateTime.now());
        return entity;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
