package com.justine.repository;

import com.justine.dtos.response.FoodItemResponseDTO;
import com.justine.model.FoodItem;
import io.micrometer.common.KeyValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    List<FoodItem> findByHotelId(Long hotelId);
}
