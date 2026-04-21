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
@Table(name = "favorite_items")

public class FavoriteItem {
    @Id
    private String id;

    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Item item;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = IdGenerator.generateId();
        }
    }
}
