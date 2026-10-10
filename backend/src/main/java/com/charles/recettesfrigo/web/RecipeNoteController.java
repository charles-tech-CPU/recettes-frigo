package com.charles.recettesfrigo.web;

import com.charles.recettesfrigo.dto.RecipeNoteCreateDto;
import com.charles.recettesfrigo.dto.RecipeNoteDto;
import com.charles.recettesfrigo.service.RecipeNoteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipe-notes")
public class RecipeNoteController {

    private final RecipeNoteService recipeNoteService;

    public RecipeNoteController(RecipeNoteService recipeNoteService) {
        this.recipeNoteService = recipeNoteService;
    }

    /** Notes d'une recette : recipe = "mealdb:<idMeal>" ou "perso:<id>". */
    @GetMapping
    public List<RecipeNoteDto> findByRecipe(@RequestParam("recipe") String recipeKey) {
        return recipeNoteService.findByRecipe(recipeKey);
    }

    @PostMapping
    public RecipeNoteDto create(@Valid @RequestBody RecipeNoteCreateDto dto) {
        return recipeNoteService.create(dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        recipeNoteService.delete(id);
    }
}
