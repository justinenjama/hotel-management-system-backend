package com.justine.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.justine.model.AuditLog;
import com.justine.repository.AuditLogRepository;
import com.justine.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Service
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Heavy job queue executor for high concurrency
    private final ExecutorService auditExecutor = new ThreadPoolExecutor(
            4,
            8,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Generic Save
    @Async("auditExecutor")
    @Transactional
    @Override
    public void logAction(Long actorId, String action, String entity, Long entityId, String metadataJson) {
        auditExecutor.submit(() -> {
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
                log.debug("[AUDIT] {} | action={} | entityId={}", entity, action, entityId);
            } catch (Exception e) {
                log.error("[AUDIT ERROR] Could not save log for {}: {}", entity, e.getMessage());
            }
        });
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
        auditExecutor.submit(() -> {
            try {
                String ip = getClientIp(request);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("ipAddress", ip);
                metadata.put("description", description);
                String json = objectMapper.writeValueAsString(metadata);

                AuditLog logEntry = AuditLog.builder()
                        .actorId(null)
                        .action(action)
                        .entity("PasswordReset")
                        .metadataJson(json)
                        .createdAt(LocalDateTime.now())
                        .build();

                auditLogRepository.save(logEntry);
                log.info("[AUDIT] PasswordReset | action={} | ip={}", action, ip);
            } catch (Exception e) {
                log.error("[AUDIT ERROR] Failed password reset log: {}", e.getMessage());
            }
        });
    }

    // ------------------ Testimonial Logs ------------------
    @Override
    public void logTestimonial(Long actorId, String action, Long testimonialId, Map<String, Object> metadata) {
        logEntity("Testimonial", actorId, action, testimonialId, metadata);
    }

    // ------------------ Comment Logs ------------------
    @Override
    public void logComment(Long actorId, String action, Long commentId, Map<String, Object> metadata) {
        logEntity("TestimonialComment", actorId, action, commentId, metadata);
    }

    // ------------------ Like Logs ------------------
    @Override
    public void logLike(Long actorId, String action, Long likeId, Map<String, Object> metadata) {
        logEntity("TestimonialLike", actorId, action, likeId, metadata);
    }

    // ------------------ System Logs ------------------
    @Override
    public void logSystem(String action, Map<String, Object> metadata) {
        auditExecutor.submit(() -> {
            try {
                String json = objectMapper.writeValueAsString(metadata != null ? metadata : Map.of());
                AuditLog logEntry = AuditLog.builder()
                        .action(action)
                        .entity("System")
                        .metadataJson(json)
                        .createdAt(LocalDateTime.now())
                        .build();

                auditLogRepository.save(logEntry);
                log.debug("[AUDIT] System | action={} | metadata={}", action, json);
            } catch (Exception e) {
                log.error("[AUDIT ERROR] Failed system log '{}': {}", action, e.getMessage());
            }
        });
    }

    // ------------------ Contact Message Logs ------------------
    @Override
    public void logContactMessage(String entityName, Long entityId, String action, Map<String, Object> metadata) {
        auditExecutor.submit(() -> {
            try {
                String json = objectMapper.writeValueAsString(metadata != null ? metadata : Map.of());
                AuditLog auditLog = AuditLog.builder()
                        .entity(entityName)
                        .action(action)
                        .entityId(entityId)
                        .metadataJson(json)
                        .createdAt(LocalDateTime.now())
                        .build();

                auditLogRepository.save(auditLog);
                log.debug("[AUDIT] Contact | action={} | entityId={}", action, entityId);
            } catch (Exception e) {
                log.error("[AUDIT ERROR] Failed contact message log: {}", e.getMessage(), e);
            }
        });
    }

    // ------------------ Private Helper ------------------
    private void logEntity(String entityName, Long actorId, String action, Long entityId, Map<String, Object> metadata) {
        auditExecutor.submit(() -> {
            try {
                String json = objectMapper.writeValueAsString(metadata != null ? metadata : Map.of());
                logAction(actorId, action, entityName, entityId, json);
            } catch (Exception e) {
                log.error("[AUDIT ERROR] Failed {} log {}: {}", entityName, action, e.getMessage());
            }
        });
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip;
    }
}
