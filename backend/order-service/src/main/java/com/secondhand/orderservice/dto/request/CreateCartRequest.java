package com.secondhand.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCartRequest {

    @NotBlank(message = "userId is required")
    private String userId;
}