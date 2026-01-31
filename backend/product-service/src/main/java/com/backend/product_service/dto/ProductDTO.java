package com.backend.product_service.dto;

import java.util.List;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.product_service.model.Product;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductDTO {
    @NotBlank
    String productId;
    @NotBlank(message = "Product name cannot be blank")
    @Size(min = 3, max = 100, message = "Product name requires 3 to 100 characters")
    private String name;
    @NotBlank(message = "Product description cannot be blank")
    @Size(min = 5, message = "Product description requires minimum 5 characters")
    private String description;
    @Positive(message = "Price value must be positive")
    private Double price;
    @PositiveOrZero(message = "Quantity value must not be negative")
    private Integer quantity;

    @NotBlank(message = "First name is required")
    private String sellerFirstName;
    @NotBlank(message = "Last name is required")
    private String sellerLastName;
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String sellerEmail;
    private boolean CreatedByMe;
    private List<MediaUploadResponseDTO> media;

    public ProductDTO(Product product, InfoUserDTO seller, List<MediaUploadResponseDTO> media) {
        this.productId = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.quantity = product.getQuantity();
        this.sellerFirstName = seller.getFirstName();
        this.sellerLastName = seller.getLastName();
        this.sellerEmail = seller.getEmail();
        this.CreatedByMe = false;
        this.media = media;
    }
}