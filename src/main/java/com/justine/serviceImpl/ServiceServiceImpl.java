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
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;
    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService;

    public ServiceServiceImpl(ServiceRepository serviceRepository, GuestRepository guestRepository, BookingRepository bookingRepository, AuditLogService auditLogService) {
        this.serviceRepository = serviceRepository;
        this.guestRepository = guestRepository;
        this.bookingRepository = bookingRepository;
        this.auditLogService = auditLogService;
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ============ ADD SERVICE ============
    @Override
    public ResponseEntity<ServiceResponseDTO> addService(ServiceRequestDTO dto, Long currentUserId) {
        try {
            if (!isAdmin()) {
                auditLogService.logService(null, "UNAUTHORIZED_ADD_SERVICE", null,
                        Map.of("userId", currentUserId, "reason", "Not an admin"));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Service service = Service.builder()
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .serviceType(dto.getServiceType())
                    .price(dto.getPrice())
                    .build();

            serviceRepository.save(service);

            auditLogService.logService(null, "ADD_SERVICE_SUCCESS", service.getId(),
                    Map.of("userId", currentUserId, "serviceType", dto.getServiceType()));

            return ResponseEntity.status(HttpStatus.CREATED).body(toServiceResponse(service));

        } catch (Exception e) {
            auditLogService.logService(null, "ADD_SERVICE_FAILED", null,
                    Map.of("userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ UPDATE SERVICE ============
    @Override
    public ResponseEntity<ServiceResponseDTO> updateService(Long serviceId, ServiceRequestDTO dto, Long currentUserId) {
        try {
            if (!isAdmin()) {
                auditLogService.logService(null, "UNAUTHORIZED_UPDATE_SERVICE", serviceId,
                        Map.of("userId", currentUserId));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new NoSuchElementException("Service not found"));

            service.setServiceType(dto.getServiceType());
            service.setPrice(dto.getPrice());
            service.setName(dto.getName());
            service.setDescription(dto.getDescription());
            serviceRepository.save(service);

            auditLogService.logService(null, "UPDATE_SERVICE_SUCCESS", serviceId,
                    Map.of("userId", currentUserId, "newType", dto.getServiceType()));

            return ResponseEntity.ok(toServiceResponse(service));

        } catch (NoSuchElementException e) {
            auditLogService.logService(null, "UPDATE_SERVICE_NOT_FOUND", serviceId,
                    Map.of("userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (Exception e) {
            auditLogService.logService(null, "UPDATE_SERVICE_FAILED", serviceId,
                    Map.of("userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ DELETE SERVICE ============
    @Override
    public ResponseEntity<Void> deleteService(Long serviceId, Long currentUserId) {
        try {
            if (!isAdmin()) {
                auditLogService.logService(null, "UNAUTHORIZED_DELETE_SERVICE", serviceId,
                        Map.of("userId", currentUserId));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            serviceRepository.deleteById(serviceId);

            auditLogService.logService(null, "DELETE_SERVICE_SUCCESS", serviceId,
                    Map.of("userId", currentUserId));

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            auditLogService.logService(null, "DELETE_SERVICE_FAILED", serviceId,
                    Map.of("userId", currentUserId, "error", e.getMessage()));
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

            auditLogService.logService(null, "FETCH_SERVICES_SUCCESS", null,
                    Map.of("count", services.size()));

            return ResponseEntity.ok(services);

        } catch (Exception e) {
            auditLogService.logService(null, "FETCH_SERVICES_FAILED", null,
                    Map.of("error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ ADD SERVICE TO BOOKING ============
    @Override
    public ResponseEntity<Void> addServiceToBooking(Long bookingId, Long serviceId, Long currentUserId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new NoSuchElementException("Booking not found"));

            if (!isAdmin() && !booking.getGuest().getId().equals(currentUserId)) {
                auditLogService.logService(null, "UNAUTHORIZED_ADD_SERVICE_TO_BOOKING", bookingId,
                        Map.of("userId", currentUserId));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new NoSuchElementException("Service not found"));

            booking.getServices().add(service);
            bookingRepository.save(booking);

            auditLogService.logService(null, "ADD_SERVICE_TO_BOOKING_SUCCESS", serviceId,
                    Map.of("bookingId", bookingId, "userId", currentUserId));

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            auditLogService.logService(null, "ADD_SERVICE_TO_BOOKING_FAILED", serviceId,
                    Map.of("bookingId", bookingId, "userId", currentUserId, "error", e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============ REMOVE SERVICE FROM BOOKING ============
    @Override
    public ResponseEntity<Void> removeServiceFromBooking(Long bookingId, Long serviceId, Long currentUserId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new NoSuchElementException("Booking not found"));

            if (!isAdmin() && !booking.getGuest().getId().equals(currentUserId)) {
                auditLogService.logService(null, "UNAUTHORIZED_REMOVE_SERVICE_FROM_BOOKING", bookingId,
                        Map.of("userId", currentUserId));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new NoSuchElementException("Service not found"));

            booking.getServices().remove(service);
            bookingRepository.save(booking);

            auditLogService.logService(null, "REMOVE_SERVICE_FROM_BOOKING_SUCCESS", serviceId,
                    Map.of("bookingId", bookingId, "userId", currentUserId));

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            auditLogService.logService(null, "REMOVE_SERVICE_FROM_BOOKING_FAILED", serviceId,
                    Map.of("bookingId", bookingId, "userId", currentUserId, "error", e.getMessage()));
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
