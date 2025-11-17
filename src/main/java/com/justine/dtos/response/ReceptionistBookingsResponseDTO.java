package com.justine.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionistBookingsResponseDTO {
    private List<BookingResponseDTO> bookings;
    private Map<String, Object> contributions;  // e.g., {"totalBookings": 5, "totalRevenue": 15000.0, "totalUnpaidRevenue": 2000.0}
}