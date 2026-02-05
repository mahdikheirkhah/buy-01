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
    @NotBlank(message = "Name is mandatory")
    @Size(min = 3, max = 100, message = "Name should have 3-100 characters")
    private String name;
    @NotBlank(message = "Description is mandatory")
    @Size(min = 5, message = "Description should have minimum 5 characters")
    private String description;
    @Positive(message = "Price should be a positive number")
    private Double price;
    @PositiveOrZero(message = "Quantity should be non-negative")
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
