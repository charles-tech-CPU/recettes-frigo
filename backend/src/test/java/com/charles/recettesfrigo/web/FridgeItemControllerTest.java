package com.charles.recettesfrigo.web;

import static org.mockito.Mockito.verify;

import com.charles.recettesfrigo.dto.FridgeItemCreateDto;
import com.charles.recettesfrigo.service.FridgeItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FridgeItemControllerTest {

    @Mock
    private FridgeItemService fridgeItemService;

    @InjectMocks
    private FridgeItemController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        FridgeItemCreateDto dto = new FridgeItemCreateDto("Poulet", "2", "Viande", null);

        controller.findAll();
        controller.create(dto);
        controller.update(1L, dto);
        controller.delete(1L);

        verify(fridgeItemService).findAll();
        verify(fridgeItemService).create(dto);
        verify(fridgeItemService).update(1L, dto);
        verify(fridgeItemService).delete(1L);
    }
}
