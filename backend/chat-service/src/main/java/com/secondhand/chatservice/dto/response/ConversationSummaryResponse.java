package com.secondhand.chatservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ConversationSummaryResponse {
    private String conversationId;
    private List<String> participants;
    private String participantUserId;
    private Boolean isOnline;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
