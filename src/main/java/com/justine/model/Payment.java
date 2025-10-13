package com.justine.model;

import com.justine.enums.PaymentMethod;
import com.justine.enums.PaymentStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private java.time.LocalDateTime paymentDate;
    private String transactionId;

    @OneToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;
}
