package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.domain.RecipeNote;
import com.charles.recettesfrigo.dto.RecipeNoteCreateDto;
import com.charles.recettesfrigo.dto.RecipeNoteDto;
import com.charles.recettesfrigo.repository.RecipeNoteRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/** Notes libres sur une recette TheMealDB ("mealdb:<idMeal>") ou perso ("perso:<id>"). */
@Service
public class RecipeNoteService {

    private final RecipeNoteRepository recipeNoteRepository;

    public RecipeNoteService(RecipeNoteRepository recipeNoteRepository) {
        this.recipeNoteRepository = recipeNoteRepository;
    }

    public List<RecipeNoteDto> findByRecipe(String recipeKey) {
        RecipeKeys.check(recipeKey);
        return recipeNoteRepository.findByRecipeKeyOrderByCreatedAtAsc(recipeKey).stream()
                .map(RecipeNoteService::toDto)
                .toList();
    }

    public RecipeNoteDto create(RecipeNoteCreateDto dto) {
        RecipeKeys.check(dto.recipeKey());
        RecipeNote note = new RecipeNote();
        note.setRecipeKey(dto.recipeKey());
        note.setContent(dto.content().trim());
        note.setCreatedAt(OffsetDateTime.now());
        return toDto(recipeNoteRepository.save(note));
    }

    public void delete(Long id) {
        recipeNoteRepository.deleteById(id);
    }

    private static RecipeNoteDto toDto(RecipeNote note) {
        return new RecipeNoteDto(note.getId(), note.getRecipeKey(), note.getContent(), note.getCreatedAt());
    }
}
