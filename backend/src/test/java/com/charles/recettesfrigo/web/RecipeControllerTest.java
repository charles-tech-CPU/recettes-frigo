package com.charles.recettesfrigo.web;

import static org.mockito.Mockito.verify;

import com.charles.recettesfrigo.service.RecipeSuggestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeControllerTest {

    @Mock
    private RecipeSuggestionService recipeSuggestionService;

    @InjectMocks
    private RecipeController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        controller.suggestions(12);
        controller.detail("52772");

        verify(recipeSuggestionService).suggestFromFridge(12);
        verify(recipeSuggestionService).findDetail("52772");
    }
}
