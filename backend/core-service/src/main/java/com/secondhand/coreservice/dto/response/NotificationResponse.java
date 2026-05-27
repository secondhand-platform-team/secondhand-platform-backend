package com.secondhand.coreservice.dto.response;

import com.secondhand.coreservice.model.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String userId;
    private String content;
    private NotificationType type;
    private String itemId; // optional
    private Boolean isRead;
    private LocalDateTime createdAt;
}
