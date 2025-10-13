package com.justine.dtos.request;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int numberOfGuests;
    private Long guestId;
    private Long roomId;
    private List<Long> serviceIds;
}
