package com.justine.serviceImpl;

import com.justine.dtos.request.*;
import com.justine.dtos.response.*;
import com.justine.enums.OrderStatus;
import com.justine.model.*;
import com.justine.repository.*;
import com.justine.service.AuditLogService;
import com.justine.service.RestaurantService;
import com.justine.utils.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final GuestRepository guestRepository;
    private final FoodItemRepository foodItemRepository;
    private final RestaurantOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuditLogService auditLogService;
    private final StaffRepository staffRepository;
    private final HotelRepository hotelRepository;
    private final CloudinaryService cloudinaryService;

    // Utility: check admin by user ID
    private boolean isAdmin(Long currentUserId) {
        return staffRepository.findById(currentUserId)
                .map(staff -> "ADMIN".equalsIgnoreCase(String.valueOf(staff.getRole())))
                .orElse(false);
    }

    // ============ FOOD ITEMS ============
    @Override
    public ResponseEntity<FoodItemResponseDTO> addFoodItem(FoodItemRequestDTO dto, MultipartFile imageFile, Long currentUserId) {
        try {
            if (!isAdmin(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Hotel hotel = hotelRepository.findById(dto.getHotelId())
                    .orElseThrow(() -> new RuntimeException("Hotel not found"));

            String imageUrl = null;
            if (imageFile != null && !imageFile.isEmpty()) {
                Map<String, String> uploadResult = cloudinaryService.uploadFileWithEagerSizes(imageFile, "food_items");
                imageUrl = uploadResult.get("large"); // store the large version
            }

            FoodItem item = FoodItem.builder()
                    .itemName(dto.getItemName())
                    .price(dto.getPrice())
                    .category(dto.getCategory())
                    .hotel(hotel)
                    .imageUrl(imageUrl)
                    .build();
            foodItemRepository.save(item);

            return ResponseEntity.status(HttpStatus.CREATED).body(toFoodItemResponse(item));
        } catch (Exception e) {
            log.error("Error adding food item: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<FoodItemResponseDTO> updateFoodItem(Long id, FoodItemRequestDTO dto, MultipartFile imageFile, Long currentUserId) {
        try {
            if (!isAdmin(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            FoodItem item = foodItemRepository.findById(id).orElseThrow(() -> new RuntimeException("Food item not found"));
            Hotel hotel = hotelRepository.findById(dto.getHotelId()).orElseThrow();

            // Delete old image if new image is provided
            if (imageFile != null && !imageFile.isEmpty() && item.getImageUrl() != null) {
                String publicId = cloudinaryService.extractPublicIdFromUrl(item.getImageUrl());
                cloudinaryService.deleteFile(publicId);
            }

            String imageUrl = item.getImageUrl();
            if (imageFile != null && !imageFile.isEmpty()) {
                Map<String, String> uploadResult = cloudinaryService.uploadFileWithEagerSizes(imageFile, "food_items");
                imageUrl = uploadResult.get("large");
            }

            item.setItemName(dto.getItemName());
            item.setPrice(dto.getPrice());
            item.setCategory(dto.getCategory());
            item.setHotel(hotel);
            item.setImageUrl(imageUrl);

            foodItemRepository.save(item);
            return ResponseEntity.ok(toFoodItemResponse(item));
        } catch (Exception e) {
            log.error("Error updating food item {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Void> deleteFoodItem(Long id, Long currentUserId) {
        try {
            if (!isAdmin(currentUserId)) {
                auditLogService.logRestaurant(currentUserId, "DELETE_FOOD_ITEM_FORBIDDEN", id, Map.of());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            foodItemRepository.deleteById(id);
            auditLogService.logRestaurant(currentUserId, "DELETE_FOOD_ITEM_SUCCESS", id, Map.of());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            auditLogService.logRestaurant(currentUserId, "DELETE_FOOD_ITEM_ERROR", id, Map.of("error", e.getMessage()));
            log.error("Error deleting food item {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<FoodItemResponseDTO>> getAllFoodItems() {
        List<FoodItemResponseDTO> items = foodItemRepository.findAll().stream()
                .map(this::toFoodItemResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @Override
    public ResponseEntity<List<FoodItemResponseDTO>> getFoodItemsByHotel(Long hotelId) {
        try {
            List<FoodItemResponseDTO> items = foodItemRepository.findByHotelId(hotelId).stream()
                    .map(this::toFoodItemResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("Error fetching food items for hotel {}: {}", hotelId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ============ ORDERS ============
    @Override
    public ResponseEntity<RestaurantOrderResponseDTO> createOrder(RestaurantOrderDTO dto, Long currentUserId) {
        try {
            Guest guest = guestRepository.findById(currentUserId).orElseThrow();
            Hotel hotel = hotelRepository.findById(dto.getHotelId())
                    .orElseThrow(() -> new RuntimeException("Hotel not found"));

            RestaurantOrder order = RestaurantOrder.builder()
                    .orderDate(LocalDateTime.now())
                    .status(OrderStatus.PENDING)
                    .guest(guest)
                    .hotel(hotel)
                    .totalAmount(dto.getTotalAmount())
                    .build();
            orderRepository.save(order);

            List<OrderItem> items = dto.getOrderItems().stream().map(i -> {
                FoodItem foodItem = foodItemRepository.findById(i.getFoodItemId())
                        .orElseThrow(() -> new RuntimeException("Food item not found"));

                if (!foodItem.getHotel().getId().equals(hotel.getId())) {
                    throw new RuntimeException("Food item does not belong to selected hotel");
                }

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .foodItem(foodItem)
                        .quantity(i.getQuantity())
                        .build();
                return orderItemRepository.save(orderItem);
            }).collect(Collectors.toList());

            order.setOrderItems(items);
            orderRepository.save(order);

            auditLogService.logRestaurant(currentUserId, "CREATE_ORDER_SUCCESS", order.getId(), Map.of(
                    "totalAmount", dto.getTotalAmount(),
                    "itemCount", items.size(),
                    "hotelId", hotel.getId()
            ));
            return ResponseEntity.status(HttpStatus.CREATED).body(toOrderResponse(order));
        } catch (Exception e) {
            auditLogService.logRestaurant(currentUserId, "CREATE_ORDER_ERROR", null, Map.of("error", e.getMessage()));
            log.error("Error creating order: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<RestaurantOrderResponseDTO> getOrderById(Long orderId, Long currentUserId) {
        try {
            RestaurantOrder order = orderRepository.findById(orderId).orElseThrow();
            if (!isAdmin(currentUserId) && !order.getGuest().getId().equals(currentUserId)) {
                auditLogService.logRestaurant(currentUserId, "GET_ORDER_FORBIDDEN", orderId, Map.of());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            auditLogService.logRestaurant(currentUserId, "GET_ORDER_SUCCESS", orderId, Map.of());
            return ResponseEntity.ok(toOrderResponse(order));
        } catch (Exception e) {
            auditLogService.logRestaurant(currentUserId, "GET_ORDER_ERROR", orderId, Map.of("error", e.getMessage()));
            log.error("Error retrieving order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<RestaurantOrderResponseDTO>> getOrdersByGuest(Long guestId, Long currentUserId) {
        try {
            if (!isAdmin(currentUserId) && !guestId.equals(currentUserId)) {
                auditLogService.logRestaurant(currentUserId, "GET_GUEST_ORDERS_FORBIDDEN", guestId, Map.of());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<RestaurantOrderResponseDTO> orders = orderRepository.findByGuestId(guestId)
                    .stream().map(this::toOrderResponse)
                    .collect(Collectors.toList());

            auditLogService.logRestaurant(currentUserId, "GET_GUEST_ORDERS_SUCCESS", guestId, Map.of(
                    "orderCount", orders.size()
            ));
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            auditLogService.logRestaurant(currentUserId, "GET_GUEST_ORDERS_ERROR", guestId, Map.of("error", e.getMessage()));
            log.error("Error retrieving guest orders {}: {}", guestId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<RestaurantOrderResponseDTO> cancelOrder(Long orderId, Long currentUserId) {
        try {
            RestaurantOrder order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Permission: Only admin or the guest who owns it
            if (!isAdmin(currentUserId) && !order.getGuest().getId().equals(currentUserId)) {
                auditLogService.logRestaurant(currentUserId, "CANCEL_ORDER_FORBIDDEN", orderId, Map.of());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Cannot cancel delivered orders
            if (order.getStatus() == OrderStatus.DELIVERED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            // Update status
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            auditLogService.logRestaurant(currentUserId, "CANCEL_ORDER_SUCCESS", orderId, Map.of());
            return ResponseEntity.ok(toOrderResponse(order));

        } catch (Exception e) {
            auditLogService.logRestaurant(currentUserId, "CANCEL_ORDER_ERROR", orderId, Map.of("error", e.getMessage()));
            log.error("Error cancelling order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<List<RestaurantOrderResponseDTO>> getAllOrders(Long currentUserId) {
        try {
            if (!isAdmin(currentUserId)) {
                auditLogService.logRestaurant(currentUserId, "GET_ALL_ORDERS_FORBIDDEN", null, Map.of());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<RestaurantOrderResponseDTO> orders = orderRepository.findAll()
                    .stream()
                    .map(this::toOrderResponse)
                    .collect(Collectors.toList());

            if (orders.isEmpty()){
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            }

            auditLogService.logRestaurant(currentUserId, "GET_ALL_ORDERS_SUCCESS", null, Map.of("orderCount", orders.size()));
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            auditLogService.logRestaurant(currentUserId, "GET_ALL_ORDERS_ERROR", null, Map.of("error", e.getMessage()));
            log.error("Error retrieving all orders: {}", e.getMessage());
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
                .imageUrl(item.getImageUrl())
                .hotelId(item.getHotel() != null ? item.getHotel().getId() : null)
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
                .hotelId(order.getHotel() != null ? order.getHotel().getId() : null)
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
