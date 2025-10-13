package com.justine.dtos.request;

import com.justine.enums.PaymentMethod;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {
    private Double amount;
    private PaymentMethod method;
    private String transactionId;
    private Long bookingId;
}
