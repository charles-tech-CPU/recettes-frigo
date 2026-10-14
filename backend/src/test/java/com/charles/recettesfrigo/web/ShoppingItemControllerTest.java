package com.charles.recettesfrigo.web;

import static org.mockito.Mockito.verify;

import com.charles.recettesfrigo.dto.ShoppingItemCheckDto;
import com.charles.recettesfrigo.dto.ShoppingItemsAddDto;
import com.charles.recettesfrigo.service.ShoppingItemService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShoppingItemControllerTest {

    @Mock
    private ShoppingItemService shoppingItemService;

    @InjectMocks
    private ShoppingItemController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        ShoppingItemsAddDto dto = new ShoppingItemsAddDto(List.of(new ShoppingItemsAddDto.Item("Riz", null, null)));

        controller.findAll();
        controller.addAll(dto);
        controller.setChecked(1L, new ShoppingItemCheckDto(true));
        controller.delete(2L);
        controller.deleteChecked();

        verify(shoppingItemService).findAll();
        verify(shoppingItemService).addAll(dto);
        verify(shoppingItemService).setChecked(1L, true);
        verify(shoppingItemService).delete(2L);
        verify(shoppingItemService).deleteChecked();
    }
}
