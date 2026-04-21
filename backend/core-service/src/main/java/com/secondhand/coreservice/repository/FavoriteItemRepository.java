package com.secondhand.coreservice.repository;

import com.secondhand.coreservice.model.FavoriteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, String> {
    boolean existsByUserIdAndItem_ItemId(String userId, String itemId);

    Optional<FavoriteItem> findByUserIdAndItem_ItemId(String userId, String itemId);

    List<FavoriteItem> findByUserId(String userId);
}
