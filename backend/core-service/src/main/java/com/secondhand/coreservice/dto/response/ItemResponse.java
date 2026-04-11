package com.secondhand.coreservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemResponse {
    private String itemId;
    private String title;
    private String description;
    private String categoryId;
    private BigDecimal price;
    private String condition;
    private String transactionType;
    private String status;
    private LocationResponse location;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ItemImageResponse> itemImageList;
    private List<ItemAttributeResponse> attributes;
    private String transactionId;
    private String paymentUrl;
}
