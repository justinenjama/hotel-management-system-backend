package com.justine.controller;

import com.justine.dtos.request.FoodItemRequestDTO;
import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.request.RestaurantOrderDTO;
import com.justine.dtos.response.FoodItemResponseDTO;
import com.justine.dtos.response.InvoiceResponseDTO;
import com.justine.dtos.response.PaymentResponseDTO;
import com.justine.dtos.response.RestaurantOrderResponseDTO;
import com.justine.enums.OrderStatus;
import com.justine.service.RestaurantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/restaurant")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    private Long getCurrentUserId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }

    @PostMapping("/food")
    public ResponseEntity<FoodItemResponseDTO> addFoodItem(
            @RequestPart("dto") FoodItemRequestDTO dto,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            Authentication auth) {
        return restaurantService.addFoodItem(dto, imageFile, getCurrentUserId(auth));
    }

    @PutMapping("/food/{id}")
    public ResponseEntity<FoodItemResponseDTO> updateFoodItem(
            @PathVariable Long id,
            @RequestPart("dto") FoodItemRequestDTO dto,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            Authentication auth) {
        return restaurantService.updateFoodItem(id, dto, imageFile, getCurrentUserId(auth));
    }

    @DeleteMapping("/food/{id}")
    public ResponseEntity<Void> deleteFoodItem(@PathVariable Long id, Authentication auth) {
        return restaurantService.deleteFoodItem(id, getCurrentUserId(auth));
    }

    @GetMapping("/hotel/{hotelId}/food")
    public ResponseEntity<List<FoodItemResponseDTO>> getFoodItemsByHotel(@PathVariable Long hotelId) {
        return restaurantService.getFoodItemsByHotel(hotelId);
    }

    @GetMapping("/food")
    public ResponseEntity<List<FoodItemResponseDTO>> getAllFoodItems() {
        return restaurantService.getAllFoodItems();
    }

    @PostMapping("/order")
    public ResponseEntity<RestaurantOrderResponseDTO> createOrder(@RequestBody RestaurantOrderDTO dto, Authentication auth) {
        return restaurantService.createOrder(dto, getCurrentUserId(auth));
    }

    @PostMapping("/order/{orderId}/payment")
    public ResponseEntity<PaymentResponseDTO> makeRestaurantOrderPayment(
            @PathVariable Long orderId,
            @RequestBody PaymentRequestDTO dto,
            Authentication auth) {

        // set the order ID inside the DTO for service method
        dto.setRestaurantOrderId(orderId);
        return restaurantService.makeRestaurantOrderPayment(dto, getCurrentUserId(auth));
    }

    @GetMapping("/order/{orderId}/invoice")
    public ResponseEntity<InvoiceResponseDTO> generateOrderInvoice(
            @PathVariable Long orderId,
            Authentication auth) {
        return restaurantService.generateOrderInvoice(orderId, getCurrentUserId(auth));
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<RestaurantOrderResponseDTO> getOrderById(@PathVariable Long id, Authentication auth) {
        return restaurantService.getOrderById(id, getCurrentUserId(auth));
    }

    @GetMapping("/guest/{guestId}/orders")
    public ResponseEntity<List<RestaurantOrderResponseDTO>> getOrdersByGuest(@PathVariable Long guestId, Authentication auth) {
        return restaurantService.getOrdersByGuest(guestId, getCurrentUserId(auth));
    }

    @PutMapping("/order/{id}/cancel")
    public ResponseEntity<RestaurantOrderResponseDTO> cancelOrder(
            @PathVariable Long id,
            Authentication auth
    ) {
        return restaurantService.cancelOrder(id, getCurrentUserId(auth));
    }

    @GetMapping("/restaurants")
    public ResponseEntity<List<RestaurantOrderResponseDTO>> getAllOrders(Authentication auth) {
        return restaurantService.getAllOrders(getCurrentUserId(auth));
    }

}
