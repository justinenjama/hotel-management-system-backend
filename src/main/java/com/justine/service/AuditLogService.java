package com.justine.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface AuditLogService {

    // Generic method for internal use
    void logAction(Long actorId, String action, String entity, Long entityId, String metadataJson);

    // Entity-specific log methods
    void logBooking(Long actorId, String action, Long bookingId, Map<String, Object> metadata);

    void logGuest(Long actorId, String action, Long guestId, Map<String, Object> metadata);

    void logHotel(Long actorId, String action, Long hotelId, Map<String, Object> metadata);

    void logInvoice(Long actorId, String action, Long invoiceId, Long bookingId, String invoiceUrl);

    void logPayment(Long actorId, String action, Long paymentId, Map<String, Object> metadata);

    void logRestaurant(Long actorId, String action, Long restaurantId, Map<String, Object> metadata);

    void logService(Long actorId, String action, Long serviceId, Map<String, Object> metadata);

    void logStaff(Long actorId, String action, Long staffId, Map<String, Object> metadata);

    void logAuthService(Long actorId, String action, Map<String, Object> metadata);

    void logPasswordResetAction(HttpServletRequest request, String action, String description);

}
