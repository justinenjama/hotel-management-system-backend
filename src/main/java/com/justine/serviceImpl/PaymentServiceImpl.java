package com.justine.serviceImpl;

import com.justine.dtos.request.InvoiceRequestDTO;
import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.response.PaymentResponseDTO;
import com.justine.enums.PaymentStatus;
import com.justine.model.Booking;
import com.justine.model.Payment;
import com.justine.repository.BookingRepository;
import com.justine.repository.PaymentRepository;
import com.justine.service.AuditLogService;
import com.justine.service.InvoiceService;
import com.justine.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceService invoiceService;
    private final AuditLogService auditLogService;

    public PaymentServiceImpl(PaymentRepository paymentRepository, BookingRepository bookingRepository, InvoiceService invoiceService, AuditLogService auditLogService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.invoiceService = invoiceService;
        this.auditLogService = auditLogService;
    }

    // ------------------ Authorization helpers ------------------
    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String getCurrentPrincipalName() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    // ------------------ Record Payment ------------------
    @Override
    public PaymentResponseDTO recordPayment(PaymentRequestDTO dto, String currentUserEmail) {
        try {
            Booking booking = bookingRepository.findById(dto.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            String principal = getCurrentPrincipalName();
            if (!isAdmin() && (principal == null || !principal.equals(booking.getGuest().getEmail()))) {
                throw new RuntimeException("Forbidden: not allowed to record this payment");
            }

            Payment payment = Payment.builder()
                    .amount(dto.getAmount())
                    .method(dto.getMethod())
                    .status(PaymentStatus.PAID)
                    .transactionId(dto.getTransactionId())
                    .paymentDate(LocalDateTime.now())
                    .booking(booking)
                    .build();

            Payment saved = paymentRepository.save(payment);
            booking.setPayment(saved);

            // Auto-generate invoice
            invoiceService.generateInvoice(
                    InvoiceRequestDTO.builder()
                            .invoiceNumber("INV-" + System.currentTimeMillis())
                            .issuedDate(LocalDate.now())
                            .totalAmount(saved.getAmount())
                            .paid(true)
                            .bookingId(booking.getId())
                            .build()
            );

            // Audit success
            auditLogService.logPayment(
                    booking.getGuest().getId(),
                    "RECORD_PAYMENT_SUCCESS",
                    saved.getId(),
                    Map.of(
                            "bookingId", booking.getId(),
                            "amount", saved.getAmount(),
                            "method", saved.getMethod(),
                            "transactionId", saved.getTransactionId()
                    )
            );

            log.info("Payment recorded successfully for booking {}", booking.getId());

            return PaymentResponseDTO.builder()
                    .id(saved.getId())
                    .amount(saved.getAmount())
                    .method(saved.getMethod())
                    .status(saved.getStatus())
                    .transactionId(saved.getTransactionId())
                    .paymentDate(saved.getPaymentDate())
                    .build();

        } catch (Exception e) {
            log.error("Error recording payment: {}", e.getMessage());

            auditLogService.logPayment(
                    null,
                    "RECORD_PAYMENT_ERROR",
                    null,
                    Map.of(
                            "error", e.getMessage(),
                            "request", dto
                    )
            );

            throw new RuntimeException("Payment recording failed: " + e.getMessage());
        }
    }
}
