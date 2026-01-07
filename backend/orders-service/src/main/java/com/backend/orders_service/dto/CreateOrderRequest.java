package com.backend.orders_service.dto;

import java.util.List;

import com.backend.orders_service.model.OrderItem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String shippingAddress;
    @NotEmpty
    private List<OrderItem> items;
    @NotNull
    private String paymentMethod; // PAY_ON_DELIVERY
}
