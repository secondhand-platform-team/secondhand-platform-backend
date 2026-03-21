package com.secondhand.coreservice.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemRequest {

    @NotBlank(message = "Item title is required")
    private String title;

    private String description;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    private String condition; // NEW, LIKE_NEW, GOOD, FAIR

    private String transactionType; // BUY, SELL, BOTH

    private String status; // AVAILABLE, RESERVED, SOLD, HIDDEN

    private LocationRequest location;

    private String userId;

    private List<ItemImageRequest> itemImageList;
}
