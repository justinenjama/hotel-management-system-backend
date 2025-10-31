package com.justine.repository;

import com.justine.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Invoice findByBookingId(Long bookingId);

    Invoice findByInvoiceNumber(String bookingInvoiceNumber);
}

