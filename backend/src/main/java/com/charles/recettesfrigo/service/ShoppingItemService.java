package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.domain.ShoppingItem;
import com.charles.recettesfrigo.dto.ShoppingItemDto;
import com.charles.recettesfrigo.dto.ShoppingItemsAddDto;
import com.charles.recettesfrigo.repository.ShoppingItemRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Liste de courses : a acheter d'abord, puis ce qui est deja coche. */
@Service
public class ShoppingItemService {

    private final ShoppingItemRepository shoppingItemRepository;

    public ShoppingItemService(ShoppingItemRepository shoppingItemRepository) {
        this.shoppingItemRepository = shoppingItemRepository;
    }

    public List<ShoppingItemDto> findAll() {
        return shoppingItemRepository.findAllByOrderByCheckedAscCreatedAtAscIdAsc().stream()
                .map(ShoppingItemService::toDto)
                .toList();
    }

    /**
     * Ajoute les articles qui ne sont pas deja a acheter ("Tomates" = "tomate") ; renvoie
     * ceux reellement ajoutes. Un article deja coche est de nouveau ajoute : il faut en racheter.
     */
    @Transactional
    public List<ShoppingItemDto> addAll(ShoppingItemsAddDto dto) {
        Set<String> toBuy = new HashSet<>();
        for (ShoppingItem existing : shoppingItemRepository.findAllByOrderByCheckedAscCreatedAtAscIdAsc()) {
            if (!existing.isChecked()) {
                toBuy.add(FrenchIngredientMatcher.simplify(existing.getName()));
            }
        }
        OffsetDateTime now = OffsetDateTime.now();
        List<ShoppingItem> added = new ArrayList<>();
        for (ShoppingItemsAddDto.Item item : dto.items()) {
            if (!toBuy.add(FrenchIngredientMatcher.simplify(item.name()))) {
                continue;
            }
            ShoppingItem created = new ShoppingItem();
            created.setName(item.name().trim());
            created.setQuantity(blankToNull(item.quantity()));
            created.setRecipeTitle(blankToNull(item.recipeTitle()));
            created.setCreatedAt(now);
            added.add(created);
        }
        return shoppingItemRepository.saveAll(added).stream()
                .map(ShoppingItemService::toDto)
                .toList();
    }

    public ShoppingItemDto setChecked(Long id, boolean checked) {
        ShoppingItem item = shoppingItemRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Article introuvable: " + id));
        item.setChecked(checked);
        return toDto(shoppingItemRepository.save(item));
    }

    public void delete(Long id) {
        shoppingItemRepository.deleteById(id);
    }

    @Transactional
    public void deleteChecked() {
        shoppingItemRepository.deleteByCheckedTrue();
    }

    private static ShoppingItemDto toDto(ShoppingItem item) {
        return new ShoppingItemDto(
                item.getId(), item.getName(), item.getQuantity(), item.getRecipeTitle(), item.isChecked());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
