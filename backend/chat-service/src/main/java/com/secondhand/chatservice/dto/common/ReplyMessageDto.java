package com.secondhand.chatservice.dto.common;

import com.secondhand.chatservice.model.enums.MessageType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReplyMessageDto {
    private String messageId;
    private String senderId;
    private String senderName;
    private String content;
    private MessageType messageType;
    private Boolean isDeleted;
}