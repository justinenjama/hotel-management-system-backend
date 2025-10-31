package com.justine.controller;

import com.justine.dtos.request.InvoiceRequestDTO;
import com.justine.dtos.response.InvoiceResponseDTO;
import com.justine.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    private Long getCurrentUserId(Principal principal) {
        return Long.parseLong(principal.getName());
    }

    @PostMapping
    public ResponseEntity<InvoiceResponseDTO> generateInvoice(@RequestBody InvoiceRequestDTO dto, Principal principal) {
        return ResponseEntity.ok(invoiceService.generateInvoice(dto, getCurrentUserId(principal)));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<InvoiceResponseDTO> viewInvoice(@PathVariable Long bookingId, Principal principal) {
        return ResponseEntity.ok(invoiceService.viewInvoice(bookingId, getCurrentUserId(principal)));
    }
}
