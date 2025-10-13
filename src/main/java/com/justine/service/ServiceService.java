package com.justine.service;

import com.justine.dtos.request.ServiceRequestDTO;
import com.justine.dtos.response.ServiceResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ServiceService {

    // --- SERVICES CRUD ---
    ResponseEntity<ServiceResponseDTO> addService(ServiceRequestDTO dto, String currentUserEmail);
    ResponseEntity<ServiceResponseDTO> updateService(Long serviceId, ServiceRequestDTO dto, String currentUserEmail);
    ResponseEntity<Void> deleteService(Long serviceId, String currentUserEmail);
    ResponseEntity<List<ServiceResponseDTO>> getAllServices();

    // --- LINKING SERVICES TO BOOKINGS ---
    ResponseEntity<Void> addServiceToBooking(Long bookingId, Long serviceId, String currentUserEmail);
    ResponseEntity<Void> removeServiceFromBooking(Long bookingId, Long serviceId, String currentUserEmail);
}
