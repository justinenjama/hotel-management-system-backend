package com.justine.dtos.request;

import java.time.LocalDateTime;
import java.util.List;

import com.justine.model.Guest;
import com.justine.model.OrderItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantOrderDTO {
    private LocalDateTime orderDate;
    private Double totalAmount;
    private Guest guest;
    private Long hotelId;
    private List<OrderItemRequestDTO> orderItems;
}
