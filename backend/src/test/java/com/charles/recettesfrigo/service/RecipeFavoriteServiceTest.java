package com.charles.recettesfrigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.recettesfrigo.domain.RecipeFavorite;
import com.charles.recettesfrigo.dto.RecipeFavoriteDto;
import com.charles.recettesfrigo.repository.RecipeFavoriteRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecipeFavoriteServiceTest {

    @Mock
    private RecipeFavoriteRepository recipeFavoriteRepository;

    @InjectMocks
    private RecipeFavoriteService service;

    @Test
    void ajouterUnFavori() {
        when(recipeFavoriteRepository.save(any(RecipeFavorite.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeFavoriteDto favorite = service.add(new RecipeFavoriteDto("mealdb:52772", " Poulet teriyaki ", " "));

        assertThat(favorite.recipeKey()).isEqualTo("mealdb:52772");
        assertThat(favorite.title()).isEqualTo("Poulet teriyaki");
        assertThat(favorite.imageUrl()).isNull();
    }

    @Test
    void ajouterUnFavoriDejaPresentMetAJourSonTitre() {
        RecipeFavorite existing = new RecipeFavorite();
        existing.setRecipeKey("perso:3");
        existing.setTitle("Ancien titre");
        existing.setCreatedAt(OffsetDateTime.now().minusDays(2));
        when(recipeFavoriteRepository.findById("perso:3")).thenReturn(Optional.of(existing));
        when(recipeFavoriteRepository.save(any(RecipeFavorite.class))).thenAnswer(inv -> inv.getArgument(0));

        service.add(new RecipeFavoriteDto("perso:3", "Soupe", "https://img/soupe.jpg"));

        assertThat(existing.getTitle()).isEqualTo("Soupe");
        assertThat(existing.getImageUrl()).isEqualTo("https://img/soupe.jpg");
    }

    @Test
    void lesFavorisSontRenvoyes() {
        RecipeFavorite favorite = new RecipeFavorite();
        favorite.setRecipeKey("perso:3");
        favorite.setTitle("Soupe");
        when(recipeFavoriteRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(favorite));

        assertThat(service.findAll()).extracting(RecipeFavoriteDto::title).containsExactly("Soupe");
    }

    @Test
    void retirerUnFavori() {
        service.remove("mealdb:1");

        verify(recipeFavoriteRepository).deleteById("mealdb:1");
    }

    @Test
    void uneCleDeRecetteInvalideEstRefusee() {
        assertThatThrownBy(() -> service.add(new RecipeFavoriteDto("autre:1", "X", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.remove("mealdb:abc")).isInstanceOf(IllegalArgumentException.class);
    }
}
