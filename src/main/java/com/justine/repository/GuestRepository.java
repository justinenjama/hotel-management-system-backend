package com.justine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.justine.model.Guest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    Optional<Guest> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    // GuestRepository.java
    @Query("SELECT g FROM Guest g LEFT JOIN FETCH g.bookings WHERE g.id = :id")
    Optional<Guest> findByIdWithBookings(@Param("id") Long id);

}
