package com.backend.product_service.dto;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.product_service.model.Product;
import jakarta.validation.constraints.*;
import lombok.Data;

import javax.print.attribute.standard.Media;
import java.util.List;

@Data
public class ProductDTO {
    @NotBlank
    String productId;
    @NotBlank(message = "name is required ")
    @Size(min = 3, max = 100, message = "name should be between 3 to 100 characters")
    private String name;
    @NotBlank(message = "description is required")
    @Size(min = 5, message = "description should consist at least 5 characters")
    private String description;
    @Positive(message = "price should be positive")
    private Double price;
    @PositiveOrZero(message = "quantity should be zero or more")
    private Integer quantity;

    @NotBlank(message = "First name is required")
    private String sellerFirstName;
    @NotBlank(message = "Last name is required")
    private String sellerLastName;
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String sellerEmail;
    private boolean CreatedByMe ;
    private List<MediaUploadResponseDTO> media;

    public ProductDTO(Product product, InfoUserDTO seller, List<MediaUploadResponseDTO> media) {
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