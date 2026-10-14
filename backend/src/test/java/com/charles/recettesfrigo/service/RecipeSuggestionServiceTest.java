package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.charles.recettesfrigo.domain.FridgeItem;
import com.charles.recettesfrigo.dto.CustomRecipeDto;
import com.charles.recettesfrigo.dto.RecipeDetailDto;
import com.charles.recettesfrigo.dto.RecipeDetailDto.IngredientLine;
import com.charles.recettesfrigo.dto.RecipeSuggestionDto;
import com.charles.recettesfrigo.repository.FridgeItemRepository;
import com.charles.recettesfrigo.service.RecipeTranslationService.TranslatedMeal;
import com.charles.recettesfrigo.service.TheMealDbClient.MealSummary;
import com.charles.recettesfrigo.service.TheMealDbClient.TheMealDbUnavailableException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeSuggestionServiceTest {

    @Mock
    private FridgeItemRepository fridgeItemRepository;

    @Mock
    private TheMealDbClient client;

    @Mock
    private RecipeTranslationService translationService;

    @Mock
    private CustomRecipeService customRecipeService;

    @InjectMocks
    private RecipeSuggestionService service;

    private static FridgeItem item(String name) {
        FridgeItem item = new FridgeItem();
        item.setName(name);
        return item;
    }

    private static MealSummary summary(String id, String title) {
        return new MealSummary(id, title, "https://img/" + id + ".jpg");
    }

    /** Traduction "neutre" : les recettes restent en anglais (quota de traduction epuise). */
    private void noTranslation() {
        when(translationService.translateSummaries(any())).thenAnswer(inv -> {
            Map<String, Map<String, Object>> meals = inv.getArgument(0);
            Map<String, TranslatedMeal> english = new java.util.HashMap<>();
            meals.forEach((id, meal) -> english.put(
                    id,
                    new TranslatedMeal(
                            (String) meal.get("strMeal"),
                            null,
                            null,
                            null,
                            MealIngredientParser.extract(meal),
                            false)));
            return english;
        });
    }

    private static CustomRecipeDto myRecipe(long id, String title, boolean thermomix, String... ingredients) {
        return new CustomRecipeDto(
                id,
                title,
                null,
                null,
                null,
                thermomix,
                null,
                null,
                java.util.Arrays.stream(ingredients)
                        .map(name -> new CustomRecipeDto.Ingredient(name, null))
                        .toList(),
                List.of());
    }

    @Test
    void frigoVideAucuneSuggestion() {
        when(fridgeItemRepository.findAll()).thenReturn(List.of());

        assertThat(service.suggestFromFridge(12)).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void lesRecettesSontTrieesParNombreDIngredientsDuFrigoEtLesManquantsCalcules() {
        noTranslation();
        when(fridgeItemRepository.findAll()).thenReturn(List.of(item("Poulet"), item("Riz")));
        when(client.filterByIngredient("chicken")).thenReturn(List.of(summary("1", "Curry"), summary("2", "Soup")));
        when(client.filterByIngredient("rice")).thenReturn(List.of(summary("1", "Curry")));
        when(client.lookupById("1"))
                .thenReturn(Map.of(
                        "strMeal", "Curry",
                        "strIngredient1", "Chicken Breast",
                        "strIngredient2", "Rice",
                        "strIngredient3", "Coconut Milk",
                        "strIngredient4", "Coconut Milk"));
        when(client.lookupById("2"))
                .thenReturn(Map.of("strMeal", "Soup", "strIngredient1", "Chicken", "strIngredient2", "Water"));

        List<RecipeSuggestionDto> result = service.suggestFromFridge(12);

        assertThat(result).extracting(RecipeSuggestionDto::id).containsExactly("1", "2");
        RecipeSuggestionDto curry = result.get(0);
        assertThat(curry.source()).isEqualTo("mealdb");
        assertThat(curry.title()).isEqualTo("Curry");
        assertThat(curry.matchedIngredientCount()).isEqualTo(2);
        assertThat(curry.totalIngredientCount()).isEqualTo(4);
        // un ingredient cite deux fois n'apparait qu'une fois dans les manquants
        assertThat(curry.missingIngredients()).containsExactly("Coconut Milk");
        assertThat(result.get(1).missingIngredients()).containsExactly("Water");
    }

    @Test
    void titreEtIngredientsManquantsSontAffichesTraduits() {
        when(fridgeItemRepository.findAll()).thenReturn(List.of(item("Poulet")));
        when(client.filterByIngredient("chicken")).thenReturn(List.of(summary("1", "Chicken Soup")));
        Map<String, Object> meal = Map.of("strIngredient1", "Chicken", "strIngredient2", "Water");
        when(client.lookupById("1")).thenReturn(meal);
        when(translationService.translateSummaries(Map.of("1", meal)))
                .thenReturn(Map.of(
                        "1",
                        new TranslatedMeal(
                                "Soupe de poulet",
                                "Poulet",
                                null,
                                null,
                                List.of(new IngredientLine("Poulet", ""), new IngredientLine("Eau", "")),
                                true)));

        RecipeSuggestionDto soup = service.suggestFromFridge(12).get(0);

        assertThat(soup.title()).isEqualTo("Soupe de poulet");
        assertThat(soup.missingIngredients()).containsExactly("Eau");
    }

    @Test
    void lesRecettesPersoSontClasseesAvecLesAutres() {
        noTranslation();
        when(fridgeItemRepository.findAll()).thenReturn(List.of(item("Poulet"), item("Carottes"), item("Riz")));
        when(customRecipeService.findAll())
                .thenReturn(List.of(
                        myRecipe(7L, "Poulet aux carottes", true, "Blanc de poulet", "Carotte râpée", "Crème"),
                        myRecipe(8L, "Tarte aux pommes", false, "Pommes", "Pâte brisée")));
        when(client.filterByIngredient(anyString())).thenReturn(List.of());
        when(client.filterByIngredient("chicken")).thenReturn(List.of(summary("1", "Soup")));
        when(client.lookupById("1")).thenReturn(Map.of("strMeal", "Soup", "strIngredient1", "Chicken"));

        List<RecipeSuggestionDto> result = service.suggestFromFridge(12);

        // la tarte n'utilise rien du frigo : absente ; la recette perso (2 ingredients) passe devant
        assertThat(result).extracting(RecipeSuggestionDto::id).containsExactly("7", "1");
        RecipeSuggestionDto mine = result.get(0);
        assertThat(mine.source()).isEqualTo("perso");
        assertThat(mine.thermomix()).isTrue();
        assertThat(mine.matchedIngredientCount()).isEqualTo(2);
        assertThat(mine.missingIngredients()).containsExactly("Crème");
    }

    @Test
    void sansTheMealDbLesRecettesPersoRestentProposees() {
        when(fridgeItemRepository.findAll()).thenReturn(List.of(item("Poulet")));
        when(customRecipeService.findAll()).thenReturn(List.of(myRecipe(7L, "Poulet rôti", false, "Poulet")));
        when(client.filterByIngredient(anyString())).thenThrow(new TheMealDbUnavailableException("ko", null));

        assertThat(service.suggestFromFridge(12))
                .extracting(RecipeSuggestionDto::id)
                .containsExactly("7");
    }

    @Test
    void uneRecetteDontLeDetailEchoueEstIgnoree() {
        when(fridgeItemRepository.findAll()).thenReturn(List.of(item("Poulet")));
        when(client.filterByIngredient("chicken")).thenReturn(List.of(summary("1", "A"), summary("2", "B")));
        when(client.lookupById("1")).thenThrow(new TheMealDbUnavailableException("ko", null));
        when(client.lookupById("2")).thenReturn(null);

        assertThat(service.suggestFromFridge(12)).isEmpty();
    }

    @Test
    void theMealDbInjoignableEtAucuneRecettePerso() {
        when(fridgeItemRepository.findAll()).thenReturn(List.of(item("Poulet")));
        when(client.filterByIngredient(anyString())).thenThrow(new TheMealDbUnavailableException("ko", null));

        assertThatThrownBy(() -> service.suggestFromFridge(12)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void detailDUneRecetteTraduite() {
        Map<String, Object> meal = Map.of("strMeal", "Teriyaki Chicken Casserole", "strMealThumb", "https://img");
        when(client.lookupById("52772")).thenReturn(meal);
        when(translationService.translateFull(eq("52772"), any()))
                .thenReturn(new TranslatedMeal(
                        "Gratin de poulet teriyaki",
                        "Poulet",
                        "Japonais",
                        "Préchauffer le four.",
                        List.of(new IngredientLine("sauce soja", "3/4 tasse")),
                        true));

        RecipeDetailDto detail = service.findDetail("52772");

        assertThat(detail.title()).isEqualTo("Gratin de poulet teriyaki");
        assertThat(detail.area()).isEqualTo("Japonais");
        assertThat(detail.thumbnailUrl()).isEqualTo("https://img");
        assertThat(detail.videoUrl()).isNull();
        assertThat(detail.translated()).isTrue();
        assertThat(detail.ingredients()).containsExactly(new IngredientLine("sauce soja", "3/4 tasse"));
    }

    @Test
    void detailDUneRecetteInconnue() {
        when(client.lookupById("0")).thenReturn(null);

        assertThatThrownBy(() -> service.findDetail("0")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void laCorrespondanceDesIngredientsSeFaitParMotsEntiers() {
        assertThat(RecipeSuggestionService.matchesIngredient("Chicken Breast", "Poulet")).isTrue();
        assertThat(RecipeSuggestionService.matchesIngredient("Tomato", "Tomates")).isTrue();
        assertThat(RecipeSuggestionService.matchesIngredient("Black Pepper", "Poivre")).isTrue();
        assertThat(RecipeSuggestionService.matchesIngredient("Kidney Beans", "Haricots rouges")).isTrue();

        assertThat(RecipeSuggestionService.matchesIngredient("Oxtail", "Ail")).isFalse();
        assertThat(RecipeSuggestionService.matchesIngredient("Mussels", "Sel")).isFalse();
        assertThat(RecipeSuggestionService.matchesIngredient("Pineapple", "Pomme")).isFalse();
    }
}
