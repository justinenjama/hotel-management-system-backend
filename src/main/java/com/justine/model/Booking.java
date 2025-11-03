package com.justine.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import com.justine.enums.BookingStatus;

@Entity
@Table(indexes = {
        @Index(name = "idx_booking_guest_id", columnList = "guest_id"),
        @Index(name = "idx_booking_room_id", columnList = "room_id"),
        @Index(name = "idx_booking_checkin_checkout", columnList = "checkInDate, checkOutDate"),
        @Index(name = "idx_booking_status", columnList = "status")
})

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bookingCode;
    private java.time.LocalDate checkInDate;
    private java.time.LocalDate checkOutDate;
    private int numberOfGuests;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @ManyToOne
    @JoinColumn(name = "guest_id")
    private Guest guest;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private Invoice invoice;

    @ManyToMany
    @JoinTable(
            name = "booking_services",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<Service> services;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RestaurantOrder> orders = new ArrayList<>();

}
