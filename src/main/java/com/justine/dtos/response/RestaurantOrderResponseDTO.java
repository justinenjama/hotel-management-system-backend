package com.justine.dtos.response;

import com.justine.enums.OrderStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantOrderResponseDTO {
    private Long id;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private Double totalAmount;
    private GuestResponseDTO guest;
    private Long hotelId;
    private Boolean cart;
    private List<OrderItemResponseDTO> orderItems;
}
