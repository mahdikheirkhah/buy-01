// product-service/src.main.java.com.backend.product_service.dto/ProductCardDTO.java
package com.backend.product_service.dto;

import java.util.List;

public record ProductCardDTO(
        String id,
        String name,
        String description,
        Double price,
        Integer quantity,
        boolean createdByMe,     // Your new boolean field
        List<String> imageUrls    // A limited list of images
) {
}