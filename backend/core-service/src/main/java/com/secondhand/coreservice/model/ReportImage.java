package com.secondhand.coreservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.secondhand.coreservice.utils.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "report_images")
@Builder
public class ReportImage {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonBackReference
    private Report report;

    @Column(nullable = false)
    private String imageUrl;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = IdGenerator.generateId();
        }
    }
}
