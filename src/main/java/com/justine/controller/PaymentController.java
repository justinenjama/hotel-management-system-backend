package com.justine.controller;

import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.response.PaymentResponseDTO;
import com.justine.service.PaymentService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> recordPayment(@RequestBody PaymentRequestDTO dto) {

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(paymentService.recordPayment(dto, currentUserEmail));
    }
}
