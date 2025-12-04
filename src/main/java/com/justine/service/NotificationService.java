package com.justine.service;

import com.justine.dtos.request.NotificationRequestDto;
import com.justine.dtos.response.NotificationResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface NotificationService {

    void broadcast(NotificationRequestDto notification);

    void sendToUser(String username, NotificationRequestDto notification);

    void alertActiveGuests(NotificationRequestDto notification);

    void alertHotel(Long hotelId, Long actorId, NotificationRequestDto notification);

    ResponseEntity<List<NotificationResponseDto>> getAllAlerts();
}
