package com.secondhand.coreservice.model;

import com.secondhand.coreservice.model.enums.NotificationType;
import com.secondhand.coreservice.utils.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private String id;

    // user nhận thông báo (từ auth-service)
    private String userId;

    // nội dung thông báo
    private String content;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    // item liên quan (nếu có)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    private Boolean isRead = false;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = IdGenerator.generateId();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}