package com.justine.serviceImpl;

import com.justine.dtos.request.InvoiceRequestDTO;
import com.justine.dtos.response.InvoiceResponseDTO;
import com.justine.model.Booking;
import com.justine.model.Invoice;
import com.justine.repository.BookingRepository;
import com.justine.repository.InvoiceRepository;
import com.justine.service.AuditLogService;
import com.justine.service.InvoiceService;
import com.justine.utils.InvoiceServiceHelper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceServiceHelper invoiceServiceHelper;
    private final AuditLogService auditLogService;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, BookingRepository bookingRepository, InvoiceServiceHelper invoiceServiceHelper, AuditLogService auditLogService) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
        this.invoiceServiceHelper = invoiceServiceHelper;
        this.auditLogService = auditLogService;
    }

    // ------------------ Authorization helpers ------------------
    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String getCurrentPrincipalName() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private Long getActorId(Booking booking) {
        if (booking.getStaff() != null && isAdmin()) return booking.getStaff().getId();
        if (booking.getGuest() != null) return booking.getGuest().getId();
        return null;
    }

    // ------------------ Generate Invoice ------------------
    @Override
    public InvoiceResponseDTO generateInvoice(InvoiceRequestDTO dto) {
        Booking booking = null;
        Long actorId = null;

        try {
            booking = bookingRepository.findById(dto.getBookingId())
                    .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

            String principal = getCurrentPrincipalName();
            if (!isAdmin() && (principal == null || !principal.equals(booking.getGuest().getEmail()))) {
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

            // Generate and upload PDF invoice to Cloudinary
            String url = invoiceServiceHelper.generateAndUploadInvoice(saved);
            saved.setInvoiceUrl(url);
            invoiceRepository.save(saved);

            // Log successful invoice generation
            auditLogService.logInvoice(
                    actorId,
                    "GENERATE_INVOICE",
                    saved.getId(),
                    booking.getId(),
                    url
            );

            log.info("Invoice {} generated successfully for booking {}", saved.getInvoiceNumber(), booking.getId());
            return mapToResponse(saved);
        } catch (Exception e) {
            log.error("Failed to generate invoice for booking {}: {}", dto.getBookingId(), e.getMessage());

            // Log invoice error to audit log as an action
            auditLogService.logInvoice(
                    actorId,
                    "GENERATE_INVOICE_ERROR",
                    null,
                    dto.getBookingId(),
                    e.getMessage()
            );

            throw new RuntimeException("Invoice generation failed: " + e.getMessage(), e);
        }
    }

    // ------------------ View/Download Invoice ------------------
    @Override
    public InvoiceResponseDTO viewInvoice(Long bookingId) {
        Booking booking = null;
        Long actorId = null;

        try {
            booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

            Invoice invoice = invoiceRepository.findByBookingId(bookingId);
            if (invoice == null) {
                throw new EntityNotFoundException("Invoice not found for booking ID " + bookingId);
            }

            String principal = getCurrentPrincipalName();
            if (!isAdmin() && (principal == null || !principal.equals(booking.getGuest().getEmail()))) {
                throw new SecurityException("Forbidden: You are not allowed to view this invoice");
            }

            actorId = getActorId(booking);

            // Log successful view
            auditLogService.logInvoice(
                    actorId,
                    "VIEW_INVOICE",
                    invoice.getId(),
                    bookingId,
                    invoice.getInvoiceUrl()
            );

            log.info("Invoice {} viewed successfully by {}", invoice.getInvoiceNumber(), principal);
            return mapToResponse(invoice);
        } catch (EntityNotFoundException e) {
            log.warn("Invoice view failed for booking {}: {}", bookingId, e.getMessage());

            auditLogService.logInvoice(
                    actorId,
                    "VIEW_INVOICE_ERROR",
                    null,
                    bookingId,
                    e.getMessage()
            );

            throw e;
        } catch (Exception e) {
            log.error("Unexpected error viewing invoice for booking {}: {}", bookingId, e.getMessage());

            auditLogService.logInvoice(
                    actorId,
                    "VIEW_INVOICE_ERROR",
                    null,
                    bookingId,
                    e.getMessage()
            );

            throw new RuntimeException("Failed to view invoice: " + e.getMessage(), e);
        }
    }

    // ------------------ Helper ------------------
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
