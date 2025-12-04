package com.justine.repository;

import java.util.List;
import java.util.Optional;

import com.justine.enums.StaffRole;
import com.justine.model.Staff;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    boolean existsByEmail(String email);

    List<Staff> findByRole(StaffRole staffRole);

    @Query("SELECT s FROM Staff s LEFT JOIN FETCH s.hotel WHERE s.id = :id")
    Optional<Staff> findByIdWithHotel(@Param("id") Long id);

    @Query("SELECT s FROM Staff s WHERE s.email = :email")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<Staff> findByEmail(String email);

    @Query("SELECT s FROM Staff s WHERE s.hotel.id = :hotelId")
    List<Staff> findByHotelId(@Param("hotelId") Long hotelId);

}
