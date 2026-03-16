package com.secondhand.chatservice.dto.websocket;

import com.secondhand.chatservice.model.enums.MessageStatus;
import com.secondhand.chatservice.model.enums.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    private String messageId;
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private LocalDateTime createdAt;
}
