package com.charles.recettesfrigo.web;

import static org.mockito.Mockito.verify;

import com.charles.recettesfrigo.dto.CustomRecipeDto;
import com.charles.recettesfrigo.dto.RecipeImportRequestDto;
import com.charles.recettesfrigo.service.CustomRecipeService;
import com.charles.recettesfrigo.service.RecipeImportService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomRecipeControllerTest {

    @Mock
    private CustomRecipeService customRecipeService;

    @Mock
    private RecipeImportService recipeImportService;

    @InjectMocks
    private CustomRecipeController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        CustomRecipeDto dto =
                new CustomRecipeDto(null, "Quiche", null, null, null, false, null, null, List.of(), List.of());

        controller.findAll();
        controller.findById(1L);
        controller.create(dto);
        controller.update(1L, dto);
        controller.delete(1L);
        controller.importFromUrl(new RecipeImportRequestDto("https://exemple.fr/quiche"));

        verify(customRecipeService).findAll();
        verify(customRecipeService).findById(1L);
        verify(customRecipeService).create(dto);
        verify(customRecipeService).update(1L, dto);
        verify(customRecipeService).delete(1L);
        verify(recipeImportService).importFromUrl("https://exemple.fr/quiche");
    }
}
