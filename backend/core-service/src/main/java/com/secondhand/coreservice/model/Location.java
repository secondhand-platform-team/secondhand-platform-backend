package com.secondhand.coreservice.model;

import com.secondhand.coreservice.utils.IdGenerator;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "locations")
public class Location {

    @Id
    private String locationId;

    private String address;

    private String ward;

    private String district;

    private String city;

    @OneToOne
    @JoinColumn(name = "item_id")
    private Item item;

    @PrePersist
    protected void onPrePersist() {
        if (this.locationId == null) {
            this.locationId = IdGenerator.generateLocationId();
        }
    }
}
