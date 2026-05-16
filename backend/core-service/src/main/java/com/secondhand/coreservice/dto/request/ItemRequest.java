package com.secondhand.coreservice.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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

    private String condition;

    private String transactionType;

    private String status;

    private LocationRequest location;

    private List<ItemImageRequest> itemImageList;

    private List<ItemAttributeRequest> attributes;

    // "WALLET" or "VNPAY"
    private String paymentMethod;
    
    // Fee for posting the item
    private BigDecimal postingFee;

    // Payment verification fields
    private String transactionId;

    private String orderId;

    private String responseCode;

    private String secureHash;
}
