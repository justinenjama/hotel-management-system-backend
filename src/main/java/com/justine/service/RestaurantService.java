package com.justine.service;

import com.justine.dtos.request.FoodItemRequestDTO;
import com.justine.dtos.request.RestaurantOrderDTO;
import com.justine.dtos.response.FoodItemResponseDTO;
import com.justine.dtos.response.RestaurantOrderResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface RestaurantService {

    // --- FOOD ITEMS ---
    ResponseEntity<FoodItemResponseDTO> addFoodItem(FoodItemRequestDTO dto, String currentUserEmail);
    ResponseEntity<FoodItemResponseDTO> updateFoodItem(Long id, FoodItemRequestDTO dto, String currentUserEmail);
    ResponseEntity<Void> deleteFoodItem(Long id, String currentUserEmail);
    ResponseEntity<List<FoodItemResponseDTO>> getAllFoodItems();

    // --- ORDERS ---
    ResponseEntity<RestaurantOrderResponseDTO> createOrder(RestaurantOrderDTO dto, String currentUserEmail);
    ResponseEntity<RestaurantOrderResponseDTO> getOrderById(Long orderId, String currentUserEmail);
    ResponseEntity<List<RestaurantOrderResponseDTO>> getOrdersByGuest(Long guestId, String currentUserEmail);
}
