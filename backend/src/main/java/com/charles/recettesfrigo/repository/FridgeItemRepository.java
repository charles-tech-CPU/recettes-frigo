package com.charles.recettesfrigo.repository;

import com.charles.recettesfrigo.domain.FridgeItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FridgeItemRepository extends JpaRepository<FridgeItem, Long> {
    List<FridgeItem> findAllByOrderByCategoryAscNameAsc();
}
