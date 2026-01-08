package com.backend.orders_service.dto;

import com.backend.orders_service.model.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {
    @NotBlank
    private String shippingAddress;

    @NotNull
    private PaymentMethod paymentMethod;
}
