package com.backend.product_service.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.backend.product_service.model.Product;

/**
 * Custom repository interface for complex Product queries
 */
public interface ProductRepositoryCustom {
    /**
     * Search and filter products with optional criteria
     * All parameters are optional - only applied if not null
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
