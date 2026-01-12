package com.backend.orders_service.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @NotBlank
    private String productId;
    @NotNull
    @Min(1)
    private Integer quantity;

    // Store price and sellerId at time of purchase for historical accuracy
    private BigDecimal price;
    private String sellerId;
    private String productName;
}
