// product-service/src.main.java.com.backend.product_service.dto/ProductCardDTO.java
package com.backend.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProductCardDTO {
    private String id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private boolean createdByMe;
    private List<String> imageUrls;
}