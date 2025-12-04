package com.justine.model;

import com.justine.enums.PaymentMethod;
import com.justine.enums.PaymentStatus;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    private java.time.LocalDateTime paymentDate;
    private String transactionId;

    @OneToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @OneToOne
    @JoinColumn(name = "restaurant_order_id")
    private RestaurantOrder restaurantOrder;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Guest guest;
    private String mpesaReceiptNumber;

    @Column(unique = true)
    private String checkoutRequestId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.MPESA;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

}
