package com.secondhand.chatservice.dto.common;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageReactionDto {
    private String emoji;
    private String userId;
    private String userName;
}