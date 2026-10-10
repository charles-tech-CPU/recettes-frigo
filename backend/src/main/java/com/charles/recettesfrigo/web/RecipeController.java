package com.charles.recettesfrigo.web;

import com.charles.recettesfrigo.dto.RecipeDetailDto;
import com.charles.recettesfrigo.dto.RecipeSuggestionDto;
import com.charles.recettesfrigo.service.RecipeSuggestionService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeSuggestionService recipeSuggestionService;

    public RecipeController(RecipeSuggestionService recipeSuggestionService) {
        this.recipeSuggestionService = recipeSuggestionService;
    }

    @GetMapping("/suggestions")
    public List<RecipeSuggestionDto> suggestions(@RequestParam(defaultValue = "12") int limit) {
        return recipeSuggestionService.suggestFromFridge(limit);
    }

    @GetMapping("/{mealId}")
    public RecipeDetailDto detail(@PathVariable String mealId) {
        return recipeSuggestionService.findDetail(mealId);
    }
}
