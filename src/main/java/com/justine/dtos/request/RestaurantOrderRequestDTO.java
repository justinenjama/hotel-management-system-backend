package com.justine.dtos.request;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantOrderRequestDTO {
    private Long guestId;
    private List<OrderItemRequestDTO> orderItems;
}
