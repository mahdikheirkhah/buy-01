package com.backend.orders_service.dto;

import java.time.Instant;
import java.util.List;

import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerOrderDTO {
    @JsonProperty("id")
    private String orderId;
    private List<OrderItem> items;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String imageUrl; // Image URL of the first item in the order
}