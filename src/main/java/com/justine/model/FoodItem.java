package com.justine.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

import com.justine.enums.FoodCategory;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemName;
    private Double price;
    @Enumerated(EnumType.STRING)
    private FoodCategory category;

    @OneToMany(mappedBy = "foodItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;
}
