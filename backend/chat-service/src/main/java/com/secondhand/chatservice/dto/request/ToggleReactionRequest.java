package com.secondhand.chatservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ToggleReactionRequest {
    private String emoji;
}