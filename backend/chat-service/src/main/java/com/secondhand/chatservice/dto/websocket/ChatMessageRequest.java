package com.secondhand.chatservice.dto.websocket;

import com.secondhand.chatservice.model.enums.MessageType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequest {
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String content;
    private MessageType type;
    private String replyToMessageId;
}
