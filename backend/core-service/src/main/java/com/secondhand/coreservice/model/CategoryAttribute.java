package com.secondhand.coreservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.secondhand.coreservice.model.enums.AttributeDataType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.JoinColumn;
import com.secondhand.coreservice.utils.IdGenerator;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "category_attributes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "category_id", "code" })
})
@Builder
public class CategoryAttribute {
    @Id
    private String attributeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttributeDataType dataType;
    private String unit;
    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;
    @Column(nullable = false)
    @Builder.Default
    private Boolean filterable = true;
    @Column(nullable = false)
    @Builder.Default
    private Boolean searchable = false;
    private BigDecimal minValueNumber;
    private BigDecimal maxValueNumber;
    @Column(columnDefinition = "TEXT")
    private String optionsJson;
    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.attributeId == null) {
            this.attributeId = IdGenerator.generateAttributeId();
        }
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now();
        }
    }
}
