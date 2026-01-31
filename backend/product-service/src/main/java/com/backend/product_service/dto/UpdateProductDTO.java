package com.backend.product_service.dto;

import com.backend.product_service.model.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateProductDTO {
    @Size(min = 3, max = 100, message = "Name update requires 3 to 100 characters")
    private String name;
    @Size(min = 5, message = "Description update requires at least 5 characters")
    private String description;
    @PositiveOrZero(message = "Price update must be zero or positive")
    private Double price;
    @PositiveOrZero(message = "Quantity update must be non-negative")
    private Integer quantity;

    public Product toProduct() {
        return Product
                .builder()
                .name(this.name)
                .description(this.description)
                .price(this.price)
                .quantity(this.quantity)
                .build();
    }
}