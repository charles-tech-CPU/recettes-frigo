package com.charles.recettesfrigo.web;

import com.charles.recettesfrigo.dto.FridgeItemCreateDto;
import com.charles.recettesfrigo.dto.FridgeItemDto;
import com.charles.recettesfrigo.service.FridgeItemService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fridge-items")
public class FridgeItemController {

    private final FridgeItemService fridgeItemService;

    public FridgeItemController(FridgeItemService fridgeItemService) {
        this.fridgeItemService = fridgeItemService;
    }

    @GetMapping
    public List<FridgeItemDto> findAll() {
        return fridgeItemService.findAll();
    }

    @PostMapping
    public FridgeItemDto create(@Valid @RequestBody FridgeItemCreateDto dto) {
        return fridgeItemService.create(dto);
    }

    @PutMapping("/{id}")
    public FridgeItemDto update(@PathVariable Long id, @Valid @RequestBody FridgeItemCreateDto dto) {
        return fridgeItemService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        fridgeItemService.delete(id);
    }
}
