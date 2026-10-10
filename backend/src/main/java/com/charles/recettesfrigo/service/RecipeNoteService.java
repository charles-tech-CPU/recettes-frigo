package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.domain.RecipeNote;
import com.charles.recettesfrigo.dto.RecipeNoteCreateDto;
import com.charles.recettesfrigo.dto.RecipeNoteDto;
import com.charles.recettesfrigo.repository.RecipeNoteRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Notes libres sur une recette TheMealDB ("mealdb:<idMeal>") ou perso ("perso:<id>"). */
@Service
public class RecipeNoteService {

    private static final Pattern RECIPE_KEY = Pattern.compile("(mealdb|perso):\\d{1,18}");

    private final RecipeNoteRepository recipeNoteRepository;

    public RecipeNoteService(RecipeNoteRepository recipeNoteRepository) {
        this.recipeNoteRepository = recipeNoteRepository;
    }

    public List<RecipeNoteDto> findByRecipe(String recipeKey) {
        checkKey(recipeKey);
        return recipeNoteRepository.findByRecipeKeyOrderByCreatedAtAsc(recipeKey).stream()
                .map(RecipeNoteService::toDto)
                .toList();
    }

    public RecipeNoteDto create(RecipeNoteCreateDto dto) {
        checkKey(dto.recipeKey());
        RecipeNote note = new RecipeNote();
        note.setRecipeKey(dto.recipeKey());
        note.setContent(dto.content().trim());
        note.setCreatedAt(OffsetDateTime.now());
        return toDto(recipeNoteRepository.save(note));
    }

    public void delete(Long id) {
        recipeNoteRepository.deleteById(id);
    }

    private static void checkKey(String recipeKey) {
        if (recipeKey == null || !RECIPE_KEY.matcher(recipeKey).matches()) {
            throw new IllegalArgumentException("Recette inconnue: " + recipeKey);
        }
    }

    private static RecipeNoteDto toDto(RecipeNote note) {
        return new RecipeNoteDto(note.getId(), note.getRecipeKey(), note.getContent(), note.getCreatedAt());
    }
}
