package com.justine.service;

import com.justine.dtos.request.ContactRequestDto;
import com.justine.dtos.request.ReplyRequestDto;
import com.justine.dtos.response.ContactResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ContactService {

    ResponseEntity<?> addContactMessage(ContactRequestDto dto);

    ResponseEntity<List<ContactResponseDto>> getAllContactMessages();

    ResponseEntity<?> replyContactMessageById(Long id, ReplyRequestDto dto);
}
