package com.charles.recettesfrigo.repository;

import com.charles.recettesfrigo.domain.RecipeTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeTranslationRepository extends JpaRepository<RecipeTranslation, String> {}
