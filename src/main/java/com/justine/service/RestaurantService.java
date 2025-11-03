package com.justine.service;

import com.justine.dtos.request.FoodItemRequestDTO;
import com.justine.dtos.request.PaymentRequestDTO;
import com.justine.dtos.request.RestaurantOrderDTO;
import com.justine.dtos.response.FoodItemResponseDTO;
import com.justine.dtos.response.InvoiceResponseDTO;
import com.justine.dtos.response.PaymentResponseDTO;
import com.justine.dtos.response.RestaurantOrderResponseDTO;
import org.springframework.http.ResponseEntity;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RestaurantService {

    // --- FOOD ITEMS ---
    ResponseEntity<FoodItemResponseDTO> addFoodItem(FoodItemRequestDTO dto, MultipartFile imageFile, Long currentUserId);

    ResponseEntity<FoodItemResponseDTO> updateFoodItem(Long id, FoodItemRequestDTO dto, MultipartFile imageFile, Long currentUserId);

    ResponseEntity<Void> deleteFoodItem(Long id, Long currentUserId);
    ResponseEntity<List<FoodItemResponseDTO>> getFoodItemsByHotel(Long hotelId);
    ResponseEntity<List<FoodItemResponseDTO>> getAllFoodItems();

    // --- ORDERS ---
    ResponseEntity<RestaurantOrderResponseDTO> createOrder(RestaurantOrderDTO dto, Long currentUserId);

    // --- PAYMENTS & INVOICES ---
    ResponseEntity<PaymentResponseDTO> makeRestaurantOrderPayment(PaymentRequestDTO dto, Long currentUserId);

    ResponseEntity<InvoiceResponseDTO> generateOrderInvoice(Long orderId, Long currentUserId);
    ResponseEntity<RestaurantOrderResponseDTO> getOrderById(Long orderId, Long currentUserId);
    ResponseEntity<List<RestaurantOrderResponseDTO>> getOrdersByGuest(Long guestId, Long currentUserId);

    ResponseEntity<RestaurantOrderResponseDTO> cancelOrder(Long orderId, Long currentUserId);

    ResponseEntity<List<RestaurantOrderResponseDTO>> getAllOrders(Long currentUserId);
}
