package com.backend.product_service.service;

import com.backend.common.exception.CustomException;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductMapper;
import com.backend.product_service.repository.ProductRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Autowired
    public ProductService(ProductRepository productRepository,  ProductMapper productMapper) {
        this.productMapper = productMapper;
        this.productRepository = productRepository;
    }
    public Product getProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public void createProduct(String sellerId,CreateProductDTO productDto) {
        Product product = productDto.toProduct();
        if (checkId(sellerId)) {
            throw new CustomException("Seller ID is null", HttpStatus.UNAUTHORIZED);
        }

        product.setSellerID(sellerId);
        productRepository.save(product);
    }

    public void updateProduct(String productId, String sellerId ,UpdateProductDTO productDto) {
        Product existingProduct = checkProduct(productId, sellerId);
        productMapper.updateProductFromDto(productDto, existingProduct);
        productRepository.save(existingProduct);
    }

    public void deleteProduct(String productId, String sellerId) {
        Product existingProduct = checkProduct(productId, sellerId);
        productRepository.delete(existingProduct);
    }

    private boolean checkId(String id) {
        return id == null || id.isBlank();
    }
    private Product checkProduct(String productId, String sellerId) {
        if (checkId(productId)) {
            throw new CustomException("Seller ID is null", HttpStatus.UNAUTHORIZED);
        }
        if (checkId(sellerId)) {
            throw new CustomException("product id is null", HttpStatus.BAD_REQUEST);
        }
        Product existingProduct = getProduct(productId);
        if (!existingProduct.getSellerID().equals(sellerId)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return existingProduct;
    }
}
