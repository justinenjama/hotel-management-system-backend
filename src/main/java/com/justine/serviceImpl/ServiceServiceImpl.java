package com.justine.serviceImpl;

import com.justine.dtos.request.ServiceRequestDTO;
import com.justine.dtos.response.ServiceResponseDTO;
import com.justine.model.Booking;
import com.justine.model.Guest;
import com.justine.model.Service;
import com.justine.repository.BookingRepository;
import com.justine.repository.GuestRepository;
import com.justine.repository.ServiceRepository;
import com.justine.service.AuditLogService;
import com.justine.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;
    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService;

    // Utility: Check if user is admin
    private boolean isAdmin(String email) {
        Optional<Guest> user = guestRepository.findByEmail(email);
        return user.isPresent() && "ADMIN".equalsIgnoreCase(user.get().getRole());
    }

    // ============ ADD SERVICE ============
    @Override
    public ResponseEntity<ServiceResponseDTO> addService(ServiceRequestDTO dto, String currentUserEmail) {
        try {
            if (!isAdmin(currentUserEmail)) {
                auditLogService.logService(
                        null, "UNAUTHORIZED_ADD_SERVICE", null,
                        Map.of("user", currentUserEmail, "reason", "Not an admin")
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Service service = Service.builder()
                    .serviceType(dto.getServiceType())
                    .price(dto.getPrice())
                    .build();

            serviceRepository.save(service);

            auditLogService.logService(
                    null, "ADD_SERVICE_SUCCESS", service.getId(),
                    Map.of("user", currentUserEmail, "serviceType", dto.getServiceType())
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(toServiceResponse(service));

        } catch (Exception e) {
            auditLogService.logService(
                    null, "ADD_SERVICE_FAILED", null,
                    Map.of("user", currentUserEmail, "error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ UPDATE SERVICE ============
    @Override
    public ResponseEntity<ServiceResponseDTO> updateService(Long serviceId, ServiceRequestDTO dto, String currentUserEmail) {
        try {
            if (!isAdmin(currentUserEmail)) {
                auditLogService.logService(
                        null, "UNAUTHORIZED_UPDATE_SERVICE", serviceId,
                        Map.of("user", currentUserEmail)
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new NoSuchElementException("Service not found"));

            service.setServiceType(dto.getServiceType());
            service.setPrice(dto.getPrice());
            serviceRepository.save(service);

            auditLogService.logService(
                    null, "UPDATE_SERVICE_SUCCESS", serviceId,
                    Map.of("user", currentUserEmail, "newType", dto.getServiceType())
            );

            return ResponseEntity.ok(toServiceResponse(service));

        } catch (NoSuchElementException e) {
            auditLogService.logService(
                    null, "UPDATE_SERVICE_NOT_FOUND", serviceId,
                    Map.of("user", currentUserEmail, "error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (Exception e) {
            auditLogService.logService(
                    null, "UPDATE_SERVICE_FAILED", serviceId,
                    Map.of("user", currentUserEmail, "error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ DELETE SERVICE ============
    @Override
    public ResponseEntity<Void> deleteService(Long serviceId, String currentUserEmail) {
        try {
            if (!isAdmin(currentUserEmail)) {
                auditLogService.logService(
                        null, "UNAUTHORIZED_DELETE_SERVICE", serviceId,
                        Map.of("user", currentUserEmail)
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            serviceRepository.deleteById(serviceId);

            auditLogService.logService(
                    null, "DELETE_SERVICE_SUCCESS", serviceId,
                    Map.of("user", currentUserEmail)
            );

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            auditLogService.logService(
                    null, "DELETE_SERVICE_FAILED", serviceId,
                    Map.of("user", currentUserEmail, "error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ GET ALL SERVICES ============
    @Override
    public ResponseEntity<List<ServiceResponseDTO>> getAllServices() {
        try {
            List<ServiceResponseDTO> services = serviceRepository.findAll()
                    .stream()
                    .map(this::toServiceResponse)
                    .collect(Collectors.toList());

            auditLogService.logService(
                    null, "FETCH_SERVICES_SUCCESS", null,
                    Map.of("count", services.size())
            );

            return ResponseEntity.ok(services);

        } catch (Exception e) {
            auditLogService.logService(
                    null, "FETCH_SERVICES_FAILED", null,
                    Map.of("error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ ADD SERVICE TO BOOKING ============
    @Override
    public ResponseEntity<Void> addServiceToBooking(Long bookingId, Long serviceId, String currentUserEmail) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new NoSuchElementException("Booking not found"));

            if (!isAdmin(currentUserEmail) && !booking.getGuest().getEmail().equals(currentUserEmail)) {
                auditLogService.logService(
                        null, "UNAUTHORIZED_ADD_SERVICE_TO_BOOKING", bookingId,
                        Map.of("user", currentUserEmail)
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new NoSuchElementException("Service not found"));

            booking.getServices().add(service);
            bookingRepository.save(booking);

            auditLogService.logService(
                    null, "ADD_SERVICE_TO_BOOKING_SUCCESS", serviceId,
                    Map.of("bookingId", bookingId, "user", currentUserEmail)
            );

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            auditLogService.logService(
                    null, "ADD_SERVICE_TO_BOOKING_FAILED", serviceId,
                    Map.of("bookingId", bookingId, "user", currentUserEmail, "error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ REMOVE SERVICE FROM BOOKING ============
    @Override
    public ResponseEntity<Void> removeServiceFromBooking(Long bookingId, Long serviceId, String currentUserEmail) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new NoSuchElementException("Booking not found"));

            if (!isAdmin(currentUserEmail) && !booking.getGuest().getEmail().equals(currentUserEmail)) {
                auditLogService.logService(
                        null, "UNAUTHORIZED_REMOVE_SERVICE_FROM_BOOKING", bookingId,
                        Map.of("user", currentUserEmail)
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new NoSuchElementException("Service not found"));

            booking.getServices().remove(service);
            bookingRepository.save(booking);

            auditLogService.logService(
                    null, "REMOVE_SERVICE_FROM_BOOKING_SUCCESS", serviceId,
                    Map.of("bookingId", bookingId, "user", currentUserEmail)
            );

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            auditLogService.logService(
                    null, "REMOVE_SERVICE_FROM_BOOKING_FAILED", serviceId,
                    Map.of("bookingId", bookingId, "user", currentUserEmail, "error", e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ MAPPER ============
    private ServiceResponseDTO toServiceResponse(Service service) {
        return ServiceResponseDTO.builder()
                .id(service.getId())
                .serviceType(service.getServiceType())
                .price(service.getPrice())
                .build();
    }
}
