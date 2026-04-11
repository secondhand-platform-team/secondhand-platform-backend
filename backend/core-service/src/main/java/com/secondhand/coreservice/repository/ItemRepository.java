package com.secondhand.coreservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.model.enums.ItemStatus;

@Repository
public interface ItemRepository extends JpaRepository<Item, String> {
    List<Item> findByCategory_CategoryId(String categoryId);

    List<Item> findByUserId(String userId);

    Optional<Item> findByItemId(String itemId);

    List<Item> findAllByStatus(ItemStatus status);
}
