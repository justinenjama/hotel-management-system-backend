package com.justine.dtos.response;

import com.justine.enums.PaymentMethod;
import com.justine.enums.PaymentStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private Long id;
    private Double amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private LocalDateTime paymentDate;
    private String transactionId;
    private BookingResponseDTO booking;
}
