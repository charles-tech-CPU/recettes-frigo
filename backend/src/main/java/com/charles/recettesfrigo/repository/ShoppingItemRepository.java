package com.charles.recettesfrigo.repository;

import com.charles.recettesfrigo.domain.ShoppingItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {
    List<ShoppingItem> findAllByOrderByCheckedAscCreatedAtAscIdAsc();

    void deleteByCheckedTrue();
}
