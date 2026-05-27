package com.secondhand.coreservice.controller;

import com.secondhand.coreservice.dto.request.ReportRequest;
import com.secondhand.coreservice.dto.response.MessageResponse;
import com.secondhand.coreservice.dto.response.ReportResponse;
import com.secondhand.coreservice.exception.BadRequestException;
import com.secondhand.coreservice.model.enums.ReportStatus;
import com.secondhand.coreservice.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * Tạo báo cáo vi phạm bài viết (hỗ trợ upload ảnh)
     */
    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<ReportResponse> createReport(
            @RequestPart("report") String reportJson,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ReportRequest request = mapper.readValue(reportJson, ReportRequest.class);

            log.info("Creating report for item: {} with {} images",
                    request.getItemId(), images != null ? images.length : 0);
            ReportResponse response = reportService.createReport(request, images);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating report", e);
            throw new BadRequestException("Invalid report JSON: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin chi tiết báo cáo
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponse> getReport(@PathVariable String reportId) {
        log.info("Getting report: {}", reportId);
        ReportResponse response = reportService.getReportById(reportId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách báo cáo theo bài viết
     */
    @GetMapping("/item/{itemId}")
    public ResponseEntity<Page<ReportResponse>> getReportsByItem(
            @PathVariable String itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting reports for item: {}, page: {}, size: {}", itemId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> reports = reportService.getReportsByItemId(itemId, pageable);
        return ResponseEntity.ok(reports);
    }

    /**
     * Lấy danh sách báo cáo của người dùng
     */
    @GetMapping("/reporter/my-reports")
    public ResponseEntity<Page<ReportResponse>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting user's reports, page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> reports = reportService.getMyReports(pageable);
        return ResponseEntity.ok(reports);
    }

    /**
     * Lấy danh sách báo cáo theo trạng thái (chỉ dành cho admin)
     */
    @GetMapping("/admin/status/{status}")
    public ResponseEntity<Page<ReportResponse>> getReportsByStatus(
            @PathVariable ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting reports by status: {}, page: {}, size: {}", status, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> reports = reportService.getReportsByStatus(status, pageable);
        return ResponseEntity.ok(reports);
    }

    /**
     * Lấy danh sách báo cáo được gán cho nhân viên (chỉ dành cho admin)
     */
    @GetMapping("/admin/staff/{staffId}")
    public ResponseEntity<Page<ReportResponse>> getReportsByStaff(
            @PathVariable String staffId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting reports for staff: {}, page: {}, size: {}", staffId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> reports = reportService.getReportsByStaffId(staffId, pageable);
        return ResponseEntity.ok(reports);
    }

    /**
     * Gán báo cáo cho nhân viên xử lý (chỉ dành cho admin)
     */
    @PatchMapping("/admin/{reportId}/assign-staff")
    public ResponseEntity<ReportResponse> assignReportToStaff(
            @PathVariable String reportId,
            @RequestParam String staffId) {
        log.info("Assigning report {} to staff {}", reportId, staffId);
        ReportResponse response = reportService.assignReportToStaff(reportId, staffId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật trạng thái báo cáo (chỉ dành cho admin)
     */
    @PatchMapping("/admin/{reportId}/status")
    public ResponseEntity<ReportResponse> updateReportStatus(
            @PathVariable String reportId,
            @RequestParam ReportStatus status,
            @RequestParam(required = false) String adminNote) {
        log.info("Updating report {} status to {}", reportId, status);
        ReportResponse response = reportService.updateReportStatus(reportId, status, adminNote);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa báo cáo (chỉ dành cho admin)
     */
    @DeleteMapping("/admin/{reportId}")
    public ResponseEntity<MessageResponse> deleteReport(@PathVariable String reportId) {
        log.info("Deleting report: {}", reportId);
        reportService.deleteReport(reportId);
        return ResponseEntity.ok(new MessageResponse("Báo cáo đã được xóa thành công"));
    }

    /**
     * Lấy số báo cáo chưa xử lý (chỉ dành cho admin)
     */
    @GetMapping("/admin/stats/pending-count")
    public ResponseEntity<Long> getPendingReportCount() {
        log.info("Getting pending reports count");
        long count = reportService.countPendingReports();
        return ResponseEntity.ok(count);
    }
}
