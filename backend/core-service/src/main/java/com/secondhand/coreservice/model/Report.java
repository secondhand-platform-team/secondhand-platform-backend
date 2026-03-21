package com.secondhand.coreservice.model;

import com.secondhand.coreservice.model.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user báo cáo (từ auth-service)
    private String reporterId;

    // lý do báo cáo
    private String reason;

    private String description;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;
}