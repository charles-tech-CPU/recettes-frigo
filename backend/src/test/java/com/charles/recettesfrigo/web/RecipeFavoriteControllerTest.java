package com.charles.recettesfrigo.web;

import static org.mockito.Mockito.verify;

import com.charles.recettesfrigo.dto.RecipeFavoriteDto;
import com.charles.recettesfrigo.service.RecipeFavoriteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeFavoriteControllerTest {

    @Mock
    private RecipeFavoriteService recipeFavoriteService;

    @InjectMocks
    private RecipeFavoriteController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        RecipeFavoriteDto dto = new RecipeFavoriteDto("mealdb:1", "Curry", null);

        controller.findAll();
        controller.add(dto);
        controller.remove("mealdb:1");

        verify(recipeFavoriteService).findAll();
        verify(recipeFavoriteService).add(dto);
        verify(recipeFavoriteService).remove("mealdb:1");
    }
}
