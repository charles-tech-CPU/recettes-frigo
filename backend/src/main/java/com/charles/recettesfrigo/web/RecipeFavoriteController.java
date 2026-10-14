package com.charles.recettesfrigo.web;

import com.charles.recettesfrigo.dto.RecipeFavoriteDto;
import com.charles.recettesfrigo.service.RecipeFavoriteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
public class RecipeFavoriteController {

    private final RecipeFavoriteService recipeFavoriteService;

    public RecipeFavoriteController(RecipeFavoriteService recipeFavoriteService) {
        this.recipeFavoriteService = recipeFavoriteService;
    }

    @GetMapping
    public List<RecipeFavoriteDto> findAll() {
        return recipeFavoriteService.findAll();
    }

    @PostMapping
    public RecipeFavoriteDto add(@Valid @RequestBody RecipeFavoriteDto dto) {
        return recipeFavoriteService.add(dto);
    }

    /** recipe = "mealdb:<idMeal>" ou "perso:<id>". */
    @DeleteMapping
    public void remove(@RequestParam("recipe") String recipeKey) {
        recipeFavoriteService.remove(recipeKey);
    }
}
