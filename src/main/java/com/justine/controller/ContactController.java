package com.justine.controller;

import com.justine.dtos.request.ContactRequestDto;
import com.justine.dtos.request.ReplyRequestDto;
import com.justine.dtos.response.ContactResponseDto;
import com.justine.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<?> addContactMessage(@Valid @RequestBody ContactRequestDto dto) {
        return contactService.addContactMessage(dto);
    }

    @GetMapping
    public ResponseEntity<List<ContactResponseDto>> getAllContactMessages() {
        return contactService.getAllContactMessages();
    }

    @PostMapping("/reply/{id}")
    public ResponseEntity<?> replyContactMessageById(@PathVariable Long id, @RequestBody ReplyRequestDto dto) {
        return contactService.replyContactMessageById(id, dto);
    }
}
