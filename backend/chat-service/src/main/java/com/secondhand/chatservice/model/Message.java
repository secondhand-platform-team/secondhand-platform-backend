package com.secondhand.chatservice.model;

import com.secondhand.chatservice.model.enums.MessageStatus;
import com.secondhand.chatservice.model.enums.MessageType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Document(collection = "messages")
public class Message {

    @Id
    private String id;

    private String conversationId;

    private String senderId;

    private String receiverId;

    private MessageType type;

    private String content;

    private String replyToMessageId;

    private List<MessageReaction> reactions;

    private MessageStatus status;

    private LocalDateTime createdAt;
}