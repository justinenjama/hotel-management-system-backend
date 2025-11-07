package com.justine.controller;

import com.justine.dtos.request.NotificationRequestDto;
import com.justine.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Broadcast notification
    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcast(@RequestBody NotificationRequestDto payload) {
        notificationService.broadcast(payload);
        return ResponseEntity.ok().build();
    }

    // Send to specific user
    @PostMapping("/user/{username}")
    public ResponseEntity<?> sendToUser(@PathVariable String username, @RequestBody NotificationRequestDto payload) {
        notificationService.sendToUser(username, payload);
        return ResponseEntity.ok().build();
    }

    // Emergency alert to all active guests
    @PostMapping("/emergency-alert")
    public ResponseEntity<?> emergencyAlert(@RequestBody NotificationRequestDto payload) {
        notificationService.alertActiveGuests(payload);
        return ResponseEntity.ok().body("Emergency alert sent to all active guests!");
    }

    // Emergency alert to a specific hotel
    @PostMapping("/emergency-alert/hotel/{hotelId}")
    public ResponseEntity<?> emergencyAlertHotel(@PathVariable Long hotelId,
                                                 @RequestBody NotificationRequestDto payload,
                                                 @RequestHeader(value = "X-Actor-Id", required = false) Long actorId) {
        notificationService.alertHotel(hotelId, actorId, payload);
        return ResponseEntity.ok().body("Emergency alert sent to all active guests and staff in hotel ID " + hotelId);
    }
}
