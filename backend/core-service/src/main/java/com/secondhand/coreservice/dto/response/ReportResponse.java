package com.secondhand.coreservice.dto.response;

import com.secondhand.coreservice.model.enums.ReportCode;
import com.secondhand.coreservice.model.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private String id;
    private String reporterId;
    private ReportCode code;
    private String reason;
    private String description;
    private ReportStatus status;
    private String itemId;
    private List<ReportImageResponse> reportImages;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String assignedStaffId;
    private String adminNote;
}
