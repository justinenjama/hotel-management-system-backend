package com.justine.controller;

import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.response.PaymentResponseDTO;
import com.justine.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    private Long getCurrentUserId(Principal principal) {
        return Long.parseLong(principal.getName());
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> recordPayment(@RequestBody PaymentRequestDTO dto, Principal principal) {
        return ResponseEntity.ok(paymentService.recordPayment(dto, getCurrentUserId(principal)));
    }
}
