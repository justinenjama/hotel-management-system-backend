package com.justine.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justine.model.AuditLog;
import com.justine.repository.AuditLogRepository;
import com.justine.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // ------------------ Generic Save ------------------
    @Override
    public void logAction(Long actorId, String action, String entity, Long entityId, String metadataJson) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .actorId(actorId)
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .metadataJson(metadataJson)
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(logEntry);
            log.info("[AUDIT] {} | action={} | entityId={}", entity, action, entityId);
        } catch (Exception e) {
            log.error("[AUDIT ERROR] Could not save log for {}: {}", entity, e.getMessage());
        }
    }

    // ------------------ Booking Logs ------------------
    @Override
    public void logBooking(Long actorId, String action, Long bookingId, Map<String, Object> metadata) {
        logEntity("Booking", actorId, action, bookingId, metadata);
    }

    // ------------------ Guest Logs ------------------
    @Override
    public void logGuest(Long actorId, String action, Long guestId, Map<String, Object> metadata) {
        logEntity("Guest", actorId, action, guestId, metadata);
    }

    // ------------------ Hotel Logs ------------------
    @Override
    public void logHotel(Long actorId, String action, Long hotelId, Map<String, Object> metadata) {
        logEntity("Hotel", actorId, action, hotelId, metadata);
    }

    // ------------------ Invoice Logs ------------------
    @Override
    public void logInvoice(Long actorId, String action, Long invoiceId, Long bookingId, String invoiceUrl) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("bookingId", bookingId);
            metadata.put("invoiceUrl", invoiceUrl);

            String json = objectMapper.writeValueAsString(metadata);
            logAction(actorId, action, "Invoice", invoiceId, json);
        } catch (Exception e) {
            log.error("[AUDIT ERROR] Failed invoice log {}: {}", action, e.getMessage());
        }
    }

    // ------------------ Payment Logs ------------------
    @Override
    public void logPayment(Long actorId, String action, Long paymentId, Map<String, Object> metadata) {
        logEntity("Payment", actorId, action, paymentId, metadata);
    }

    // ------------------ Restaurant Logs ------------------
    @Override
    public void logRestaurant(Long actorId, String action, Long restaurantId, Map<String, Object> metadata) {
        logEntity("Restaurant", actorId, action, restaurantId, metadata);
    }

    // ------------------ Service Logs ------------------
    @Override
    public void logService(Long actorId, String action, Long serviceId, Map<String, Object> metadata) {
        logEntity("Service", actorId, action, serviceId, metadata);
    }

    // ------------------ Staff Logs ------------------
    @Override
    public void logStaff(Long actorId, String action, Long staffId, Map<String, Object> metadata) {
        logEntity("Staff", actorId, action, staffId, metadata);
    }

    // ------------------ Auth Service Logs ------------------
    @Override
    public void logAuthService(Long actorId, String action, Map<String, Object> metadata) {
        logEntity("AuthService", actorId, action, null, metadata);
    }

    // ------------------ Password Reset Logs ------------------
    @Override
    public void logPasswordResetAction(HttpServletRequest request, String action, String description) {
        try {
            String ip = getClientIp(request);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("ipAddress", ip);
            metadata.put("description", description);

            String json = objectMapper.writeValueAsString(metadata);

            AuditLog logEntry = AuditLog.builder()
                    .actorId(null) // unknown until authenticated
                    .action(action)
                    .entity("PasswordReset")
                    .entityId(null)
                    .metadataJson(json)
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(logEntry);
            log.info("[AUDIT] PasswordReset | action={} | ip={}", action, ip);

        } catch (Exception e) {
            log.error("[AUDIT ERROR] Failed password reset log: {}", e.getMessage());
        }
    }

    // ------------------ Private Helper ------------------
    private void logEntity(String entityName, Long actorId, String action, Long entityId, Map<String, Object> metadata) {
        try {
            String json = objectMapper.writeValueAsString(metadata != null ? metadata : Map.of());
            logAction(actorId, action, entityName, entityId, json);
        } catch (Exception e) {
            log.error("[AUDIT ERROR] Failed {} log {}: {}", entityName, action, e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
