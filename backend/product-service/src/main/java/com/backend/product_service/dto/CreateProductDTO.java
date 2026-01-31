package com.backend.product_service.dto;

import com.backend.product_service.model.Product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProductDTO {
    @NotBlank(message = "Name field is required")
    @Size(min = 3, max = 100, message = "Name length must be 3-100 characters")
    private String name;
    @NotBlank(message = "Description field is required")
    @Size(min = 5, message = "Description must contain at least 5 characters")
    private String description;
    @Positive(message = "Price must be a positive value")
    private Double price;
    @PositiveOrZero(message = "Quantity cannot be negative")
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
