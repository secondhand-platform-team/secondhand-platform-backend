package com.secondhand.chatservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Document(collection = "message_reads")
public class MessageRead {

    @Id
    private String id;

    private String messageId;

    private String userId;

    private LocalDateTime readAt;
}