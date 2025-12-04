package com.justine.serviceImpl;

import com.justine.dtos.request.NotificationRequestDto;
import com.justine.dtos.response.GuestResponseDTO;
import com.justine.dtos.response.NotificationResponseDto;
import com.justine.dtos.response.StaffResponseDTO;
import com.justine.enums.DeliveryStatus;
import com.justine.model.*;
import com.justine.repository.BookingRepository;
import com.justine.repository.GuestRepository;
import com.justine.repository.NotificationRepository;
import com.justine.repository.StaffRepository;
import com.justine.service.AuditLogService;
import com.justine.service.EmailService;
import com.justine.service.NotificationService;
import com.justine.service.SMSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;
    private final GuestRepository guestRepository;
    private final StaffRepository staffRepository;
    private final SMSService smsService;

    public NotificationServiceImpl(SimpMessagingTemplate messagingTemplate,
                                   BookingRepository bookingRepository,
                                   EmailService emailService,
                                   NotificationRepository notificationRepository,
                                   AuditLogService auditLogService,
                                   GuestRepository guestRepository,
                                   StaffRepository staffRepository,
                                   SMSService smsService) {

        this.messagingTemplate = messagingTemplate;
        this.bookingRepository = bookingRepository;
        this.emailService = emailService;
        this.notificationRepository = notificationRepository;
        this.auditLogService = auditLogService;
        this.guestRepository = guestRepository;
        this.staffRepository = staffRepository;
        this.smsService = smsService;
    }

    // WEBSOCKET
    @Override
    public void broadcast(NotificationRequestDto notification) {
        messagingTemplate.convertAndSend("/topic/alerts", notification);
    }

    @Override
    public void sendToUser(String username, NotificationRequestDto notification) {
        messagingTemplate.convertAndSendToUser(username, "/queue/alerts", notification);
    }

    // ALERT ACTIVE GUESTS (GUESTS ONLY)
    @Override
    public void alertActiveGuests(NotificationRequestDto notification) {
        try {
            LocalDate today = LocalDate.now();
            List<Booking> activeBookings =
                    bookingRepository.findByCheckInDateBeforeAndCheckOutDateAfter(today, today);

            if (activeBookings.isEmpty()) {
                log.warn("No active guest bookings found");
                return;
            }

            notifyGuestsFromBookings(activeBookings, notification, null);

        } catch (Exception e) {
            log.error("Failed to alert active guests: {}", e.getMessage(), e);
        }
    }

    // ALERT HOTEL = GUESTS + HOTEL STAFF ONLY
    @Override
    public void alertHotel(Long hotelId, Long actorId, NotificationRequestDto notification) {
        try {
            // ---- ACTIVE GUESTS ONLY ----
            List<Booking> activeBookings =
                    bookingRepository.findActiveBookingsByHotel(hotelId, LocalDate.now());

            if (!activeBookings.isEmpty()) {
                notifyGuestsFromBookings(activeBookings, notification, actorId);
            } else {
                log.warn("No active bookings found for hotelId={}", hotelId);
            }

            // ---- HOTEL STAFF ONLY ----
            List<Staff> staffMembers = staffRepository.findByHotelId(hotelId);

            if (!staffMembers.isEmpty()) {
                notifyHotelStaff(staffMembers, notification, actorId);
            } else {
                log.warn("No staff found for hotelId={}", hotelId);
            }

            auditLogService.logHotel(
                    actorId,
                    "Emergency alert triggered",
                    hotelId,
                    Map.of(
                            "title", notification.getTitle(),
                            "message", notification.getMessage(),
                            "recipients", "Guests and Staff"
                    )
            );

        } catch (Exception e) {
            log.error("Failed to alert hotelId={}: {}", hotelId, e.getMessage(), e);
            auditLogService.logSystem(
                    "Failed to send emergency alert for hotelId=" + hotelId,
                    Map.of("error", e.getMessage())
            );
        }
    }

    // SHARED DELIVERY CORE (EMAIL + SMS)
    private void sendEmailAndSms(Set<String> emails,
                                 Set<String> phones,
                                 String title,
                                 String message) {

        if (!emails.isEmpty()) {
            emailService.sendEmailToMultiple(
                    emails.toArray(new String[0]),
                    title,
                    message
            );
        }

        if (!phones.isEmpty()) {
            smsService.sendBulkSMS(
                    phones.toArray(new String[0]),
                    message
            );
        }
    }

    // GUEST NOTIFICATION (FROM BOOKINGS ONLY)
    private void notifyGuestsFromBookings(List<Booking> bookings,
                                          NotificationRequestDto notification,
                                          Long actorId) {
        try {
            List<Guest> guests = bookings.stream()
                    .map(Booking::getGuest)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            Set<String> emails = guests.stream()
                    .map(Guest::getEmail)
                    .filter(e -> e != null && !e.isBlank())
                    .collect(Collectors.toSet());

            Set<String> phones = guests.stream()
                    .map(Guest::getPhoneNumber)
                    .filter(p -> p != null && !p.isBlank())
                    .collect(Collectors.toSet());

            sendEmailAndSms(emails, phones,
                    notification.getTitle(),
                    notification.getMessage());

            saveGuestNotifications(guests, notification);

            broadcast(notification);

        } catch (Exception e) {
            log.error("Failed to notify guests: {}", e.getMessage(), e);
            if (actorId != null) {
                auditLogService.logSystem(
                        "Guest alert failed for actorId=" + actorId,
                        Map.of("error", e.getMessage())
                );
            }
        }
    }

    // STAFF NOTIFICATION (HOTEL STAFF ONLY)
    private void notifyHotelStaff(List<Staff> staffMembers,
                                  NotificationRequestDto notification,
                                  Long actorId) {
        try {
            Set<String> emails = staffMembers.stream()
                    .map(Staff::getEmail)
                    .filter(e -> e != null && !e.isBlank())
                    .collect(Collectors.toSet());

            Set<String> phones = staffMembers.stream()
                    .map(Staff::getPhoneNumber)
                    .filter(p -> p != null && !p.isBlank())
                    .collect(Collectors.toSet());

            sendEmailAndSms(emails, phones,
                    notification.getTitle(),
                    notification.getMessage());

            saveStaffNotifications(staffMembers, notification);

        } catch (Exception e) {
            log.error("Failed to notify staff: {}", e.getMessage(), e);
            if (actorId != null) {
                auditLogService.logSystem(
                        "Staff alert failed for actorId=" + actorId,
                        Map.of("error", e.getMessage())
                );
            }
        }
    }

    // DB PERSISTENCE HELPERS
    private void saveGuestNotifications(List<Guest> guests,
                                        NotificationRequestDto notification) {

        List<Notification> savedNotifications = new ArrayList<>();

        for (Guest guest : guests) {

            Notification n = Notification.builder()
                    .guest(guest)
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .severity(notification.getSeverity())
                    .smsStatus(DeliveryStatus.PENDING)
                    .emailStatus(DeliveryStatus.PENDING)
                    .smsRetryCount(0)
                    .build();

            savedNotifications.add(notificationRepository.save(n));
        }

        sendEmailAndSmsWithStatusTracking(savedNotifications, notification.getMessage());
    }

    private void saveStaffNotifications(List<Staff> staffMembers,
                                        NotificationRequestDto notification) {

        List<Notification> savedNotifications = new ArrayList<>();

        for (Staff staff : staffMembers) {

            Notification n = Notification.builder()
                    .staff(staff)
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .severity(notification.getSeverity())
                    .smsStatus(DeliveryStatus.PENDING)
                    .emailStatus(DeliveryStatus.PENDING)
                    .smsRetryCount(0)
                    .build();

            savedNotifications.add(notificationRepository.save(n));
        }

        sendEmailAndSmsWithStatusTracking(savedNotifications, notification.getMessage());
    }

    // FETCH ALL ALERTS
    @Override
    public ResponseEntity<List<NotificationResponseDto>> getAllAlerts() {
        List<NotificationResponseDto> notifications = notificationRepository.findAll()
                .stream()
                .map(this::mapNotificationsToDto)
                .toList();

        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    private boolean sendSmsWithRetry(String phone, String message, int maxRetries) {
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                smsService.sendSMS(phone, message);
                return true; // SUCCESS
            } catch (Exception ex) {
                attempt++;
                log.warn("SMS attempt {} failed for {}: {}", attempt, phone, ex.getMessage());

                try {
                    Thread.sleep(2000L * attempt); // BACKOFF
                } catch (InterruptedException ignored) {}
            }
        }
        return false; // FAILED AFTER RETRIES
    }

    private void sendEmailAndSmsWithStatusTracking(
            List<Notification> dbNotifications,
            String message) {

        for (Notification notification : dbNotifications) {

            // ===== SMS DELIVERY WITH RETRY =====
            String phone = notification.getGuest() != null
                    ? notification.getGuest().getPhoneNumber()
                    : notification.getStaff().getPhoneNumber();

            boolean smsSent = sendSmsWithRetry(phone, message, 3);

            notification.setSmsRetryCount(notification.getSmsRetryCount() + 1);

            notification.setSmsStatus(
                    smsSent ? DeliveryStatus.SENT : DeliveryStatus.FAILED
            );

            notificationRepository.save(notification);
        }
    }

    // DTO MAPPERS
    private NotificationResponseDto mapNotificationsToDto(Notification notification) {

        List<GuestResponseDTO> guests = null;
        List<StaffResponseDTO> staffs = null;

        if (notification.getGuest() != null) {
            Guest guest = guestRepository.findById(notification.getGuest().getId()).orElse(null);
            if (guest != null) {
                guests = mapGuestToDto(guest);
            }
        }

        if (notification.getStaff() != null) {
            Staff staff = staffRepository.findById(notification.getStaff().getId()).orElse(null);
            if (staff != null) {
                staffs = mapStaffToDto(staff);
            }
        }

        return NotificationResponseDto.builder()
                .title(notification.getTitle())
                .message(notification.getMessage())
                .severity(notification.getSeverity())
                .guests(guests)
                .staffs(staffs)
                .build();
    }

    private List<GuestResponseDTO> mapGuestToDto(Guest guest) {
        return List.of(GuestResponseDTO.builder()
                .phoneNumber(guest.getPhoneNumber())
                .fullName(guest.getFullName())
                .email(guest.getEmail())
                .gender(guest.getGender())
                .build());
    }

    private List<StaffResponseDTO> mapStaffToDto(Staff staff) {
        return List.of(StaffResponseDTO.builder()
                .gender(staff.getGender())
                .phoneNumber(staff.getPhoneNumber())
                .email(staff.getEmail())
                .fullName(staff.getFullName())
                .build());
    }
}
