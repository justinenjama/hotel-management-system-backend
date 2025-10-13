package com.justine.dtos.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestResponseDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String idNumber; 
    private String gender;
    private String role;
    private List<BookingResponseDTO> bookings;
    private List<RestaurantOrderResponseDTO> restaurantOrders;
}