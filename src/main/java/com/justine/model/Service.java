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
@Table(
        name = "services",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_service_per_hotel",
                        columnNames = {"hotel_id", "service_type"}
                )
        }
)
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    private String name;

    private String description;

    private Double price;

    @ManyToMany(mappedBy = "services")
    private List<Booking> bookings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    /**
     * Initialize price if null using the default for the selected service type.
     */
    @PrePersist
    public void applyDefaultPrice() {
        if (this.price == null && this.serviceType != null) {
            this.price = this.serviceType.getDefaultPriceKES();
        }
    }
}
