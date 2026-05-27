package com.secondhand.coreservice.service.impl;

import com.secondhand.coreservice.dto.response.NotificationResponse;
import com.secondhand.coreservice.exception.BadRequestException;
import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.model.Notification;
import com.secondhand.coreservice.model.enums.NotificationType;
import com.secondhand.coreservice.repository.ItemRepository;
import com.secondhand.coreservice.repository.NotificationRepository;
import com.secondhand.coreservice.security.JwtAuthenticatedUser;
import com.secondhand.coreservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ItemRepository itemRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticatedUser) {
            JwtAuthenticatedUser user = (JwtAuthenticatedUser) authentication.getPrincipal();
            return user.userId();
        }
        throw new RuntimeException("Unauthorized");
    }

    @Override
    @Transactional
    public void createAndSendNotification(String userId, String content, NotificationType type, String itemId) {
        log.info("Creating notification for user: {}, type: {}", userId, type);
        
        Notification notification = new Notification();
        // ID is generated in PrePersist
        notification.setUserId(userId);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(false);
        
        if (itemId != null) {
            Item item = itemRepository.findById(itemId).orElse(null);
            notification.setItem(item);
        }
        
        Notification savedNotification = notificationRepository.save(notification);
        
        NotificationResponse response = mapToResponse(savedNotification);
        
        // Send real-time notification
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, response);
    }

    @Override
    public Page<NotificationResponse> getNotificationsByUserId(int page, int size) {
        String userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void markAsRead(String notificationId) {
        String userId = getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Notification not found"));
                
        if (!notification.getUserId().equals(userId)) {
            throw new BadRequestException("You do not have permission to access this notification");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        String userId = getCurrentUserId();
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
        
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    public long countUnreadNotifications() {
        String userId = getCurrentUserId();
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .content(notification.getContent())
                .type(notification.getType())
                .itemId(notification.getItem() != null ? notification.getItem().getItemId() : null)
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteNotification(String notificationId) {
        String userId = getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Notification not found"));
                
        if (!notification.getUserId().equals(userId)) {
            throw new BadRequestException("You do not have permission to delete this notification");
        }
        
        notificationRepository.delete(notification);
    }
}
