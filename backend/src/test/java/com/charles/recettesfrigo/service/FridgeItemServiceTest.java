package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.recettesfrigo.domain.FridgeItem;
import com.charles.recettesfrigo.dto.FridgeItemCreateDto;
import com.charles.recettesfrigo.dto.FridgeItemDto;
import com.charles.recettesfrigo.repository.FridgeItemRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FridgeItemServiceTest {

    @Mock
    private FridgeItemRepository fridgeItemRepository;

    @InjectMocks
    private FridgeItemService fridgeItemService;

    private static FridgeItem item(Long id, String name, String quantity, String category) {
        FridgeItem item = new FridgeItem();
        item.setId(id);
        item.setName(name);
        item.setQuantity(quantity);
        item.setCategory(category);
        return item;
    }

    @Test
    void lInventaireEstRenvoyeTrieParCategorie() {
        when(fridgeItemRepository.findAllByOrderByCategoryAscNameAsc())
                .thenReturn(List.of(item(2L, "Riz", "500g", "Epicerie"), item(1L, "Poulet", "2", "Viande")));

        assertThat(fridgeItemService.findAll()).extracting(FridgeItemDto::name).containsExactly("Riz", "Poulet");
    }

    @Test
    void ajouterUnIngredientNettoieLeNom() {
        when(fridgeItemRepository.save(any(FridgeItem.class))).thenAnswer(inv -> inv.getArgument(0));

        FridgeItemDto dto = fridgeItemService.create(new FridgeItemCreateDto("  Tomate ", "4", "Legumes"));

        assertThat(dto.name()).isEqualTo("Tomate");
        assertThat(dto.quantity()).isEqualTo("4");
        assertThat(dto.category()).isEqualTo("Legumes");
    }

    @Test
    void modifierUnIngredientExistant() {
        when(fridgeItemRepository.findById(1L)).thenReturn(Optional.of(item(1L, "Poulet", "2", "Viande")));
        when(fridgeItemRepository.save(any(FridgeItem.class))).thenAnswer(inv -> inv.getArgument(0));

        FridgeItemDto dto = fridgeItemService.update(1L, new FridgeItemCreateDto("Poulet", "1", null));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.quantity()).isEqualTo("1");
        assertThat(dto.category()).isNull();
    }

    @Test
    void modifierUnIngredientInconnuEchoue() {
        when(fridgeItemRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fridgeItemService.update(9L, new FridgeItemCreateDto("X", null, null)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("9");
    }

    @Test
    void supprimerDelegueAuRepository() {
        fridgeItemService.delete(3L);

        verify(fridgeItemRepository).deleteById(3L);
    }
}
