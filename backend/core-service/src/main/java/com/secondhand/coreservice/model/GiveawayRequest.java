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
@Table(name = "giveaway_requests")
public class GiveawayRequest {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Item item;

    private String content;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.id == null) {
            this.id = IdGenerator.generateId();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
