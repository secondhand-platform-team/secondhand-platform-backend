package com.secondhand.coreservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.secondhand.coreservice.model.enums.ReportCode;
import com.secondhand.coreservice.model.enums.ReportStatus;
import com.secondhand.coreservice.utils.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "reports")
@Builder
public class Report {

    @Id
    private String id;

    // user báo cáo (từ auth-service)
    @Column(nullable = false)
    private String reporterId;

    // mã báo cáo
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportCode code;

    // lý do báo cáo
    @Column(nullable = false)
    private String reason;

    // mô tả chi tiết
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    @JsonIgnore
    private Item item;

    // ảnh báo cáo (tối đa 2 ảnh)
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ReportImage> reportImages;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    // nhân viên/admin xử lý báo cáo
    private String assignedStaffId;

    // ghi chú của admin khi xử lý báo cáo
    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = IdGenerator.generateId();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ReportStatus.PENDING;
        }
    }
}