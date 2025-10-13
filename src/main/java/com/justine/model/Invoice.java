package com.justine.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;
    private java.time.LocalDate issuedDate;
    private Double totalAmount;
    private boolean paid;
    private String invoiceUrl;
    @OneToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;
}
