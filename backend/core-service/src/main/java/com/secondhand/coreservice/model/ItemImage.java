package com.secondhand.coreservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.secondhand.coreservice.utils.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "item_images")
public class ItemImage {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Item item;

    private String url;

    private String cloudinaryPublicId;

    private Boolean isThumbnail;

    private Integer displayOrder;

    private Boolean isPrimary;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = IdGenerator.generateItemImageId();
        }
    }
}
