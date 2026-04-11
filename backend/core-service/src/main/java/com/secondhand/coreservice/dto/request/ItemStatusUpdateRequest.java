package com.secondhand.coreservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;
}
