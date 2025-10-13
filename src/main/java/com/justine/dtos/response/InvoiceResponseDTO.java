package com.justine.dtos.response;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDTO {
    private Long id;
    private String invoiceNumber;
    private String invoiceUrl;
    private LocalDate issuedDate;
    private Double totalAmount;
    private boolean paid;
    private BookingResponseDTO booking;
}
