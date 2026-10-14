package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.domain.FridgeItem;
import com.charles.recettesfrigo.dto.CustomRecipeDto;
import com.charles.recettesfrigo.dto.FridgeItemDto;
import com.charles.recettesfrigo.dto.RecipeDetailDto;
import com.charles.recettesfrigo.dto.RecipeSuggestionDto;
import com.charles.recettesfrigo.repository.FridgeItemRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * Coeur de l'appli : propose des recettes a partir de l'inventaire du frigo,
 * TheMealDB d'un cote et recettes perso de l'autre, classees ensemble par nombre
 * d'ingredients du frigo utilises.
 * <p>
 * TheMealDB ne sait filtrer que par UN SEUL ingredient a la fois
 * (filter.php?i=...), donc on interroge l'API une fois par ingredient du
 * frigo, on compte combien de fois chaque recette ressort (= combien
 * d'ingredients du frigo elle utilise), puis on trie les recettes par ce
 * score decroissant - sans jamais rien inventer : le detail (ingredients
 * manquants) n'est calcule qu'a partir de la vraie liste d'ingredients de la
 * recette, recuperee via lookup.php. Titres et ingredients manquants sont
 * affiches en francais (RecipeTranslationService, via MyMemory).
 */
@Service
public class RecipeSuggestionService {

    private final FridgeItemRepository fridgeItemRepository;
    private final TheMealDbClient client;
    private final RecipeTranslationService translationService;
    private final CustomRecipeService customRecipeService;

    public RecipeSuggestionService(
            FridgeItemRepository fridgeItemRepository,
            TheMealDbClient client,
            RecipeTranslationService translationService,
            CustomRecipeService customRecipeService) {
        this.fridgeItemRepository = fridgeItemRepository;
        this.client = client;
        this.translationService = translationService;
        this.customRecipeService = customRecipeService;
    }

    public List<RecipeSuggestionDto> suggestFromFridge(int maxResults) {
        List<FridgeItem> fridgeItems = fridgeItemRepository.findAll();
        if (fridgeItems.isEmpty()) {
            return List.of();
        }
        List<RecipeSuggestionDto> suggestions = new ArrayList<>(customSuggestions(fridgeItems));
        try {
            suggestions.addAll(mealDbSuggestions(fridgeItems, maxResults));
        } catch (IllegalStateException ex) {
            // TheMealDB injoignable : les recettes perso suffisent si on en a
            if (suggestions.isEmpty()) {
                throw ex;
            }
        }
        // Tri stable : a score egal, les recettes perso (ajoutees en premier) passent devant
        suggestions.sort(Comparator.comparingInt(RecipeSuggestionDto::matchedIngredientCount)
                .reversed());
        return suggestions;
    }

    /** Recettes perso utilisant au moins un ingredient du frigo (correspondance en francais). */
    private List<RecipeSuggestionDto> customSuggestions(List<FridgeItem> fridgeItems) {
        List<RecipeSuggestionDto> suggestions = new ArrayList<>();
        for (CustomRecipeDto recipe : customRecipeService.findAll()) {
            List<String> ingredientNames = ingredientNames(recipe);
            List<FridgeItem> used = usedByCustomRecipe(ingredientNames, fridgeItems);
            if (used.isEmpty()) {
                continue;
            }
            List<String> missing = ingredientNames.stream()
                    .filter(ingredient -> fridgeItems.stream()
                            .noneMatch(fridge -> FrenchIngredientMatcher.matches(ingredient, fridge.getName())))
                    .toList();
            suggestions.add(new RecipeSuggestionDto(
                    String.valueOf(recipe.id()),
                    "perso",
                    recipe.title(),
                    recipe.imageUrl(),
                    recipe.thermomix(),
                    used.size(),
                    ingredientNames.size(),
                    missing,
                    ids(used)));
        }
        return suggestions;
    }

    private List<RecipeSuggestionDto> mealDbSuggestions(List<FridgeItem> fridgeItems, int maxResults) {
        List<String> fridgeNames = fridgeItems.stream().map(FridgeItem::getName).toList();

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

        // Detail de chaque recette retenue (liste complete des ingredients)
        Map<String, Map<String, Object>> detailsById = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : ranked) {
            try {
                Map<String, Object> detail = client.lookupById(entry.getKey());
                if (detail != null) {
                    detailsById.put(entry.getKey(), detail);
                }
            } catch (TheMealDbClient.TheMealDbUnavailableException ex) {
                // on saute cette recette plutot que de faire echouer toute la liste
            }
        }
        // Titres et noms d'ingredients traduits en une fois pour toute la liste
        Map<String, RecipeTranslationService.TranslatedMeal> frenchById =
                translationService.translateSummaries(detailsById);

        List<RecipeSuggestionDto> suggestions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : ranked) {
            String mealId = entry.getKey();
            Map<String, Object> detail = detailsById.get(mealId);
            if (detail == null) continue;

            // Ingredients manquants calcules sur les noms anglais d'origine, affiches traduits
            List<RecipeDetailDto.IngredientLine> allIngredients = MealIngredientParser.extract(detail);
            RecipeTranslationService.TranslatedMeal french = frenchById.get(mealId);
            List<String> missing = new ArrayList<>();
            for (int i = 0; i < allIngredients.size(); i++) {
                String englishName = allIngredients.get(i).ingredient();
                if (fridgeNames.stream().noneMatch(fridgeName -> matchesIngredient(englishName, fridgeName))) {
                    missing.add(
                            french != null && i < french.ingredients().size()
                                    ? french.ingredients().get(i).ingredient()
                                    : englishName);
                }
            }

            TheMealDbClient.MealSummary summary = summaryByMealId.get(mealId);
            String title =
                    french != null && french.title() != null && !french.title().isBlank()
                            ? french.title()
                            : summary != null ? summary.strMeal() : String.valueOf(detail.get("strMeal"));
            suggestions.add(new RecipeSuggestionDto(
                    mealId,
                    "mealdb",
                    title,
                    summary != null ? summary.strMealThumb() : String.valueOf(detail.get("strMealThumb")),
                    false,
                    entry.getValue(),
                    allIngredients.size(),
                    missing.stream().distinct().toList(),
                    ids(usedByMeal(allIngredients, fridgeItems))));
        }
        return suggestions;
    }

    /** Ingredients du frigo qu'utilise une recette ("J'ai cuisine ca" : quoi retirer du frigo). */
    public List<FridgeItemDto> fridgeItemsUsedBy(String recipeKey) {
        RecipeKeys.check(recipeKey);
        List<FridgeItem> fridgeItems = fridgeItemRepository.findAllByOrderByCategoryAscNameAsc();
        List<FridgeItem> used;
        if (RecipeKeys.isCustom(recipeKey)) {
            CustomRecipeDto recipe = customRecipeService.findById(Long.valueOf(RecipeKeys.id(recipeKey)));
            used = usedByCustomRecipe(ingredientNames(recipe), fridgeItems);
        } else {
            String mealId = RecipeKeys.id(recipeKey);
            Map<String, Object> meal = client.lookupById(mealId);
            if (meal == null) {
                throw new EntityNotFoundException("Recette introuvable sur TheMealDB: " + mealId);
            }
            used = usedByMeal(MealIngredientParser.extract(meal), fridgeItems);
        }
        return used.stream().map(FridgeItemService::toDto).toList();
    }

    private static List<String> ingredientNames(CustomRecipeDto recipe) {
        return recipe.ingredients().stream()
                .map(CustomRecipeDto.Ingredient::name)
                .toList();
    }

    private static List<FridgeItem> usedByCustomRecipe(List<String> ingredientNames, List<FridgeItem> fridgeItems) {
        return fridgeItems.stream()
                .filter(fridge -> ingredientNames.stream()
                        .anyMatch(ingredient -> FrenchIngredientMatcher.matches(ingredient, fridge.getName())))
                .toList();
    }

    private static List<FridgeItem> usedByMeal(
            List<RecipeDetailDto.IngredientLine> ingredients, List<FridgeItem> fridgeItems) {
        return fridgeItems.stream()
                .filter(fridge ->
                        ingredients.stream().anyMatch(line -> matchesIngredient(line.ingredient(), fridge.getName())))
                .toList();
    }

    private static List<Long> ids(List<FridgeItem> items) {
        return items.stream().map(FridgeItem::getId).toList();
    }

    public RecipeDetailDto findDetail(String mealId) {
        Map<String, Object> meal = client.lookupById(mealId);
        if (meal == null) {
            throw new EntityNotFoundException("Recette introuvable sur TheMealDB: " + mealId);
        }
        RecipeTranslationService.TranslatedMeal french = translationService.translateFull(mealId, meal);
        return new RecipeDetailDto(
                mealId,
                french.title(),
                str(meal.get("strMealThumb")),
                french.category(),
                french.area(),
                french.instructions(),
                str(meal.get("strYoutube")),
                str(meal.get("strSource")),
                french.ingredients(),
                french.translated());
    }

    /**
     * Correspondance approximative : traduction FR->EN de l'item du frigo, comparee par mots
     * entiers et au singulier ("Chicken Breast" contient "chicken", mais "Oxtail" ne contient
     * pas "ail" ni "Pineapple" "apple").
     */
    static boolean matchesIngredient(String englishIngredientFromApi, String fridgeName) {
        String api = words(englishIngredientFromApi);
        String translated = words(IngredientTranslations.toEnglishQueryTerm(fridgeName));
        String french = words(IngredientTranslations.normalize(fridgeName));
        return containsWords(api, translated) || containsWords(translated, api) || containsWords(api, french);
    }

    /** "Chicken_Breasts" -> " chicken breast " : mots en minuscules, au singulier, entoures d'espaces. */
    private static String words(String name) {
        String[] parts = name.toLowerCase(Locale.ENGLISH).split("[^a-z0-9]+");
        StringBuilder result = new StringBuilder(" ");
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(singular(part)).append(' ');
            }
        }
        return result.toString();
    }

    private static String singular(String word) {
        if (word.length() > 4 && word.endsWith("ies")) return word.substring(0, word.length() - 3) + "y";
        if (word.length() > 4 && word.endsWith("oes")) return word.substring(0, word.length() - 2);
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss"))
            return word.substring(0, word.length() - 1);
        return word;
    }

    private static boolean containsWords(String text, String searched) {
        return !searched.isBlank() && text.contains(searched);
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
