package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.recettesfrigo.domain.RecipeNote;
import com.charles.recettesfrigo.dto.RecipeNoteCreateDto;
import com.charles.recettesfrigo.dto.RecipeNoteDto;
import com.charles.recettesfrigo.repository.RecipeNoteRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeNoteServiceTest {

    @Mock
    private RecipeNoteRepository recipeNoteRepository;

    @InjectMocks
    private RecipeNoteService service;

    @Test
    void ajouterUneNoteSurUneRecetteTheMealDb() {
        when(recipeNoteRepository.save(any(RecipeNote.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeNoteDto note = service.create(new RecipeNoteCreateDto("mealdb:52772", "  Cuisson 200°C, 25 min "));

        assertThat(note.recipeKey()).isEqualTo("mealdb:52772");
        assertThat(note.content()).isEqualTo("Cuisson 200°C, 25 min");
        assertThat(note.createdAt()).isNotNull();
    }

    @Test
    void lesNotesDUneRecettePersoSontRenvoyees() {
        RecipeNote note = new RecipeNote();
        note.setRecipeKey("perso:3");
        note.setContent("Doubler l'ail");
        when(recipeNoteRepository.findByRecipeKeyOrderByCreatedAtAsc("perso:3")).thenReturn(List.of(note));

        assertThat(service.findByRecipe("perso:3"))
                .extracting(RecipeNoteDto::content)
                .containsExactly("Doubler l'ail");
    }

    @Test
    void uneCleDeRecetteInvalideEstRefusee() {
        assertThatThrownBy(() -> service.findByRecipe("autre:1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(new RecipeNoteCreateDto("mealdb:abc", "x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void supprimerUneNote() {
        service.delete(5L);

        verify(recipeNoteRepository).deleteById(5L);
    }
}
