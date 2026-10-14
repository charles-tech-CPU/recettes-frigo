package com.charles.recettesfrigo.service;

import com.charles.recettesfrigo.domain.RecipeFavorite;
import com.charles.recettesfrigo.dto.RecipeFavoriteDto;
import com.charles.recettesfrigo.repository.RecipeFavoriteRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/** Recettes favorites, TheMealDB ou perso, les plus recentes d'abord. */
@Service
public class RecipeFavoriteService {

    private final RecipeFavoriteRepository recipeFavoriteRepository;

    public RecipeFavoriteService(RecipeFavoriteRepository recipeFavoriteRepository) {
        this.recipeFavoriteRepository = recipeFavoriteRepository;
    }

    public List<RecipeFavoriteDto> findAll() {
        return recipeFavoriteRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(RecipeFavoriteService::toDto)
                .toList();
    }

    /** Ajoute la recette aux favoris ; deja en favori, son titre et sa photo sont mis a jour. */
    public RecipeFavoriteDto add(RecipeFavoriteDto dto) {
        RecipeKeys.check(dto.recipeKey());
        RecipeFavorite favorite = recipeFavoriteRepository
                .findById(dto.recipeKey())
                .orElseGet(() -> {
                    RecipeFavorite created = new RecipeFavorite();
                    created.setRecipeKey(dto.recipeKey());
                    created.setCreatedAt(OffsetDateTime.now());
                    return created;
                });
        favorite.setTitle(dto.title().trim());
        favorite.setImageUrl(
                dto.imageUrl() == null || dto.imageUrl().isBlank()
                        ? null
                        : dto.imageUrl().trim());
        return toDto(recipeFavoriteRepository.save(favorite));
    }

    public void remove(String recipeKey) {
        RecipeKeys.check(recipeKey);
        recipeFavoriteRepository.deleteById(recipeKey);
    }

    private static RecipeFavoriteDto toDto(RecipeFavorite favorite) {
        return new RecipeFavoriteDto(favorite.getRecipeKey(), favorite.getTitle(), favorite.getImageUrl());
    }
}
