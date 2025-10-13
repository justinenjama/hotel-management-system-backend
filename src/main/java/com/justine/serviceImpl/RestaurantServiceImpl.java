package com.justine.serviceImpl;

import com.justine.dtos.request.*;
import com.justine.dtos.response.*;
import com.justine.enums.OrderStatus;
import com.justine.model.*;
import com.justine.repository.*;
import com.justine.service.AuditLogService;
import com.justine.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantServiceImpl implements RestaurantService {

    private final GuestRepository guestRepository;
    private final FoodItemRepository foodItemRepository;
    private final RestaurantOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuditLogService auditLogService;

    // Utility: check admin
    private boolean isAdmin(String email) {
        Optional<Guest> user = guestRepository.findByEmail(email);
        return user.isPresent() && "ADMIN".equalsIgnoreCase(user.get().getRole());
    }

    // Utility: get current actorId
    private Long getActorId(String email) {
        return guestRepository.findByEmail(email).map(Guest::getId).orElse(null);
    }

    // ============ FOOD ITEMS ============
    @Override
    public ResponseEntity<FoodItemResponseDTO> addFoodItem(FoodItemRequestDTO dto, String currentUserEmail) {
        Long actorId = getActorId(currentUserEmail);
        try {
            if (!isAdmin(currentUserEmail)) {
                auditLogService.logRestaurant(actorId, "ADD_FOOD_ITEM_FORBIDDEN", null, Map.of(
                        "email", currentUserEmail,
                        "itemName", dto.getItemName()
                ));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            FoodItem item = FoodItem.builder()
                    .itemName(dto.getItemName())
                    .price(dto.getPrice())
                    .category(dto.getCategory())
                    .build();

            foodItemRepository.save(item);

            auditLogService.logRestaurant(actorId, "ADD_FOOD_ITEM_SUCCESS", item.getId(), Map.of(
                    "itemName", dto.getItemName(),
                    "price", dto.getPrice(),
                    "category", dto.getCategory()
            ));

            return ResponseEntity.status(HttpStatus.CREATED).body(toFoodItemResponse(item));
        } catch (Exception e) {
            auditLogService.logRestaurant(actorId, "ADD_FOOD_ITEM_ERROR", null, Map.of(
                    "error", e.getMessage()
            ));
            log.error("Error adding food item: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<FoodItemResponseDTO> updateFoodItem(Long id, FoodItemRequestDTO dto, String currentUserEmail) {
        Long actorId = getActorId(currentUserEmail);
        try {
            if (!isAdmin(currentUserEmail)) {
                auditLogService.logRestaurant(actorId, "UPDATE_FOOD_ITEM_FORBIDDEN", id, Map.of("email", currentUserEmail));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            FoodItem item = foodItemRepository.findById(id).orElseThrow();
            item.setItemName(dto.getItemName());
            item.setPrice(dto.getPrice());
            item.setCategory(dto.getCategory());
            foodItemRepository.save(item);

            auditLogService.logRestaurant(actorId, "UPDATE_FOOD_ITEM_SUCCESS", id, Map.of(
                    "itemName", dto.getItemName(),
                    "price", dto.getPrice(),
                    "category", dto.getCategory()
            ));

            return ResponseEntity.ok(toFoodItemResponse(item));
        } catch (Exception e) {
            auditLogService.logRestaurant(actorId, "UPDATE_FOOD_ITEM_ERROR", id, Map.of("error", e.getMessage()));
            log.error("Error updating food item {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Void> deleteFoodItem(Long id, String currentUserEmail) {
        Long actorId = getActorId(currentUserEmail);
        try {
            if (!isAdmin(currentUserEmail)) {
                auditLogService.logRestaurant(actorId, "DELETE_FOOD_ITEM_FORBIDDEN", id, Map.of("email", currentUserEmail));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            foodItemRepository.deleteById(id);
            auditLogService.logRestaurant(actorId, "DELETE_FOOD_ITEM_SUCCESS", id, Map.of("deletedBy", currentUserEmail));
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            auditLogService.logRestaurant(actorId, "DELETE_FOOD_ITEM_ERROR", id, Map.of("error", e.getMessage()));
            log.error("Error deleting food item {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<FoodItemResponseDTO>> getAllFoodItems() {
        List<FoodItemResponseDTO> items = foodItemRepository.findAll()
                .stream().map(this::toFoodItemResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    // ============ ORDERS ============
    @Override
    public ResponseEntity<RestaurantOrderResponseDTO> createOrder(RestaurantOrderDTO dto, String currentUserEmail) {
        Long actorId = getActorId(currentUserEmail);
        try {
            Guest guest = guestRepository.findByEmail(currentUserEmail).orElseThrow();

            RestaurantOrder order = RestaurantOrder.builder()
                    .orderDate(LocalDateTime.now())
                    .status(OrderStatus.PENDING)
                    .guest(guest)
                    .totalAmount(dto.getTotalAmount())
                    .build();

            orderRepository.save(order);

            List<OrderItem> items = dto.getOrderItems().stream().map(i -> {
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .foodItem(i.getFoodItem())
                        .quantity(i.getQuantity())
                        .build();
                return orderItemRepository.save(orderItem);
            }).collect(Collectors.toList());

            order.setOrderItems(items);
            orderRepository.save(order);

            auditLogService.logRestaurant(actorId, "CREATE_ORDER_SUCCESS", order.getId(), Map.of(
                    "guestEmail", currentUserEmail,
                    "totalAmount", dto.getTotalAmount(),
                    "itemCount", items.size()
            ));

            return ResponseEntity.status(HttpStatus.CREATED).body(toOrderResponse(order));
        } catch (Exception e) {
            auditLogService.logRestaurant(actorId, "CREATE_ORDER_ERROR", null, Map.of("error", e.getMessage()));
            log.error("Error creating restaurant order: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<RestaurantOrderResponseDTO> getOrderById(Long orderId, String currentUserEmail) {
        Long actorId = getActorId(currentUserEmail);
        try {
            RestaurantOrder order = orderRepository.findById(orderId).orElseThrow();

            if (!isAdmin(currentUserEmail) && !order.getGuest().getEmail().equals(currentUserEmail)) {
                auditLogService.logRestaurant(actorId, "GET_ORDER_FORBIDDEN", orderId, Map.of("email", currentUserEmail));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            auditLogService.logRestaurant(actorId, "GET_ORDER_SUCCESS", orderId, Map.of("guestEmail", currentUserEmail));
            return ResponseEntity.ok(toOrderResponse(order));
        } catch (Exception e) {
            auditLogService.logRestaurant(actorId, "GET_ORDER_ERROR", orderId, Map.of("error", e.getMessage()));
            log.error("Error retrieving order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<RestaurantOrderResponseDTO>> getOrdersByGuest(Long guestId, String currentUserEmail) {
        Long actorId = getActorId(currentUserEmail);
        try {
            Guest guest = guestRepository.findById(guestId).orElseThrow();
            if (!isAdmin(currentUserEmail) && !guest.getEmail().equals(currentUserEmail)) {
                auditLogService.logRestaurant(actorId, "GET_GUEST_ORDERS_FORBIDDEN", guestId, Map.of("email", currentUserEmail));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<RestaurantOrderResponseDTO> orders = orderRepository.findByGuestId(guestId)
                    .stream().map(this::toOrderResponse)
                    .collect(Collectors.toList());

            auditLogService.logRestaurant(actorId, "GET_GUEST_ORDERS_SUCCESS", guestId, Map.of(
                    "orderCount", orders.size()
            ));

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            auditLogService.logRestaurant(actorId, "GET_GUEST_ORDERS_ERROR", guestId, Map.of("error", e.getMessage()));
            log.error("Error retrieving guest orders {}: {}", guestId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ============ MAPPERS ============
    private FoodItemResponseDTO toFoodItemResponse(FoodItem item) {
        return FoodItemResponseDTO.builder()
                .id(item.getId())
                .itemName(item.getItemName())
                .price(item.getPrice())
                .category(item.getCategory())
                .build();
    }

    private OrderItemResponseDTO toOrderItemResponse(OrderItem item) {
        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .quantity(item.getQuantity())
                .foodItem(toFoodItemResponse(item.getFoodItem()))
                .build();
    }

    private RestaurantOrderResponseDTO toOrderResponse(RestaurantOrder order) {
        return RestaurantOrderResponseDTO.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .guest(toGuestResponse(order.getGuest()))
                .orderItems(order.getOrderItems().stream()
                        .map(this::toOrderItemResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private GuestResponseDTO toGuestResponse(Guest guest) {
        return GuestResponseDTO.builder()
                .id(guest.getId())
                .fullName(guest.getFullName())
                .email(guest.getEmail())
                .phoneNumber(guest.getPhoneNumber())
                .idNumber(guest.getIdNumber())
                .role(guest.getRole())
                .gender(guest.getGender())
                .build();
    }
}
