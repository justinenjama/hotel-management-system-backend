package com.justine.dtos.response;

import com.justine.enums.BookingStatus;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDTO {
    private Long id;
    private String bookingCode;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int numberOfGuests;
    private BookingStatus status;
    private GuestResponseDTO guest;
    private RoomResponseDTO room;
    private PaymentResponseDTO payment;
    private InvoiceResponseDTO invoice;
    private List<ServiceResponseDTO> services;
    private boolean invoiceExists;
}
