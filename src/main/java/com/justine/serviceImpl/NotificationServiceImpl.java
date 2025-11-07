package com.justine.serviceImpl;

import com.justine.dtos.request.NotificationRequestDto;
import com.justine.model.*;
import com.justine.repository.BookingRepository;
import com.justine.repository.NotificationRepository;
import com.justine.service.AuditLogService;
import com.justine.service.EmailService;
import com.justine.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    public NotificationServiceImpl(SimpMessagingTemplate messagingTemplate,
                                   BookingRepository bookingRepository,
                                   EmailService emailService,
                                   NotificationRepository notificationRepository,
                                   AuditLogService auditLogService) {
        this.messagingTemplate = messagingTemplate;
        this.bookingRepository = bookingRepository;
        this.emailService = emailService;
        this.notificationRepository = notificationRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public void broadcast(NotificationRequestDto notification) {
        messagingTemplate.convertAndSend("/topic/alerts", notification);
    }

    @Override
    public void sendToUser(String username, NotificationRequestDto notification) {
        messagingTemplate.convertAndSendToUser(username, "/queue/alerts", notification);
    }

    @Override
    public void alertActiveGuests(NotificationRequestDto notification) {
        try {
            LocalDate today = LocalDate.now();
            List<Booking> activeBookings = bookingRepository.findByCheckInDateBeforeAndCheckOutDateAfter(today, today);

            sendNotificationToBookings(activeBookings, notification, null);
        } catch (Exception e) {
            log.error("❌ Failed to alert active guests: {}", e.getMessage());
        }
    }

    @Override
    public void alertHotel(Long hotelId, Long actorId, NotificationRequestDto notification) {
        try {
            List<Booking> activeBookings = bookingRepository.findActiveBookingsByHotel(hotelId, LocalDate.now());

            if (activeBookings.isEmpty()) {
                log.warn("No active bookings found for hotelId={}", hotelId);
                return;
            }

            sendNotificationToBookings(activeBookings, notification, actorId);

            auditLogService.logHotel(actorId, "Emergency alert triggered", hotelId,
                    Map.of("title", notification.getTitle(), "message", notification.getMessage())
            );

        } catch (Exception e) {
            log.error("❌ Failed to alert hotelId={}: {}", hotelId, e.getMessage());
            auditLogService.logSystem("Failed to send emergency alert for hotelId=" + hotelId,
                    Map.of("error", e.getMessage()));
        }
    }

    private void sendNotificationToBookings(List<Booking> bookings, NotificationRequestDto notification, Long actorId) {
        try {
            // Collect guest emails
            List<String> guestEmails = bookings.stream()
                    .map(Booking::getGuest)
                    .filter(g -> g != null && g.getEmail() != null)
                    .map(Guest::getEmail)
                    .distinct()
                    .collect(Collectors.toList());

            // Collect staff emails
            List<String> staffEmails = bookings.stream()
                    .map(Booking::getStaff)
                    .filter(s -> s != null && s.getEmail() != null)
                    .map(Staff::getEmail)
                    .distinct()
                    .collect(Collectors.toList());

            // Merge and send emails
            List<String> allEmails = guestEmails;
            allEmails.addAll(staffEmails.stream()
                    .filter(email -> !allEmails.contains(email))
                    .collect(Collectors.toList()));

            if (!allEmails.isEmpty()) {
                emailService.sendEmailToMultiple(
                        allEmails.toArray(new String[0]),
                        notification.getTitle(),
                        notification.getMessage()
                );
            }

            // Save notifications to DB
            bookings.forEach(booking -> {
                Guest guest = booking.getGuest();
                if (guest != null) {
                    Notification n = Notification.builder()
                            .guest(guest)
                            .title(notification.getTitle())
                            .message(notification.getMessage())
                            .severity(notification.getSeverity())
                            .build();
                    notificationRepository.save(n);
                }

                Staff staff = booking.getStaff();
                if (staff != null) {
                    Notification n = Notification.builder()
                            .staff(staff)
                            .title(notification.getTitle())
                            .message(notification.getMessage())
                            .severity(notification.getSeverity())
                            .build();
                    notificationRepository.save(n);
                }
            });

            // Broadcast via WebSocket
            broadcast(notification);

        } catch (Exception e) {
            log.error("❌ Failed to send notifications to bookings: {}", e.getMessage());
            if (actorId != null) {
                auditLogService.logSystem("Failed to send emergency alert for actorId=" + actorId,
                        Map.of("error", e.getMessage()));
            }
        }
    }
}
