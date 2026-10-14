package com.charles.recettesfrigo.repository;

import com.charles.recettesfrigo.domain.RecipeFavorite;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeFavoriteRepository extends JpaRepository<RecipeFavorite, String> {
    List<RecipeFavorite> findAllByOrderByCreatedAtDesc();
}
