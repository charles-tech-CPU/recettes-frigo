package com.charles.recettesfrigo.web;

import static org.mockito.Mockito.verify;

import com.charles.recettesfrigo.dto.RecipeNoteCreateDto;
import com.charles.recettesfrigo.service.RecipeNoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeNoteControllerTest {

    @Mock
    private RecipeNoteService recipeNoteService;

    @InjectMocks
    private RecipeNoteController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        RecipeNoteCreateDto dto = new RecipeNoteCreateDto("mealdb:1", "Cuisson 200°C");

        controller.findByRecipe("mealdb:1");
        controller.create(dto);
        controller.delete(2L);

        verify(recipeNoteService).findByRecipe("mealdb:1");
        verify(recipeNoteService).create(dto);
        verify(recipeNoteService).delete(2L);
    }
}
