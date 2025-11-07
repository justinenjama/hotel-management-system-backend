package com.justine.service;

import com.justine.dtos.request.NotificationRequestDto;

public interface NotificationService {

    void broadcast(NotificationRequestDto notification);

    void sendToUser(String username, NotificationRequestDto notification);

    void alertActiveGuests(NotificationRequestDto notification);

    void alertHotel(Long hotelId, Long actorId, NotificationRequestDto notification);
}
