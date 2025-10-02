package com.backend.product_service.dto;

import com.backend.product_service.model.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public class ClientProductDTO {
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

    public List<ClientProductDTO> fromProductList(List<Product> productList) {
        List<ClientProductDTO> clientProductDTOList = new ArrayList<>();
        for (Product product : productList) {
            clientProductDTOList.add(ClientProductDTO
                    .builder()
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .quantity(product.getQuantity())
                    .build());
        }
        return clientProductDTOList;
    }
}
