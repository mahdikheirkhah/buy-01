package com.backend.orders_service.model;

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
    @NotBlank
    private String productName;
    @NotBlank
    private String sellerId;
    @NotNull
    @Min(1)
    private Integer quantity;
    @NotNull
    @Min(0)
    private Double unitPrice;

    public Double getTotalPrice() {
        return unitPrice * quantity;
    }
}
