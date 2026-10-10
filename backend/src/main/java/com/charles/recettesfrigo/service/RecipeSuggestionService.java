package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.domain.FridgeItem;
import com.charles.recettesfrigo.dto.RecipeDetailDto;
import com.charles.recettesfrigo.dto.RecipeSuggestionDto;
import com.charles.recettesfrigo.repository.FridgeItemRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * Coeur de l'appli : propose des recettes TheMealDB a partir de l'inventaire
 * du frigo. TheMealDB ne sait filtrer que par UN SEUL ingredient a la fois
 * (filter.php?i=...), donc on interroge l'API une fois par ingredient du
 * frigo, on compte combien de fois chaque recette ressort (= combien
 * d'ingredients du frigo elle utilise), puis on trie les recettes par ce
 * score decroissant - une approximation raisonnable du "que puis-je cuisiner
 * avec le plus de choses que j'ai deja", sans jamais rien inventer : le
 * detail (ingredients manquants) n'est calcule qu'a partir de la vraie liste
 * d'ingredients de la recette, recuperee via lookup.php.
 */
@Service
public class RecipeSuggestionService {

    private final FridgeItemRepository fridgeItemRepository;
    private final TheMealDbClient client;

    public RecipeSuggestionService(FridgeItemRepository fridgeItemRepository, TheMealDbClient client) {
        this.fridgeItemRepository = fridgeItemRepository;
        this.client = client;
    }

    public List<RecipeSuggestionDto> suggestFromFridge(int maxResults) {
        List<FridgeItem> fridgeItems = fridgeItemRepository.findAll();
        if (fridgeItems.isEmpty()) {
            return List.of();
        }
        List<String> fridgeNamesLower = fridgeItems.stream()
                .map(i -> i.getName().toLowerCase(Locale.FRENCH))
                .toList();

        Map<String, Integer> matchCountByMealId = new HashMap<>();
        Map<String, TheMealDbClient.MealSummary> summaryByMealId = new HashMap<>();
        int failures = 0;

        for (FridgeItem item : fridgeItems) {
            String englishTerm = IngredientTranslations.toEnglishQueryTerm(item.getName());
            try {
                for (TheMealDbClient.MealSummary meal : client.filterByIngredient(englishTerm)) {
                    matchCountByMealId.merge(meal.idMeal(), 1, Integer::sum);
                    summaryByMealId.putIfAbsent(meal.idMeal(), meal);
                }
            } catch (TheMealDbClient.TheMealDbUnavailableException ex) {
                failures++;
            }
        }
        if (failures > 0 && failures == fridgeItems.size()) {
            throw new IllegalStateException("TheMealDB est injoignable pour le moment - reessaie plus tard.");
        }

        List<Map.Entry<String, Integer>> ranked = matchCountByMealId.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(Math.max(1, maxResults))
                .toList();

        List<RecipeSuggestionDto> suggestions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ranked) {
            String mealId = entry.getKey();
            Map<String, Object> detail;
            try {
                detail = client.lookupById(mealId);
            } catch (TheMealDbClient.TheMealDbUnavailableException ex) {
                continue; // on saute cette recette plutot que de faire echouer toute la liste
            }
            if (detail == null) continue;

            List<RecipeDetailDto.IngredientLine> allIngredients = MealIngredientParser.extract(detail);
            List<String> missing = allIngredients.stream()
                    .map(RecipeDetailDto.IngredientLine::ingredient)
                    .filter(ingredientName -> fridgeNamesLower.stream()
                            .noneMatch(fridgeName -> matchesIngredient(ingredientName, fridgeName)))
                    .toList();

            TheMealDbClient.MealSummary summary = summaryByMealId.get(mealId);
            suggestions.add(new RecipeSuggestionDto(
                    mealId,
                    summary != null ? summary.strMeal() : String.valueOf(detail.get("strMeal")),
                    summary != null ? summary.strMealThumb() : String.valueOf(detail.get("strMealThumb")),
                    entry.getValue(),
                    allIngredients.size(),
                    missing));
        }
        return suggestions;
    }

    public RecipeDetailDto findDetail(String mealId) {
        Map<String, Object> meal = client.lookupById(mealId);
        if (meal == null) {
            throw new EntityNotFoundException("Recette introuvable sur TheMealDB: " + mealId);
        }
        return new RecipeDetailDto(
                mealId,
                str(meal.get("strMeal")),
                str(meal.get("strMealThumb")),
                str(meal.get("strCategory")),
                str(meal.get("strArea")),
                str(meal.get("strInstructions")),
                str(meal.get("strYoutube")),
                str(meal.get("strSource")),
                MealIngredientParser.extract(meal));
    }

    /** Correspondance approximative : traduction FR->EN de l'item du frigo, comparee par inclusion. */
    private boolean matchesIngredient(String englishIngredientFromApi, String fridgeNameLower) {
        String translated =
                IngredientTranslations.toEnglishQueryTerm(fridgeNameLower).replace('_', ' ');
        String apiLower = englishIngredientFromApi.toLowerCase(Locale.ENGLISH);
        return apiLower.contains(translated) || translated.contains(apiLower) || apiLower.contains(fridgeNameLower);
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
