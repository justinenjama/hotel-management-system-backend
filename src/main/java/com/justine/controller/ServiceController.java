package com.justine.controller;

import com.justine.dtos.request.ServiceRequestDTO;
import com.justine.dtos.response.ServiceResponseDTO;
import com.justine.service.ServiceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @PostMapping
    public ResponseEntity<ServiceResponseDTO> addService(@Valid @RequestBody ServiceRequestDTO dto, Authentication auth) {
        return serviceService.addService(dto, auth.getName());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponseDTO> updateService(@PathVariable Long id, @RequestBody ServiceRequestDTO dto, Authentication auth) {
        return serviceService.updateService(id, dto, auth.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id, Authentication auth) {
        return serviceService.deleteService(id, auth.getName());
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponseDTO>> getAllServices() {
        return serviceService.getAllServices();
    }

    @PostMapping("/{serviceId}/bookings/{bookingId}")
    public ResponseEntity<Void> addServiceToBooking(
            @PathVariable Long serviceId,
            @PathVariable Long bookingId,
            Authentication auth) {
        return serviceService.addServiceToBooking(bookingId, serviceId, auth.getName());
    }

    @DeleteMapping("/{serviceId}/bookings/{bookingId}")
    public ResponseEntity<Void> removeServiceFromBooking(
            @PathVariable Long serviceId,
            @PathVariable Long bookingId,
            Authentication auth) {
        return serviceService.removeServiceFromBooking(bookingId, serviceId, auth.getName());
    }
}
