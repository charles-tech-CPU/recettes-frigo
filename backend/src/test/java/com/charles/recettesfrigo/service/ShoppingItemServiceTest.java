package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.recettesfrigo.domain.ShoppingItem;
import com.charles.recettesfrigo.dto.ShoppingItemDto;
import com.charles.recettesfrigo.dto.ShoppingItemsAddDto;
import com.charles.recettesfrigo.repository.ShoppingItemRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShoppingItemServiceTest {

    @Mock
    private ShoppingItemRepository shoppingItemRepository;

    @InjectMocks
    private ShoppingItemService service;

    private static ShoppingItem item(Long id, String name, boolean checked) {
        ShoppingItem item = new ShoppingItem();
        item.setId(id);
        item.setName(name);
        item.setChecked(checked);
        return item;
    }

    private static ShoppingItemsAddDto.Item toAdd(String name, String quantity) {
        return new ShoppingItemsAddDto.Item(name, quantity, "Curry");
    }

    @Test
    void lesArticlesDejaAAcheterNeSontPasAjoutesDeuxFois() {
        when(shoppingItemRepository.findAllByOrderByCheckedAscCreatedAtAscIdAsc())
                .thenReturn(List.of(item(1L, "Tomates", false), item(2L, "Riz", true)));
        when(shoppingItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ShoppingItemDto> added = service.addAll(new ShoppingItemsAddDto(List.of(
                toAdd("tomate", "4"),
                toAdd(" Riz ", "500 g"),
                toAdd("Lait de coco", " "),
                toAdd("lait de coco", null))));

        // "tomate" est deja a acheter ; le riz deja coche est a racheter ; le lait de coco n'est ajoute qu'une fois
        assertThat(added).extracting(ShoppingItemDto::name).containsExactly("Riz", "Lait de coco");
        assertThat(added.get(0).quantity()).isEqualTo("500 g");
        assertThat(added.get(0).recipeTitle()).isEqualTo("Curry");
        assertThat(added.get(1).quantity()).isNull();
    }

    @Test
    void cocherUnArticle() {
        when(shoppingItemRepository.findById(1L)).thenReturn(Optional.of(item(1L, "Tomates", false)));
        when(shoppingItemRepository.save(any(ShoppingItem.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.setChecked(1L, true).checked()).isTrue();
    }

    @Test
    void cocherUnArticleInconnuEchoue() {
        when(shoppingItemRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setChecked(9L, true)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void laListeEstRenvoyee() {
        when(shoppingItemRepository.findAllByOrderByCheckedAscCreatedAtAscIdAsc())
                .thenReturn(List.of(item(1L, "Tomates", false)));

        assertThat(service.findAll()).extracting(ShoppingItemDto::name).containsExactly("Tomates");
    }

    @Test
    void retirerUnArticleOuLesArticlesCoches() {
        service.delete(3L);
        service.deleteChecked();

        verify(shoppingItemRepository).deleteById(3L);
        verify(shoppingItemRepository).deleteByCheckedTrue();
    }
}
