package com.justine.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import com.justine.enums.ServiceType;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    private Double price;

    @ManyToMany(mappedBy = "services")
    private List<Booking> bookings;
}
