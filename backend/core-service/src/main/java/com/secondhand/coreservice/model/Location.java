package com.secondhand.coreservice.model;
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

    @OneToOne
    @JoinColumn(name = "item_id")
    private Item item;
}
