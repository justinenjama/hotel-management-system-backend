package com.justine.controller;

import com.justine.dtos.request.InvoiceRequestDTO;
import com.justine.dtos.response.InvoiceResponseDTO;
import com.justine.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponseDTO> generateInvoice(@RequestBody InvoiceRequestDTO dto) {
        return ResponseEntity.ok(invoiceService.generateInvoice(dto));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<InvoiceResponseDTO> viewInvoice(@PathVariable Long bookingId) {
        return ResponseEntity.ok(invoiceService.viewInvoice(bookingId));
    }
}
