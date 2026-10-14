package com.charles.recettesfrigo.web;

import com.charles.recettesfrigo.dto.ShoppingItemCheckDto;
import com.charles.recettesfrigo.dto.ShoppingItemDto;
import com.charles.recettesfrigo.dto.ShoppingItemsAddDto;
import com.charles.recettesfrigo.service.ShoppingItemService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shopping-items")
public class ShoppingItemController {

    private final ShoppingItemService shoppingItemService;

    public ShoppingItemController(ShoppingItemService shoppingItemService) {
        this.shoppingItemService = shoppingItemService;
    }

    @GetMapping
    public List<ShoppingItemDto> findAll() {
        return shoppingItemService.findAll();
    }

    /** Renvoie les articles reellement ajoutes (ceux deja a acheter sont ignores). */
    @PostMapping
    public List<ShoppingItemDto> addAll(@Valid @RequestBody ShoppingItemsAddDto dto) {
        return shoppingItemService.addAll(dto);
    }

    @PatchMapping("/{id}")
    public ShoppingItemDto setChecked(@PathVariable Long id, @RequestBody ShoppingItemCheckDto dto) {
        return shoppingItemService.setChecked(id, dto.checked());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        shoppingItemService.delete(id);
    }

    @DeleteMapping("/checked")
    public void deleteChecked() {
        shoppingItemService.deleteChecked();
    }
}
