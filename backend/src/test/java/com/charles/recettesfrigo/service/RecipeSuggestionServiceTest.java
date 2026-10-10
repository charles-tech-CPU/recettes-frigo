package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.charles.recettesfrigo.domain.FridgeItem;
import com.charles.recettesfrigo.dto.RecipeDetailDto;
import com.charles.recettesfrigo.dto.RecipeSuggestionDto;
import com.charles.recettesfrigo.repository.FridgeItemRepository;
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

    @Test
    void frigoVideAucuneSuggestion() {
        when(fridgeItemRepository.findAll()).thenReturn(List.of());

        assertThat(service.suggestFromFridge(12)).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void lesRecettesSontTrieesParNombreDIngredientsDuFrigoEtLesManquantsCalcules() {
        when(fridgeItemRepository.findAll()).thenReturn(List.of(item("Poulet"), item("Riz")));
        when(client.filterByIngredient("chicken")).thenReturn(List.of(summary("1", "Curry"), summary("2", "Soup")));
        when(client.filterByIngredient("rice")).thenReturn(List.of(summary("1", "Curry")));
        when(client.lookupById("1"))
                .thenReturn(Map.of(
                        "strIngredient1", "Chicken Breast",
                        "strIngredient2", "Rice",
                        "strIngredient3", "Coconut Milk"));
        when(client.lookupById("2")).thenReturn(Map.of("strIngredient1", "Chicken", "strIngredient2", "Water"));

        List<RecipeSuggestionDto> result = service.suggestFromFridge(12);

        assertThat(result).extracting(RecipeSuggestionDto::mealId).containsExactly("1", "2");
        RecipeSuggestionDto curry = result.get(0);
        assertThat(curry.title()).isEqualTo("Curry");
        assertThat(curry.matchedIngredientCount()).isEqualTo(2);
        assertThat(curry.totalIngredientCount()).isEqualTo(3);
        assertThat(curry.missingIngredients()).containsExactly("Coconut Milk");
        assertThat(result.get(1).missingIngredients()).containsExactly("Water");
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
    void theMealDbInjoignablePourTousLesIngredients() {
        when(fridgeItemRepository.findAll()).thenReturn(List.of(item("Poulet")));
        when(client.filterByIngredient(anyString())).thenThrow(new TheMealDbUnavailableException("ko", null));

        assertThatThrownBy(() -> service.suggestFromFridge(12)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void detailDUneRecette() {
        when(client.lookupById("52772"))
                .thenReturn(Map.of(
                        "strMeal", "Teriyaki Chicken Casserole",
                        "strCategory", "Chicken",
                        "strIngredient1", "soy sauce",
                        "strMeasure1", "3/4 cup"));

        RecipeDetailDto detail = service.findDetail("52772");

        assertThat(detail.title()).isEqualTo("Teriyaki Chicken Casserole");
        assertThat(detail.category()).isEqualTo("Chicken");
        assertThat(detail.videoUrl()).isNull();
        assertThat(detail.ingredients()).containsExactly(new RecipeDetailDto.IngredientLine("soy sauce", "3/4 cup"));
    }

    @Test
    void detailDUneRecetteInconnue() {
        when(client.lookupById("0")).thenReturn(null);

        assertThatThrownBy(() -> service.findDetail("0")).isInstanceOf(EntityNotFoundException.class);
    }
}
