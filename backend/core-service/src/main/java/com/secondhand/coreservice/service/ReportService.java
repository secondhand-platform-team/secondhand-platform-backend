package com.secondhand.coreservice.service;

import com.secondhand.coreservice.dto.request.ReportRequest;
import com.secondhand.coreservice.dto.response.ReportResponse;
import com.secondhand.coreservice.model.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

public interface ReportService {
    ReportResponse createReport(ReportRequest request);

    ReportResponse createReport(ReportRequest request, MultipartFile[] images);

    ReportResponse getReportById(String reportId);

    // <ReportResponse> getReportsByItemId(String itemId, Pageable pageable);

    @Transactional(readOnly = true)
    Page<ReportResponse> getReportsByItemId(String itemId, Pageable pageable);

    Page<ReportResponse> getReportsByReporterId(String reporterId, Pageable pageable);

    Page<ReportResponse> getReportsByStatus(ReportStatus status, Pageable pageable);

    Page<ReportResponse> getReportsByStaffId(String staffId, Pageable pageable);

    Page<ReportResponse> getMyReports(Pageable pageable);

    ReportResponse updateReportStatus(String reportId, ReportStatus status, String adminNote);

    ReportResponse assignReportToStaff(String reportId, String staffId);

    void deleteReport(String reportId);

    long countPendingReports();
}
