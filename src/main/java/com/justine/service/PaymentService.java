package com.justine.service;

import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.response.PaymentResponseDTO;

public interface PaymentService {
    PaymentResponseDTO recordPayment(PaymentRequestDTO dto, String currentUserEmail);
}



