package com.backend.product_service.dto;

import com.backend.product_service.model.Product;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple lightweight DTO for internal service-to-service calls
 * Contains only essential product data without seller details or media
 */
@Data
@NoArgsConstructor
public class ProductSimpleDTO {
    private String productId;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private String sellerID;

    public ProductSimpleDTO(Product product) {
        this.productId = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.quantity = product.getQuantity();
        this.sellerID = product.getSellerID();
    }
}
