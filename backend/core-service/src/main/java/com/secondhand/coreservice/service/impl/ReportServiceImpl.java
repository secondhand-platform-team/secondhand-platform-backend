package com.secondhand.coreservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondhand.coreservice.dto.request.ReportRequest;
import com.secondhand.coreservice.dto.response.ReportResponse;
import com.secondhand.coreservice.exception.BadRequestException;
import com.secondhand.coreservice.exception.ResourceNotFoundException;
import com.secondhand.coreservice.model.Report;
import com.secondhand.coreservice.model.ReportImage;
import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.model.enums.ReportStatus;
import com.secondhand.coreservice.repository.ReportRepository;
import com.secondhand.coreservice.repository.ReportImageRepository;
import com.secondhand.coreservice.repository.ItemRepository;
import com.secondhand.coreservice.security.JwtAuthenticatedUser;
import com.secondhand.coreservice.service.ReportService;
import com.secondhand.coreservice.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReportImageRepository reportImageRepository;
    private final ItemRepository itemRepository;
    private final CloudinaryService cloudinaryService;
    private final ObjectMapper objectMapper;
    private final com.secondhand.coreservice.service.NotificationService notificationService;

    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticatedUser) {
            JwtAuthenticatedUser user = (JwtAuthenticatedUser) authentication.getPrincipal();
            return user.userId();
        }
        throw new RuntimeException("Unauthorized");
    }

    @Override
    public ReportResponse createReport(ReportRequest request) {
        return createReport(request, null);
    }

    @Override
    @Transactional
    public ReportResponse createReport(ReportRequest request, MultipartFile[] images) {
        String reporterId = getCurrentUserId();
        log.info("Creating report for item: {} by user: {} with {} images",
                request.getItemId(), reporterId, images != null ? images.length : 0);

        // Kiểm tra item có tồn tại không
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item không tìm thấy"));

        // Tạo report
        Report report = Report.builder()
                .reporterId(reporterId)
                .code(request.getCode())
                .reason(request.getReason())
                .description(request.getDescription())
                .item(item)
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .reportImages(new ArrayList<>())
                .build();

        report = reportRepository.save(report);
        log.info("Report created with id: {}", report.getId());

        // Xử lý upload ảnh báo cáo (tối đa 2 ảnh)
        if (images != null && images.length > 0) {
            List<ReportImage> reportImages = processAndUploadReportImages(report, images);
            report.setReportImages(reportImages);
        }

        // Send notification to reporter
        notificationService.createAndSendNotification(
                reporterId,
                "Bạn đã gửi báo cáo thành công cho bài đăng \"" + item.getTitle() + "\". Chúng tôi sẽ xem xét và phản hồi sớm nhất có thể.",
                com.secondhand.coreservice.model.enums.NotificationType.SYSTEM,
                item.getItemId());

        return mapToReportResponse(report);
    }

    /**
     * Xử lý và upload ảnh báo cáo lên Cloudinary (tối đa 2 ảnh)
     */
    private List<ReportImage> processAndUploadReportImages(Report report, MultipartFile[] images) {
        List<ReportImage> reportImages = new ArrayList<>();

        int imageCount = 0;
        for (int i = 0; i < images.length && imageCount < 2; i++) {
            MultipartFile file = images[i];

            // Validate file không rỗng
            if (file.isEmpty()) {
                log.warn("Skipping empty file at index {}", i);
                continue;
            }

            // Validate content type
            if (!isValidImageFile(file)) {
                throw new BadRequestException(
                        "Invalid image file at index " + i + ". Allowed types: jpg, jpeg, png, gif, webp");
            }

            // Validate file size (10MB limit)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new BadRequestException("Image file at index " + i + " exceeds 10MB limit");
            }

            try {
                String imageUrl = cloudinaryService.uploadImage(file);
                ReportImage reportImage = ReportImage.builder()
                        .report(report)
                        .imageUrl(imageUrl)
                        .build();
                reportImages.add(reportImage);
                imageCount++;
                log.debug("Successfully uploaded report image {} to: {}", i, imageUrl);
            } catch (IOException e) {
                log.error("Failed to upload image at index {}", i, e);
                throw new BadRequestException("Failed to upload image: " + e.getMessage());
            }
        }

        if (!reportImages.isEmpty()) {
            reportImages = reportImageRepository.saveAll(reportImages);
            log.info("Successfully saved {} report images for report {}", reportImages.size(), report.getId());
        }

        return reportImages;
    }

    /**
     * Validate image file type
     */
    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/") &&
                (contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/gif") ||
                        contentType.equals("image/webp"));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportById(String reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report không tìm thấy"));

        // Lazy load images
        report.getReportImages().size();

        return mapToReportResponse(report);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ReportResponse> getReportsByItemId(String itemId, Pageable pageable) {
        return reportRepository.findByItemId(itemId, pageable)
                .map(this::mapToReportResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsByReporterId(String reporterId, Pageable pageable) {
        return reportRepository.findByReporterId(reporterId, pageable)
                .map(this::mapToReportResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsByStatus(ReportStatus status, Pageable pageable) {
        return reportRepository.findByStatus(status, pageable)
                .map(this::mapToReportResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsByStaffId(String staffId, Pageable pageable) {
        return reportRepository.findByAssignedStaffId(staffId, pageable)
                .map(this::mapToReportResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getMyReports(Pageable pageable) {
        String userId = getCurrentUserId();
        return reportRepository.findByReporterId(userId, pageable)
                .map(this::mapToReportResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingReports() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Override
    public ReportResponse updateReportStatus(String reportId, ReportStatus status, String adminNote) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report không tìm thấy"));

        report.setStatus(status);
        report.setAdminNote(adminNote);

        if (status == ReportStatus.RESOLVED || status == ReportStatus.REJECTED) {
            report.setResolvedAt(LocalDateTime.now());
        }

        report = reportRepository.save(report);
        log.info("Report {} status updated to {}", reportId, status);

        return mapToReportResponse(report);
    }

    @Override
    public ReportResponse assignReportToStaff(String reportId, String staffId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report không tìm thấy"));

        report.setAssignedStaffId(staffId);
        report = reportRepository.save(report);
        log.info("Report {} assigned to staff {}", reportId, staffId);

        return mapToReportResponse(report);
    }

    @Override
    public void deleteReport(String reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report không tìm thấy"));

        reportRepository.delete(report);
        log.info("Report {} deleted", reportId);
    }

    private ReportResponse mapToReportResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporterId())
                .code(report.getCode())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .itemId(report.getItem().getItemId())
                .reportImages(report.getReportImages().stream()
                        .map(image -> objectMapper.convertValue(image,
                                com.secondhand.coreservice.dto.response.ReportImageResponse.class))
                        .collect(Collectors.toList()))
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .assignedStaffId(report.getAssignedStaffId())
                .adminNote(report.getAdminNote())
                .build();
    }
}
