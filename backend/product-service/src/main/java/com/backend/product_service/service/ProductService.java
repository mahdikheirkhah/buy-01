package com.backend.product_service.service;

import com.backend.common.exception.CustomException;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

public class ProductService {
    private final ProductRepository productRepository;
    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void createProduct(CreateProductDTO productDto, String sellerId) {
        if (productDto == null) {
            throw new CustomException("Product is null", HttpStatus.BAD_REQUEST);
        }
        Product product = productDto.toProduct();
        if (sellerId == null) {
            throw new CustomException("Seller ID is null", HttpStatus.BAD_REQUEST);
        }
        product.setSellerID(sellerId);
        productRepository.save(product);
    }

    public Product getProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public void deleteProduct(String id) {
        Product product =  getProduct(id);
        productRepository.delete(product);
    }

    public void updateProduct(String id, UpdateProductDTO productDto) {
        if (productDto == null) {
            throw new CustomException("Product is null", HttpStatus.BAD_REQUEST);
        }

        if (id == null || id.isBlank()) {
            throw new CustomException("Product id is null", HttpStatus.BAD_REQUEST);
        }

        Product product = getProduct(id);
        if(product == null) {
            throw new CustomException("Product not found", HttpStatus.NOT_FOUND);
        }

    }

}
