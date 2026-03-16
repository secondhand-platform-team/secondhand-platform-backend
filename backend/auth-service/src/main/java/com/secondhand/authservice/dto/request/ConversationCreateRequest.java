package com.secondhand.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationCreateRequest {

    @NotBlank(message = "userId is required")
    private String userId;
}
