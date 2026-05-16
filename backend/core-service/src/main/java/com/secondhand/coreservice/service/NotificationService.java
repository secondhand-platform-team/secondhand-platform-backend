package com.secondhand.coreservice.service;

import com.secondhand.coreservice.dto.response.NotificationResponse;
import com.secondhand.coreservice.model.enums.NotificationType;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NotificationService {

    void createAndSendNotification(String userId, String content, NotificationType type, String itemId);

    Page<NotificationResponse> getNotificationsByUserId(int page, int size);

    void markAsRead(String notificationId);

    void markAllAsRead();

    long countUnreadNotifications();
}
