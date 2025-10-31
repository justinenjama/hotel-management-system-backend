package com.justine.dtos.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDTO {
    private Long id;
    private String invoiceNumber;
    private String invoiceUrl;
    private String invoiceUrlMedium;
    private String invoiceUrlThumbnail;
    private LocalDateTime generatedAt;
    private LocalDate issuedDate;
    private Double totalAmount;
    private boolean paid;
    private BookingResponseDTO booking;
}
