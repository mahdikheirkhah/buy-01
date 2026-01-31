package com.backend.product_service.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Document(collection = "products")
@Data
@Builder
public class Product {
    @Id
    private String id;
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
    private String name;
    @NotBlank(message = "Product description is required")
    @Size(min = 5, message = "Product description must be at least 5 characters")
    private String description;
    @Positive(message = "Product price must be positive")
    private Double price;
    @PositiveOrZero(message = "Product quantity must be zero or greater")
    private Integer quantity;
    @NotBlank
    private String sellerID;
    @CreatedDate
    private Instant createdAt; // Automatically set on creation
    @LastModifiedDate
    private Instant updatedAt;

}
