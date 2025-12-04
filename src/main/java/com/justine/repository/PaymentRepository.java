package com.justine.repository;

import com.justine.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByBookingId(Long bookingId);

    Optional<Payment> findByCheckoutRequestId(String checkoutRequestId);
}
