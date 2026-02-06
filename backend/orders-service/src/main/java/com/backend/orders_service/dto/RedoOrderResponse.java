package com.backend.orders_service.dto;

import java.util.ArrayList;
import java.util.List;

import com.backend.orders_service.model.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedoOrderResponse {
    private Order order;
    private String message;
    private List<String> outOfStockProducts = new ArrayList<>();
    private List<String> partiallyFilledProducts = new ArrayList<>();

    /**
     * Example messages:
     * - "All items successfully added to cart"
     * - "Some items could not be added: Product 'Item1' is out of stock"
     * - "Some items added with reduced quantities: Product 'Item2' has only 3
     * available instead of 5"
     */
}
