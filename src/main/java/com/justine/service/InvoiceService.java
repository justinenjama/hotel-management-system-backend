package com.justine.service;

import com.justine.dtos.request.InvoiceRequestDTO;
import com.justine.dtos.response.InvoiceResponseDTO;

public interface InvoiceService {
    InvoiceResponseDTO generateInvoice(InvoiceRequestDTO dto, Long currentUserId);

    InvoiceResponseDTO viewInvoice(Long bookingId, Long currentUserId);
}
