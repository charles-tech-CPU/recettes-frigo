package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.domain.FridgeItem;
import com.charles.recettesfrigo.dto.FridgeItemCreateDto;
import com.charles.recettesfrigo.dto.FridgeItemDto;
import com.charles.recettesfrigo.repository.FridgeItemRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FridgeItemService {

    private final FridgeItemRepository fridgeItemRepository;

    public FridgeItemService(FridgeItemRepository fridgeItemRepository) {
        this.fridgeItemRepository = fridgeItemRepository;
    }

    public List<FridgeItemDto> findAll() {
        return fridgeItemRepository.findAllByOrderByCategoryAscNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public FridgeItemDto create(FridgeItemCreateDto dto) {
        checkNotInFridge(dto.name(), null);
        FridgeItem item = new FridgeItem();
        apply(item, dto);
        return toDto(fridgeItemRepository.save(item));
    }

    public FridgeItemDto update(Long id, FridgeItemCreateDto dto) {
        FridgeItem item = fridgeItemRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ingredient introuvable: " + id));
        checkNotInFridge(dto.name(), id);
        apply(item, dto);
        return toDto(fridgeItemRepository.save(item));
    }

    public void delete(Long id) {
        fridgeItemRepository.deleteById(id);
    }

    /** Le nom est unique en base : message clair plutot qu'une erreur 500 sur la contrainte. */
    private void checkNotInFridge(String name, Long exceptId) {
        fridgeItemRepository
                .findByNameIgnoreCase(name.trim())
                .filter(other -> !other.getId().equals(exceptId))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("« " + other.getName() + " » est déjà dans le frigo.");
                });
    }

    private void apply(FridgeItem item, FridgeItemCreateDto dto) {
        item.setName(dto.name().trim());
        item.setQuantity(dto.quantity());
        item.setCategory(dto.category());
    }

    private FridgeItemDto toDto(FridgeItem item) {
        return new FridgeItemDto(item.getId(), item.getName(), item.getQuantity(), item.getCategory());
    }
}
