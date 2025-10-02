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
    @NotBlank(message = "name is required ")
    @Size(min = 3, max = 100, message = "name should be between 3 to 100 characters")
    private String name;
    @NotBlank(message = "description is required")
    @Size(min = 5 , message = "description should consist at least 5 characters")
    private String description;
    @Positive(message = "price should be positive")
    private Double price;
    @PositiveOrZero(message = "quantity should be zero or more")
    private Integer quantity;

    public Product toProduct() {
        return  Product
                .builder()
                .name(this.name)
                .description(this.description)
                .price(this.price)
                .quantity(this.quantity)
                .build();
    }
}
