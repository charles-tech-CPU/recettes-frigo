package com.charles.recettesfrigo.repository;

import com.charles.recettesfrigo.domain.CustomRecipe;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomRecipeRepository extends JpaRepository<CustomRecipe, Long> {
    List<CustomRecipe> findAllByOrderByTitleAsc();
}
