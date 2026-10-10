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
import java.util.HashMap;
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
    private DeepLClient deepL;

    private RecipeTranslationService service;

    @BeforeEach
    void setUp() {
        service = new RecipeTranslationService(repository, deepL, new ObjectMapper());
    }

    private static Map<String, Object> meal() {
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

    @Test
    void uneRecetteDejaTraduiteEstRelueEnBaseSansAppelerDeepL() {
        RecipeTranslation cached = new RecipeTranslation();
        cached.setTitle("Soupe de poulet");
        cached.setIngredientsJson("[{\"ingredient\":\"Poulet\",\"measure\":\"1 kg\"}]");
        when(repository.findById("1")).thenReturn(Optional.of(cached));

        TranslatedMeal result = service.translate("1", meal());

        assertThat(result.title()).isEqualTo("Soupe de poulet");
        assertThat(result.translated()).isTrue();
        assertThat(result.ingredients()).containsExactly(new IngredientLine("Poulet", "1 kg"));
        verifyNoInteractions(deepL);
    }

    @Test
    void sansCleDeepLLaRecetteResteEnAnglais() {
        when(deepL.isEnabled()).thenReturn(false);

        TranslatedMeal result = service.translate("1", meal());

        assertThat(result.title()).isEqualTo("Chicken Soup");
        assertThat(result.translated()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void laRecetteEstTraduiteEnUneRequeteSansLesTextesVidesPuisEnregistree() {
        when(deepL.isEnabled()).thenReturn(true);
        // envoyes : titre, categorie, instructions, Chicken, 1 kg, Salt (origine et mesure vides exclues)
        when(deepL.translateToFrench(List.of("Chicken Soup", "Chicken", "Boil the water.", "Chicken", "1 kg", "Salt")))
                .thenReturn(List.of("Soupe de poulet", "Poulet", "Faire bouillir l'eau.", "Poulet", "1 kg", "Sel"));

        TranslatedMeal result = service.translate("1", meal());

        assertThat(result.translated()).isTrue();
        assertThat(result.title()).isEqualTo("Soupe de poulet");
        assertThat(result.area()).isNull();
        assertThat(result.instructions()).isEqualTo("Faire bouillir l'eau.");
        assertThat(result.ingredients())
                .containsExactly(new IngredientLine("Poulet", "1 kg"), new IngredientLine("Sel", ""));
        ArgumentCaptor<RecipeTranslation> saved = ArgumentCaptor.forClass(RecipeTranslation.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getMealId()).isEqualTo("1");
        assertThat(saved.getValue().getIngredientsJson()).contains("\"Sel\"");
    }

    @Test
    void uneErreurDeepLLaisseLaRecetteEnAnglais() {
        when(deepL.isEnabled()).thenReturn(true);
        when(deepL.translateToFrench(anyList())).thenThrow(new ResourceAccessException("timeout"));

        TranslatedMeal result = service.translate("1", meal());

        assertThat(result.translated()).isFalse();
        assertThat(result.title()).isEqualTo("Chicken Soup");
        verify(repository, never()).save(any());
    }
}
