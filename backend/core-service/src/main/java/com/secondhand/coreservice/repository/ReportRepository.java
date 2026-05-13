package com.secondhand.coreservice.repository;

import com.secondhand.coreservice.model.Report;
import com.secondhand.coreservice.model.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, String> {

    @Query("SELECT r FROM Report r WHERE r.item.itemId = :itemId")
    Page<Report> findByItemId(@Param("itemId") String itemId, Pageable pageable);

    Page<Report> findByReporterId(String reporterId, Pageable pageable);

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    Page<Report> findByAssignedStaffId(String assignedStaffId, Pageable pageable);

    long countByStatus(ReportStatus status);
}
