package com.justine.controller;

import com.justine.dtos.request.ServiceRequestDTO;
import com.justine.dtos.response.ServiceResponseDTO;
import com.justine.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/services")
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @PostMapping
    public ResponseEntity<ServiceResponseDTO> addService(@Valid @RequestBody ServiceRequestDTO dto, Principal principal) {
        return serviceService.addService(dto, Long.valueOf(principal.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponseDTO> updateService(@PathVariable Long id, @RequestBody ServiceRequestDTO dto, Principal principal) {
        return serviceService.updateService(id, dto, Long.valueOf(principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id, Principal principal) {
        return serviceService.deleteService(id, Long.valueOf(principal.getName()));
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponseDTO>> getAllServices() {
        return serviceService.getAllServices();
    }

    @PostMapping("/{serviceId}/bookings/{bookingId}")
    public ResponseEntity<Void> addServiceToBooking(@PathVariable Long serviceId, @PathVariable Long bookingId, Principal principal) {
        return serviceService.addServiceToBooking(bookingId, serviceId, Long.valueOf(principal.getName()));
    }

    @DeleteMapping("/{serviceId}/bookings/{bookingId}")
    public ResponseEntity<Void> removeServiceFromBooking(@PathVariable Long serviceId, @PathVariable Long bookingId, Principal principal) {
        return serviceService.removeServiceFromBooking(bookingId, serviceId, Long.valueOf(principal.getName()));
    }
}
