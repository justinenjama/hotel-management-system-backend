package com.justine.serviceImpl;

import com.justine.dtos.request.ContactRequestDto;
import com.justine.dtos.request.ReplyRequestDto;
import com.justine.dtos.response.ContactResponseDto;
import com.justine.model.Contact;
import com.justine.model.Staff;
import com.justine.repository.ContactRepository;
import com.justine.repository.StaffRepository;
import com.justine.security.RateLimitService;
import com.justine.service.AuditLogService;
import com.justine.service.ContactService;
import com.justine.service.EmailService;
import com.justine.utils.ValidationUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final StaffRepository staffRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final HttpServletRequest request;
    private final RateLimitService rateLimitService;

    private static final String ADMIN_EMAIL = "admin@justinehotels.com";

    public ContactServiceImpl(ContactRepository contactRepository,
                              StaffRepository staffRepository,
                              AuditLogService auditLogService,
                              EmailService emailService,
                              HttpServletRequest request,
                              RateLimitService rateLimitService) {
        this.contactRepository = contactRepository;
        this.staffRepository = staffRepository;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.request = request;
        this.rateLimitService = rateLimitService;
    }

    // ===========================================================
    // ADD CONTACT MESSAGE
    // ===========================================================
    @Override
    public ResponseEntity<?> addContactMessage(ContactRequestDto dto) {
        try {
            // -------- Validation --------
            if (dto.getEmail() == null || dto.getEmail().isBlank()
                    || dto.getFullName() == null || dto.getFullName().isBlank()
                    || dto.getMessage() == null || dto.getMessage().isBlank()) {
                return buildErrorResponse("All fields are required", HttpStatus.BAD_REQUEST);
            }

            if (!ValidationUtils.isValidEmail(dto.getEmail())) {
                return buildErrorResponse("Invalid email address", HttpStatus.BAD_REQUEST);
            }

            String ipAddress = getClientIp();

            // -------- Rate Limiting --------
            if (!rateLimitService.allowRequest(ipAddress, "CONTACT_MESSAGE")) {
                safeAuditLog("Contact", null, "RATE_LIMIT_EXCEEDED", Map.of(
                        "User", safeValue(dto.getEmail()),
                        "IP", ipAddress
                ));
                return buildErrorResponse("Too many messages. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
            }

            // -------- Save Contact --------
            Contact contact = Contact.builder()
                    .email(dto.getEmail())
                    .fullName(dto.getFullName())
                    .message(dto.getMessage())
                    .ipAddress(ipAddress)
                    .createdAt(LocalDateTime.now())
                    .isReplied(false)
                    .build();

            contactRepository.save(contact);

            safeAuditLog("Contact", contact.getId(), "ADDED_NEW_MESSAGE", Map.of(
                    "User", safeValue(dto.getEmail()),
                    "IP", ipAddress,
                    "Message", safeValue(dto.getMessage())
            ));

            return buildSuccessResponse("Message sent successfully.", mapToResponse(contact));

        } catch (Exception e) {
            log.error("Error adding contact message: {}", e.getMessage(), e);
            safeAuditLog("Contact", null, "ERROR_ADDING_MESSAGE", Map.of("Error", safeValue(e.getMessage())));
            return buildErrorResponse("An error occurred while sending the message.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===========================================================
    // GET ALL CONTACT MESSAGES
    // ===========================================================
    @Override
    public ResponseEntity<List<ContactResponseDto>> getAllContactMessages() {
        try {
            List<ContactResponseDto> messages = contactRepository
                    .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving contact messages: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===========================================================
    // REPLY TO CONTACT MESSAGE
    // ===========================================================
    @Override
    public ResponseEntity<?> replyContactMessageById(Long id, ReplyRequestDto dto) {
        try {
            Contact contact = contactRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Contact message with ID " + id + " not found"));

            if (dto.getReply() == null || dto.getReply().isBlank()) {
                return buildErrorResponse("Reply message cannot be empty", HttpStatus.BAD_REQUEST);
            }

            // -------- Staff Validation --------
            Staff staff = null;
            if (dto.getStaffId() != null) {
                staff = staffRepository.findById(dto.getStaffId()).orElse(null);
                if (staff == null) {
                    return buildErrorResponse("Invalid staff ID", HttpStatus.BAD_REQUEST);
                }
            }

            // -------- Update Contact --------
            contact.setReply(dto.getReply());
            contact.setRepliedAt(LocalDateTime.now());
            contact.setIsReplied(true);
            if (staff != null) contact.setRepliedBy(staff);
            contactRepository.save(contact);

            // -------- Send Email Async --------
            String subject = "Response to your message at Justine Hotels";
            String body = buildReplyEmail(contact, dto.getReply());
            sendReplyEmailAsync(contact.getEmail(), subject, body);

            safeAuditLog("Contact", id, "REPLIED_MESSAGE", Map.of(
                    "UserEmail", safeValue(contact.getEmail()),
                    "AdminReply", safeValue(dto.getReply()),
                    "RepliedByStaffId", safeValue(dto.getStaffId())
            ));

            log.info("Reply sent successfully to {}", contact.getEmail());
            return buildSuccessResponse("Reply sent successfully to user.", mapToResponse(contact));

        } catch (EntityNotFoundException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error replying to contact message: {}", e.getMessage(), e);
            safeAuditLog("Contact", id, "ERROR_REPLY_MESSAGE", Map.of("Error", safeValue(e.getMessage())));
            return buildErrorResponse("Failed to send reply to user.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===========================================================
    // HELPER METHODS
    // ===========================================================

    private ContactResponseDto mapToResponse(Contact contact) {
        return ContactResponseDto.builder()
                .id(contact.getId())
                .fullName(contact.getFullName())
                .email(contact.getEmail())
                .message(contact.getMessage())
                .reply(contact.getReply())
                .isReplied(contact.getIsReplied())
                .createdAt(contact.getCreatedAt())
                .repliedAt(contact.getRepliedAt())
                .build();
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private void safeAuditLog(String entity, Long entityId, String action, Map<String, Object> data) {
        try {
            auditLogService.logContactMessage(entity, entityId, action, data != null ? data : Map.of());
        } catch (Exception e) {
            log.warn("Failed to log audit: {}", e.getMessage());
        }
    }

    private Object safeValue(Object value) {
        return value != null ? value : "N/A";
    }

    private ResponseEntity<?> buildSuccessResponse(String message, Object data) {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", message,
                "data", data
        ));
    }

    private ResponseEntity<?> buildErrorResponse(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(Map.of(
                "status", "error",
                "message", message
        ));
    }

    private String buildReplyEmail(Contact contact, String reply) {
        return String.format("""
                <p>Dear %s,</p>
                <p>Thank you for reaching out to us. Here’s our response to your message:</p>
                <blockquote>%s</blockquote>
                <hr/>
                <p><b>Admin Response:</b></p>
                <p>%s</p>
                <br/>
                <p>Kind regards,<br/>Justine Hotels Support Team</p>
                """, safeValue(contact.getFullName()), safeValue(contact.getMessage()), safeValue(reply));
    }

    @Async
    public void sendReplyEmailAsync(String to, String subject, String body) {
        try {
            emailService.sendEmail(to, subject, body);
        } catch (Exception e) {
            log.error("Async email sending failed: {}", e.getMessage());
            safeAuditLog("Email", null, "ASYNC_EMAIL_FAILED", Map.of(
                    "Recipient", safeValue(to),
                    "Error", safeValue(e.getMessage())
            ));
        }
    }
}
