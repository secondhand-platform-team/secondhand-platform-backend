package com.secondhand.coreservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.secondhand.coreservice.model.CategoryAttribute;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, String> {
    /**
     * Tìm tất cả attributes của một category
     */
    List<CategoryAttribute> findByCategoryCategoryId(String categoryId);

    /**
     * Tìm attribute theo category và code
     */
    Optional<CategoryAttribute> findByCategory_CategoryIdAndCode(String categoryId, String code);

    /**
     * Kiểm tra attribute đã tồn tại theo category và code
     */
    boolean existsByCategory_CategoryIdAndCode(String categoryId, String code);
}
