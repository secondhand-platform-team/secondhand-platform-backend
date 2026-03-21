package com.secondhand.chatservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Document(collection = "conversations")
public class Conversation {

    @Id
    private String id;

    // 2 user chat với nhau
    private List<String> participants;

    @Indexed(unique = true)
    private String conversationKey;

    private String lastMessageId;

    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}