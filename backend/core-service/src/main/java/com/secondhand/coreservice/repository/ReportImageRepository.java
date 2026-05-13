package com.secondhand.coreservice.repository;

import com.secondhand.coreservice.model.ReportImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportImageRepository extends JpaRepository<ReportImage, String> {
    List<ReportImage> findByReportId(String reportId);
}
