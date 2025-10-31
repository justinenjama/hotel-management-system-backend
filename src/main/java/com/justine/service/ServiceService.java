package com.justine.service;

import com.justine.dtos.request.ServiceRequestDTO;
import com.justine.dtos.response.ServiceResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ServiceService {

    ResponseEntity<ServiceResponseDTO> addService(ServiceRequestDTO dto, Long currentUserId);
    ResponseEntity<ServiceResponseDTO> updateService(Long serviceId, ServiceRequestDTO dto, Long currentUserId);
    ResponseEntity<Void> deleteService(Long serviceId, Long currentUserId);
    ResponseEntity<List<ServiceResponseDTO>> getAllServices();
    ResponseEntity<Void> addServiceToBooking(Long bookingId, Long serviceId, Long currentUserId);
    ResponseEntity<Void> removeServiceFromBooking(Long bookingId, Long serviceId, Long currentUserId);
}
