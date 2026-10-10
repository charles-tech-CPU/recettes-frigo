package com.charles.recettesfrigo.repository;

import com.charles.recettesfrigo.domain.RecipeNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeNoteRepository extends JpaRepository<RecipeNote, Long> {
    List<RecipeNote> findByRecipeKeyOrderByCreatedAtAsc(String recipeKey);

    void deleteByRecipeKey(String recipeKey);
}
