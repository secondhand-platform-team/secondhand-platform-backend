package com.secondhand.coreservice.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
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
@Table(name = "categories")
@Builder
public class Category {

    @Id
    private String categoryId;

    private String name;

    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Category> children;


    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<CategoryAttribute> attributes;

    @PrePersist
    protected void onPrePersist() {
        if (this.categoryId == null) {
            this.categoryId = IdGenerator.generateCategoryId();
        }
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now();
        }
    }

}
