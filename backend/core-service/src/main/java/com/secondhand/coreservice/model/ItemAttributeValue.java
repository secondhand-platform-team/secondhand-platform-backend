package com.secondhand.coreservice.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import com.secondhand.coreservice.utils.IdGenerator;
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
@Table(name = "item_attribute_values", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "item_id", "attribute_id" })
})
@Builder
public class ItemAttributeValue {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    @JsonIgnore
    private Item item;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private CategoryAttribute attribute;
    @Column(columnDefinition = "TEXT")
    private String valueString;
    private BigDecimal valueNumber;
    private Long valueInteger;
    private Boolean valueBoolean;
    private LocalDate valueDate;
    @Column(columnDefinition = "TEXT")
    private String valueJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = IdGenerator.generateAttributeValueId();
        }
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now();
        }
    }
}
