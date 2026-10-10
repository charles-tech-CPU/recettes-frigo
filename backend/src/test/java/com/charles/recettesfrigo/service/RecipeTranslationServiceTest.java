package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.charles.recettesfrigo.domain.RecipeTranslation;
import com.charles.recettesfrigo.dto.RecipeDetailDto.IngredientLine;
import com.charles.recettesfrigo.repository.RecipeTranslationRepository;
import com.charles.recettesfrigo.service.RecipeTranslationService.TranslatedMeal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
class RecipeTranslationServiceTest {

    @Mock
    private RecipeTranslationRepository repository;

    @Mock
    private MyMemoryClient translator;

    private RecipeTranslationService service;

    @BeforeEach
    void setUp() {
        service = new RecipeTranslationService(repository, translator, new ObjectMapper());
    }

    private static Map<String, Object> soup() {
        Map<String, Object> meal = new HashMap<>();
        meal.put("strMeal", "Chicken Soup");
        meal.put("strCategory", "Chicken");
        meal.put("strArea", null);
        meal.put("strInstructions", "Boil the water.");
        meal.put("strIngredient1", "Chicken");
        meal.put("strMeasure1", "1 kg");
        meal.put("strIngredient2", "Salt");
        meal.put("strMeasure2", "");
        return meal;
    }

    private static RecipeTranslation cached(boolean full) {
        RecipeTranslation cached = new RecipeTranslation();
        cached.setMealId("1");
        cached.setTitle("Soupe de poulet");
        cached.setInstructions(full ? "Faire bouillir l'eau." : null);
        cached.setIngredientsJson("[{\"ingredient\":\"Poulet\",\"measure\":\"1 kg\"}]");
        cached.setFullTranslation(full);
        return cached;
    }

    @Test
    void listeLesRecettesDejaTraduitesSontRelues() {
        when(repository.findById("1")).thenReturn(Optional.of(cached(false)));

        Map<String, TranslatedMeal> result = service.translateSummaries(Map.of("1", soup()));

        assertThat(result.get("1").title()).isEqualTo("Soupe de poulet");
        verifyNoInteractions(translator);
    }

    @Test
    void listeTitresEtNomsDIngredientsDeToutesLesRecettesEnUneFois() {
        Map<String, Object> rice = new HashMap<>(Map.of("strMeal", "Fried Rice", "strIngredient1", "Rice"));
        Map<String, Map<String, Object>> meals = new LinkedHashMap<>();
        meals.put("1", soup());
        meals.put("2", rice);
        when(translator.translateToFrench(List.of("Chicken Soup", "Chicken", "Salt", "Fried Rice", "Rice")))
                .thenReturn(List.of("Soupe de poulet", "Poulet", "Sel", "Riz frit", "Riz"));

        Map<String, TranslatedMeal> result = service.translateSummaries(meals);

        assertThat(result.get("1").title()).isEqualTo("Soupe de poulet");
        assertThat(result.get("1").ingredients())
                .containsExactly(new IngredientLine("Poulet", "1 kg"), new IngredientLine("Sel", ""));
        assertThat(result.get("2").title()).isEqualTo("Riz frit");
        ArgumentCaptor<RecipeTranslation> saved = ArgumentCaptor.forClass(RecipeTranslation.class);
        verify(repository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).noneMatch(RecipeTranslation::isFullTranslation);
    }

    @Test
    void listeQuotaAtteintLesRecettesRestentEnAnglais() {
        when(translator.translateToFrench(anyList())).thenThrow(new IllegalStateException("quota"));

        Map<String, TranslatedMeal> result = service.translateSummaries(Map.of("1", soup()));

        assertThat(result.get("1").title()).isEqualTo("Chicken Soup");
        assertThat(result.get("1").translated()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void detailUneRecetteCompleteDejaTraduiteEstRelue() {
        when(repository.findById("1")).thenReturn(Optional.of(cached(true)));

        TranslatedMeal result = service.translateFull("1", soup());

        assertThat(result.instructions()).isEqualTo("Faire bouillir l'eau.");
        verifyNoInteractions(translator);
    }

    @Test
    void detailUneTraductionPartielleEstCompleteeEtEnregistree() {
        when(repository.findById("1")).thenReturn(Optional.of(cached(false)));
        when(translator.translateToFrench(
                        Arrays.asList("Chicken Soup", "Chicken", "", "Boil the water.", "Chicken", "1 kg", "Salt", "")))
                .thenReturn(Arrays.asList(
                        "Soupe de poulet", "Poulet", "", "Faire bouillir l'eau.", "Poulet", "1 kg", "Sel", ""));

        TranslatedMeal result = service.translateFull("1", soup());

        assertThat(result.translated()).isTrue();
        assertThat(result.category()).isEqualTo("Poulet");
        assertThat(result.area()).isNull();
        assertThat(result.instructions()).isEqualTo("Faire bouillir l'eau.");
        assertThat(result.ingredients())
                .containsExactly(new IngredientLine("Poulet", "1 kg"), new IngredientLine("Sel", ""));
        ArgumentCaptor<RecipeTranslation> saved = ArgumentCaptor.forClass(RecipeTranslation.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().isFullTranslation()).isTrue();
    }

    @Test
    void detailQuotaAtteintGardeLeTitreDejaTraduitEtLeResteEnAnglais() {
        when(repository.findById("1")).thenReturn(Optional.of(cached(false)));
        when(translator.translateToFrench(anyList())).thenThrow(new ResourceAccessException("timeout"));

        TranslatedMeal result = service.translateFull("1", soup());

        assertThat(result.translated()).isFalse();
        assertThat(result.title()).isEqualTo("Soupe de poulet");
        assertThat(result.instructions()).isEqualTo("Boil the water.");
        verify(repository, never()).save(any());
    }

    @Test
    void detailSansAucuneTraductionResteEnAnglais() {
        when(translator.translateToFrench(anyList())).thenThrow(new IllegalStateException("quota"));

        TranslatedMeal result = service.translateFull("1", soup());

        assertThat(result.title()).isEqualTo("Chicken Soup");
        assertThat(result.translated()).isFalse();
    }
}
