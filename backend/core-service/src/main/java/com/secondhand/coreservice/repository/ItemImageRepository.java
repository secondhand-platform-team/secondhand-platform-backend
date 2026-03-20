package com.secondhand.coreservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.secondhand.coreservice.model.ItemImage;

@Repository
public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {
}
