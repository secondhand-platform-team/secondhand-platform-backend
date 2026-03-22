package com.secondhand.coreservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.secondhand.coreservice.utils.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    private String reviewId;

    // userId
    private String reivewerId;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Item item;

    private Integer rating;

    private String commentContent;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.reviewId == null) {
            this.reviewId = IdGenerator.generateReviewId();
        }
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now();
        }
    }
}
