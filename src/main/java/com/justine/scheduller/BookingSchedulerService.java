package com.justine.scheduller;

import com.justine.enums.BookingStatus;
import com.justine.model.Booking;
import com.justine.model.Room;
import com.justine.repository.BookingRepository;
import com.justine.repository.RoomRepository;
import com.justine.service.AuditLogService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BookingSchedulerService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final AuditLogService auditLogService;

    public BookingSchedulerService(BookingRepository bookingRepository,
                                   RoomRepository roomRepository,
                                   AuditLogService auditLogService) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.auditLogService = auditLogService;
    }

    // ✅ Runs both at startup and every midnight
    @PostConstruct
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoReleaseRoomsAfterCheckout() {
        try {
            LocalDate today = LocalDate.now();

            // Only fetch bookings where end date <= today & not yet checked out
            List<Booking> bookings = bookingRepository
                    .findByCheckOutDateBeforeOrCheckOutDateEqualsAndStatusNot(
                            today, today, BookingStatus.CHECKED_OUT
                    );

            bookings.forEach(booking -> {
                Room room = booking.getRoom();

                // Mark booking as CHECKED_OUT
                if (booking.getStatus() != BookingStatus.CHECKED_OUT) {
                    booking.setStatus(BookingStatus.CHECKED_OUT);
                    bookingRepository.save(booking);

                    auditLogService.logBooking(
                            booking.getGuest() != null ? booking.getGuest().getId() : null,
                            "AUTO_BOOKING_CHECKOUT_SUCCESS",
                            booking.getId(),
                            Map.of("status", BookingStatus.CHECKED_OUT.name())
                    );
                }

                // Release room if still occupied
                if (room != null && !room.isAvailable()) {
                    room.setAvailable(true);
                    roomRepository.save(room);

                    auditLogService.logBooking(
                            booking.getGuest() != null ? booking.getGuest().getId() : null,
                            "AUTO_RELEASE_ROOM_SUCCESS",
                            booking.getId(),
                            Map.of("roomId", room.getId(), "checkOutDate", booking.getCheckOutDate())
                    );
                }
            });

            log.info("✔ Auto checkout + room release completed successfully");
        } catch (Exception e) {
            log.error("❌ Auto checkout error: {}", e.getMessage());
            auditLogService.logBooking(null, "AUTO_CHECKOUT_ERROR", null,
                    Map.of("error", e.getMessage()));
        }
    }
}
