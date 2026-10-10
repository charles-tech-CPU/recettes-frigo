package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.domain.CustomRecipe;
import com.charles.recettesfrigo.domain.CustomRecipeIngredient;
import com.charles.recettesfrigo.domain.CustomRecipeStep;
import com.charles.recettesfrigo.dto.CustomRecipeDto;
import com.charles.recettesfrigo.repository.CustomRecipeRepository;
import com.charles.recettesfrigo.repository.RecipeNoteRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomRecipeService {

    private final CustomRecipeRepository customRecipeRepository;
    private final RecipeNoteRepository recipeNoteRepository;

    public CustomRecipeService(
            CustomRecipeRepository customRecipeRepository, RecipeNoteRepository recipeNoteRepository) {
        this.customRecipeRepository = customRecipeRepository;
        this.recipeNoteRepository = recipeNoteRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomRecipeDto> findAll() {
        return customRecipeRepository.findAllByOrderByTitleAsc().stream()
                .map(CustomRecipeService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomRecipeDto findById(Long id) {
        return toDto(get(id));
    }

    @Transactional
    public CustomRecipeDto create(CustomRecipeDto dto) {
        CustomRecipe recipe = new CustomRecipe();
        recipe.setCreatedAt(OffsetDateTime.now());
        apply(recipe, dto);
        return toDto(customRecipeRepository.save(recipe));
    }

    @Transactional
    public CustomRecipeDto update(Long id, CustomRecipeDto dto) {
        CustomRecipe recipe = get(id);
        apply(recipe, dto);
        return toDto(customRecipeRepository.save(recipe));
    }

    /** Supprime aussi les notes de la recette (pas de cle etrangere sur recipe_note). */
    @Transactional
    public void delete(Long id) {
        customRecipeRepository.deleteById(id);
        recipeNoteRepository.deleteByRecipeKey("perso:" + id);
    }

    private CustomRecipe get(Long id) {
        return customRecipeRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recette introuvable: " + id));
    }

    private static void apply(CustomRecipe recipe, CustomRecipeDto dto) {
        recipe.setTitle(dto.title().trim());
        recipe.setCategory(blankToNull(dto.category()));
        recipe.setServings(dto.servings());
        recipe.setTotalTimeMinutes(dto.totalTimeMinutes());
        recipe.setThermomix(dto.thermomix());
        recipe.setImageUrl(blankToNull(dto.imageUrl()));
        recipe.setSourceUrl(blankToNull(dto.sourceUrl()));

        recipe.getIngredients().clear();
        List<CustomRecipeDto.Ingredient> ingredients = dto.ingredients() == null ? List.of() : dto.ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            CustomRecipeIngredient ingredient = new CustomRecipeIngredient();
            ingredient.setRecipe(recipe);
            ingredient.setPosition(i);
            ingredient.setName(ingredients.get(i).name().trim());
            ingredient.setQuantity(blankToNull(ingredients.get(i).quantity()));
            recipe.getIngredients().add(ingredient);
        }

        recipe.getSteps().clear();
        List<CustomRecipeDto.Step> steps = dto.steps() == null ? List.of() : dto.steps();
        for (int i = 0; i < steps.size(); i++) {
            CustomRecipeDto.Step source = steps.get(i);
            CustomRecipeStep step = new CustomRecipeStep();
            step.setRecipe(recipe);
            step.setPosition(i);
            step.setInstruction(source.instruction().trim());
            // Reglage Thermomix ignore pour une recette classique
            step.setTmTime(dto.thermomix() ? blankToNull(source.tmTime()) : null);
            step.setTmTemperature(dto.thermomix() ? blankToNull(source.tmTemperature()) : null);
            step.setTmSpeed(dto.thermomix() ? blankToNull(source.tmSpeed()) : null);
            step.setTmReverse(dto.thermomix() && source.tmReverse());
            recipe.getSteps().add(step);
        }
    }

    static CustomRecipeDto toDto(CustomRecipe recipe) {
        return new CustomRecipeDto(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getCategory(),
                recipe.getServings(),
                recipe.getTotalTimeMinutes(),
                recipe.isThermomix(),
                recipe.getImageUrl(),
                recipe.getSourceUrl(),
                recipe.getIngredients().stream()
                        .map(i -> new CustomRecipeDto.Ingredient(i.getName(), i.getQuantity()))
                        .toList(),
                recipe.getSteps().stream()
                        .map(s -> new CustomRecipeDto.Step(
                                s.getInstruction(),
                                s.getTmTime(),
                                s.getTmTemperature(),
                                s.getTmSpeed(),
                                s.isTmReverse()))
                        .toList());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
