package com.justine.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.justine.model.Guest;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    Optional<Guest> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
