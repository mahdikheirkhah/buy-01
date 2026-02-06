package com.backend.product_service.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.backend.product_service.model.Product;

public interface ProductRepository extends MongoRepository<Product, String>, ProductRepositoryCustom {

    // Find products by seller with pagination
    Page<Product> findBySellerID(String sellerId, Pageable pageable);

    List<Product> findAllBySellerID(String sellerId);

    /**
     * Search and filter products with dynamic criteria
     * Uses MongoDB aggregation-style query with proper null handling
     * Filters:
     * - keyword: searches in name and description (case-insensitive regex)
     * - price: between minPrice and maxPrice (if provided)
     * - quantity: between minQuantity and maxQuantity (if provided)
     * - createdAt: between startDate and endDate (if provided)
     */
    Page<Product> searchAndFilterProducts(
            String keyword,
            Double minPrice,
            Double maxPrice,
            Integer minQuantity,
            Integer maxQuantity,
            Instant startDate,
            Instant endDate,
            Pageable pageable);
}