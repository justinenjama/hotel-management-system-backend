package com.justine.controller;

import com.justine.dtos.request.FoodItemRequestDTO;
import com.justine.dtos.request.RestaurantOrderDTO;
import com.justine.dtos.response.FoodItemResponseDTO;
import com.justine.dtos.response.RestaurantOrderResponseDTO;
import com.justine.service.RestaurantService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurant")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping("/food")
    public ResponseEntity<FoodItemResponseDTO> addFoodItem(@RequestBody FoodItemRequestDTO dto, Authentication auth) {
        return restaurantService.addFoodItem(dto, auth.getName());
    }

    @PutMapping("/food/{id}")
    public ResponseEntity<FoodItemResponseDTO> updateFoodItem(@PathVariable Long id, @RequestBody FoodItemRequestDTO dto, Authentication auth) {
        return restaurantService.updateFoodItem(id, dto, auth.getName());
    }

    @DeleteMapping("/food/{id}")
    public ResponseEntity<Void> deleteFoodItem(@PathVariable Long id, Authentication auth) {
        return restaurantService.deleteFoodItem(id, auth.getName());
    }

    @GetMapping("/food")
    public ResponseEntity<List<FoodItemResponseDTO>> getAllFoodItems() {
        return restaurantService.getAllFoodItems();
    }

    @PostMapping("/order")
    public ResponseEntity<RestaurantOrderResponseDTO> createOrder(@RequestBody RestaurantOrderDTO dto, Authentication auth) {
        return restaurantService.createOrder(dto, auth.getName());
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<RestaurantOrderResponseDTO> getOrderById(@PathVariable Long id, Authentication auth) {
        return restaurantService.getOrderById(id, auth.getName());
    }

    @GetMapping("/guest/{guestId}/orders")
    public ResponseEntity<List<RestaurantOrderResponseDTO>> getOrdersByGuest(@PathVariable Long guestId, Authentication auth) {
        return restaurantService.getOrdersByGuest(guestId, auth.getName());
    }
}
