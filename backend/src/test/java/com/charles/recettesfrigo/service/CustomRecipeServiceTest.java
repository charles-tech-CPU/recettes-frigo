package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.recettesfrigo.domain.CustomRecipe;
import com.charles.recettesfrigo.dto.CustomRecipeDto;
import com.charles.recettesfrigo.repository.CustomRecipeRepository;
import com.charles.recettesfrigo.repository.RecipeFavoriteRepository;
import com.charles.recettesfrigo.repository.RecipeNoteRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomRecipeServiceTest {

    @Mock
    private CustomRecipeRepository customRecipeRepository;

    @Mock
    private RecipeNoteRepository recipeNoteRepository;

    @Mock
    private RecipeFavoriteRepository recipeFavoriteRepository;

    @InjectMocks
    private CustomRecipeService service;

    private static CustomRecipeDto soupe(boolean thermomix) {
        return new CustomRecipeDto(
                null,
                " Soupe de carottes ",
                "Soupe",
                4,
                30,
                thermomix,
                " ",
                "https://exemple.fr/soupe",
                List.of(
                        new CustomRecipeDto.Ingredient("Carottes", "500 g"),
                        new CustomRecipeDto.Ingredient("Oignon", "")),
                List.of(
                        new CustomRecipeDto.Step("Mixer les carottes.", "10 s", null, "5", false),
                        new CustomRecipeDto.Step("Cuire.", "20 min", "100°C", "1", true)));
    }

    @Test
    void creerUneRecetteThermomixGardeLesReglagesEtLOrdreDesEtapes() {
        when(customRecipeRepository.save(any(CustomRecipe.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomRecipeDto created = service.create(soupe(true));

        assertThat(created.title()).isEqualTo("Soupe de carottes");
        assertThat(created.imageUrl()).isNull();
        assertThat(created.ingredients())
                .containsExactly(
                        new CustomRecipeDto.Ingredient("Carottes", "500 g"),
                        new CustomRecipeDto.Ingredient("Oignon", null));
        assertThat(created.steps())
                .containsExactly(
                        new CustomRecipeDto.Step("Mixer les carottes.", "10 s", null, "5", false),
                        new CustomRecipeDto.Step("Cuire.", "20 min", "100°C", "1", true));
    }

    @Test
    void uneRecetteClassiqueNeGardePasDeReglageThermomix() {
        when(customRecipeRepository.save(any(CustomRecipe.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomRecipeDto created = service.create(soupe(false));

        assertThat(created.steps())
                .containsExactly(
                        new CustomRecipeDto.Step("Mixer les carottes.", null, null, null, false),
                        new CustomRecipeDto.Step("Cuire.", null, null, null, false));
    }

    @Test
    void modifierRemplaceIngredientsEtEtapes() {
        CustomRecipe existing = new CustomRecipe();
        existing.setId(3L);
        when(customRecipeRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(customRecipeRepository.save(any(CustomRecipe.class))).thenAnswer(inv -> inv.getArgument(0));
        service.update(3L, soupe(true));

        CustomRecipeDto updated = service.update(
                3L,
                new CustomRecipeDto(
                        null,
                        "Soupe",
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        List.of(new CustomRecipeDto.Ingredient("Poireau", null)),
                        null));

        assertThat(updated.id()).isEqualTo(3L);
        assertThat(updated.ingredients())
                .extracting(CustomRecipeDto.Ingredient::name)
                .containsExactly("Poireau");
        assertThat(updated.steps()).isEmpty();
    }

    @Test
    void lireOuModifierUneRecetteInconnueEchoue() {
        when(customRecipeRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void laListeEstTrieeParTitre() {
        CustomRecipe recipe = new CustomRecipe();
        recipe.setId(1L);
        recipe.setTitle("Quiche");
        when(customRecipeRepository.findAllByOrderByTitleAsc()).thenReturn(List.of(recipe));

        assertThat(service.findAll()).extracting(CustomRecipeDto::title).containsExactly("Quiche");
    }

    @Test
    void supprimerUneRecetteSupprimeAussiSesNotes() {
        service.delete(4L);

        verify(customRecipeRepository).deleteById(4L);
        verify(recipeNoteRepository).deleteByRecipeKey("perso:4");
        verify(recipeFavoriteRepository).deleteById("perso:4");
    }
}
