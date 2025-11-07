package com.justine.repository;

import com.justine.enums.ServiceType;
import com.justine.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    @Query("SELECT s FROM Service s JOIN s.bookings b WHERE b.id = :bookingId")
    List<Service> findAllByBookingId(@Param("bookingId") Long bookingId);

    List<Service> findByHotelId(Long hotelId);

    boolean existsByHotelIdAndServiceType(Long hotelId, ServiceType serviceType);

}
