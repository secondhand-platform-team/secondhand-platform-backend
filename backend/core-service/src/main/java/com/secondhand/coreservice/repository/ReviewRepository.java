package com.secondhand.coreservice.repository;

import com.secondhand.coreservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    
    // Tìm tất cả reviews các item của một người bán, sắp xếp mới nhất
    List<Review> findByItemUserIdOrderByCreatedAtDesc(String sellerId);
    
    // Kiểm tra xem item đã được đánh giá chưa
    boolean existsByItemItemId(String itemId);
    
    // Tìm tất cả reviews do một người mua gửi
    List<Review> findByReivewerId(String reviewerId);

    // Tính số sao trung bình của một người bán
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.item.userId = :sellerId")
    Double getAverageRatingBySellerId(@Param("sellerId") String sellerId);
}
