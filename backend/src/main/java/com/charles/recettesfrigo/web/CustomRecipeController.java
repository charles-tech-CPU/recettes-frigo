package com.charles.recettesfrigo.web;

import com.charles.recettesfrigo.dto.CustomRecipeDto;
import com.charles.recettesfrigo.dto.RecipeImportRequestDto;
import com.charles.recettesfrigo.service.CustomRecipeService;
import com.charles.recettesfrigo.service.RecipeImportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/my-recipes")
public class CustomRecipeController {

    private final CustomRecipeService customRecipeService;
    private final RecipeImportService recipeImportService;

    public CustomRecipeController(CustomRecipeService customRecipeService, RecipeImportService recipeImportService) {
        this.customRecipeService = customRecipeService;
        this.recipeImportService = recipeImportService;
    }

    @GetMapping
    public List<CustomRecipeDto> findAll() {
        return customRecipeService.findAll();
    }

    @GetMapping("/{id}")
    public CustomRecipeDto findById(@PathVariable Long id) {
        return customRecipeService.findById(id);
    }

    @PostMapping
    public CustomRecipeDto create(@Valid @RequestBody CustomRecipeDto dto) {
        return customRecipeService.create(dto);
    }

    @PutMapping("/{id}")
    public CustomRecipeDto update(@PathVariable Long id, @Valid @RequestBody CustomRecipeDto dto) {
        return customRecipeService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        customRecipeService.delete(id);
    }

    /** Recette lue sur la page du lien, NON enregistree : sert a pre-remplir le formulaire. */
    @PostMapping("/import")
    public CustomRecipeDto importFromUrl(@Valid @RequestBody RecipeImportRequestDto request) {
        return recipeImportService.importFromUrl(request.url());
    }
}
