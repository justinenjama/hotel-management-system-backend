package com.justine.dtos.request;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRequestDTO {
    private String invoiceNumber;
    private LocalDate issuedDate;
    private Double totalAmount;
    private boolean paid;
    private Long bookingId;
}
