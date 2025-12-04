package com.justine.serviceImpl;

import com.justine.dtos.request.InvoiceRequestDTO;
import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.response.PaymentResponseDTO;
import com.justine.enums.PaymentStatus;
import com.justine.model.Booking;
import com.justine.model.Payment;
import com.justine.repository.BookingRepository;
import com.justine.repository.PaymentRepository;
import com.justine.repository.StaffRepository;
import com.justine.service.AuditLogService;
import com.justine.service.InvoiceService;
import com.justine.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final StaffRepository staffRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository, BookingRepository bookingRepository, InvoiceService invoiceService, AuditLogService auditLogService, StaffRepository staffRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.invoiceService = invoiceService;
        this.auditLogService = auditLogService;
        this.staffRepository = staffRepository;
    }

    private boolean isAdmin(Long currentUserId) {
        return staffRepository.findById(currentUserId)
                .map(staff -> "ADMIN".equalsIgnoreCase(String.valueOf(staff.getRole())))
                .orElse(false);
    }

    @Override
    public PaymentResponseDTO recordPayment(PaymentRequestDTO dto, Long currentUserId) {
        try {
            Booking booking = bookingRepository.findById(dto.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!isAdmin(currentUserId) && !booking.getGuest().getId().equals(currentUserId)) {
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

            InvoiceRequestDTO invoiceRequest = InvoiceRequestDTO.builder()
                    .bookingId(booking.getId())
                    .invoiceNumber("INV-" + System.currentTimeMillis())
                    .issuedDate(LocalDateTime.now().toLocalDate())
                    .totalAmount(saved.getAmount())
                    .paid(true)
                    .build();

            invoiceService.generateInvoice(invoiceRequest, currentUserId);

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
            auditLogService.logPayment(null, "RECORD_PAYMENT_ERROR", null, Map.of("error", e.getMessage(), "request", dto));
            throw new RuntimeException("Payment recording failed: " + e.getMessage());
        }
    }

}
