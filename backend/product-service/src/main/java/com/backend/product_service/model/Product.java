package com.backend.product_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "products")
@Data
@Builder
public class Product {
    @Id
    private String id;
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
    @NotBlank
    private String sellerID;
    @CreatedDate
    private Instant createdAt; // Automatically set on creation
    @LastModifiedDate
    private Instant updatedAt;

}
