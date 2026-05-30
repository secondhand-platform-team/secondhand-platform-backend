package com.secondhand.coreservice.controller;

import com.secondhand.coreservice.exception.BadRequestException;
import com.secondhand.coreservice.exception.ResourceNotFoundException;
import com.secondhand.coreservice.exception.UnauthorizedException;
import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.model.Review;
import com.secondhand.coreservice.model.enums.ItemStatus;
import com.secondhand.coreservice.model.enums.NotificationType;
import com.secondhand.coreservice.repository.ItemRepository;
import com.secondhand.coreservice.repository.ReviewRepository;
import com.secondhand.coreservice.security.JwtAuthenticatedUser;
import com.secondhand.coreservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ItemRepository itemRepository;
    private final NotificationService notificationService;

    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticatedUser) {
            JwtAuthenticatedUser user = (JwtAuthenticatedUser) authentication.getPrincipal();
            return user.userId();
        }
        throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện");
    }

    /**
     * POST /api/items/{itemId}/reviews
     * Người mua đánh giá người bán sau khi hoàn tất giao dịch
     */
    @PostMapping("/items/{itemId}/reviews")
    public ResponseEntity<?> createReview(
            @PathVariable String itemId,
            @RequestBody Map<String, Object> body) {
        
        String currentUserId = getCurrentUserId();
        
        Item item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với ID: " + itemId));

        // 1. Kiểm tra trạng thái tin đăng phải là SOLD
        if (item.getStatus() != ItemStatus.SOLD) {
            throw new BadRequestException("Chỉ có thể đánh giá sau khi giao dịch thành công (sản phẩm đã bán)");
        }

        // 2. Không được tự đánh giá chính mình
        if (item.getUserId().equals(currentUserId)) {
            throw new BadRequestException("Bạn không thể tự đánh giá chính mình");
        }

        // 3. Mỗi sản phẩm chỉ được đánh giá 1 lần duy nhất
        if (reviewRepository.existsByItemItemId(itemId)) {
            throw new BadRequestException("Sản phẩm này đã được đánh giá trước đó");
        }

        // Parse rating và comment
        Integer rating = body.get("rating") != null ? ((Number) body.get("rating")).intValue() : null;
        if (rating == null || rating < 1 || rating > 5) {
            throw new BadRequestException("Số sao đánh giá phải từ 1 đến 5");
        }
        
        String comment = (String) body.get("comment");

        // Tạo review
        Review review = new Review();
        review.setItem(item);
        review.setReivewerId(currentUserId);
        review.setRating(rating);
        review.setCommentContent(comment);
        
        Review saved = reviewRepository.save(review);
        log.info("Successfully created review {} for itemId={} by buyerId={}", saved.getReviewId(), itemId, currentUserId);

        // 4. Gửi thông báo đến người bán (Seller)
        try {
            String notifyContent = "Sản phẩm \"" + item.getTitle() + "\" của bạn đã nhận được đánh giá " + rating + " sao từ người mua.";
            notificationService.createAndSendNotification(
                    item.getUserId(),
                    notifyContent,
                    NotificationType.SYSTEM,
                    itemId
            );
        } catch (Exception ex) {
            log.error("Failed to send review notification to seller: {}", ex.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đánh giá người bán thành công",
                "reviewId", saved.getReviewId()
        ));
    }

    /**
     * GET /api/users/{userId}/reviews
     * Lấy danh sách đánh giá & điểm trung bình sao của người bán
     */
    @GetMapping("/users/{userId}/reviews")
    public ResponseEntity<?> getSellerReviews(@PathVariable String userId) {
        List<Review> reviews = reviewRepository.findByItemUserIdOrderByCreatedAtDesc(userId);
        Double averageRating = reviewRepository.getAverageRatingBySellerId(userId);

        List<Map<String, Object>> reviewList = reviews.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getReviewId());
            map.put("reviewId", r.getReviewId());
            map.put("buyerId", r.getReivewerId());
            map.put("reviewerId", r.getReivewerId());
            map.put("rating", r.getRating());
            map.put("comment", r.getCommentContent());
            map.put("createdAt", r.getCreatedAt());
            
            // Map thông tin item tối giản
            if (r.getItem() != null) {
                map.put("itemId", r.getItem().getItemId());
                map.put("itemTitle", r.getItem().getTitle());
            }
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
        result.put("totalReviews", reviews.size());
        result.put("reviews", reviewList);

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/reviews/reviewed-items
     * Lấy danh sách itemId mà người dùng hiện tại đã đánh giá
     */
    @GetMapping("/reviews/reviewed-items")
    public ResponseEntity<?> getMyReviewedItems() {
        String currentUserId = getCurrentUserId();
        List<Review> myReviews = reviewRepository.findByReivewerId(currentUserId);
        
        List<String> reviewedItemIds = myReviews.stream()
                .filter(r -> r.getItem() != null)
                .map(r -> r.getItem().getItemId())
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(reviewedItemIds);
    }
}
