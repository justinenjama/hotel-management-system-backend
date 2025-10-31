package com.justine.serviceImpl;

import com.justine.dtos.request.InvoiceRequestDTO;
import com.justine.dtos.response.InvoiceResponseDTO;
import com.justine.model.Booking;
import com.justine.model.Invoice;
import com.justine.repository.BookingRepository;
import com.justine.repository.InvoiceRepository;
import com.justine.repository.StaffRepository;
import com.justine.service.AuditLogService;
import com.justine.service.InvoiceService;
import com.justine.utils.InvoiceServiceHelper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Slf4j
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceServiceHelper invoiceServiceHelper;
    private final AuditLogService auditLogService;
    private final StaffRepository staffRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, BookingRepository bookingRepository, InvoiceServiceHelper invoiceServiceHelper, AuditLogService auditLogService, StaffRepository staffRepository) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
        this.invoiceServiceHelper = invoiceServiceHelper;
        this.auditLogService = auditLogService;
        this.staffRepository = staffRepository;
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Long getActorId(Booking booking) {
        if (booking.getStaff() != null && isAdmin()) return booking.getStaff().getId();
        if (booking.getGuest() != null) return booking.getGuest().getId();
        return null;
    }

    @Override
    public InvoiceResponseDTO generateInvoice(InvoiceRequestDTO dto, Long currentUserId) {
        Booking booking = null;
        Long actorId = null;
        try {
            booking = bookingRepository.findById(dto.getBookingId())
                    .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

            if (!isAdmin() && !booking.getGuest().getId().equals(currentUserId)) {
                throw new SecurityException("Forbidden: Not allowed to generate this invoice");
            }

            actorId = getActorId(booking);

            Invoice invoice = Invoice.builder()
                    .invoiceNumber(dto.getInvoiceNumber())
                    .issuedDate(dto.getIssuedDate() != null ? dto.getIssuedDate() : LocalDate.now())
                    .totalAmount(dto.getTotalAmount())
                    .paid(dto.isPaid())
                    .booking(booking)
                    .build();

            Invoice saved = invoiceRepository.save(invoice);

            String url = invoiceServiceHelper.generateAndUploadInvoice(saved);
            saved.setInvoiceUrl(url);
            invoiceRepository.save(saved);

            auditLogService.logInvoice(actorId, "GENERATE_INVOICE", saved.getId(), booking.getId(), url);

            log.info("Invoice {} generated successfully for booking {}", saved.getInvoiceNumber(), booking.getId());
            return mapToResponse(saved);

        } catch (Exception e) {
            auditLogService.logInvoice(actorId, "GENERATE_INVOICE_ERROR", null, dto.getBookingId(), e.getMessage());
            log.error("Failed to generate invoice for booking {}: {}", dto.getBookingId(), e.getMessage());
            throw new RuntimeException("Invoice generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InvoiceResponseDTO viewInvoice(Long bookingId, Long currentUserId) {
        Booking booking = null;
        Long actorId = null;
        try {
            booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

            Invoice invoice = invoiceRepository.findByBookingId(bookingId);
            if (invoice == null) throw new EntityNotFoundException("Invoice not found");

            if (!isAdmin() && !booking.getGuest().getId().equals(currentUserId)) {
                throw new SecurityException("Forbidden: You are not allowed to view this invoice");
            }

            actorId = getActorId(booking);

            auditLogService.logInvoice(actorId, "VIEW_INVOICE", invoice.getId(), bookingId, invoice.getInvoiceUrl());
            log.info("Invoice {} viewed successfully by {}", invoice.getInvoiceNumber(), currentUserId);

            return mapToResponse(invoice);

        } catch (Exception e) {
            auditLogService.logInvoice(actorId, "VIEW_INVOICE_ERROR", null, bookingId, e.getMessage());
            log.error("Failed to view invoice for booking {}: {}", bookingId, e.getMessage());
            throw new RuntimeException("Invoice view failed: " + e.getMessage(), e);
        }
    }

    private InvoiceResponseDTO mapToResponse(Invoice invoice) {
        return InvoiceResponseDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .issuedDate(invoice.getIssuedDate())
                .totalAmount(invoice.getTotalAmount())
                .paid(invoice.isPaid())
                .invoiceUrl(invoice.getInvoiceUrl())
                .build();
    }
}
