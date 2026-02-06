package com.backend.orders_service.dto;

import java.util.ArrayList;
import java.util.List;

import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String shippingAddress;
    private List<OrderItem> items = new ArrayList<>();
    @NotNull
    private PaymentMethod paymentMethod;
}
