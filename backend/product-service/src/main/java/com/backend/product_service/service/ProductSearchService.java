package com.backend.product_service.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for product search and filtering
 * Uses MongoDB queries for efficient filtering at database level
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final ProductService productService;

    /**
     * Search and filter products with optional criteria
     * If all parameters are null, returns all products
     * 
     * @param keyword     - Search keyword (matches product name or description)
     * @param minPrice    - Minimum price filter (optional)
     * @param maxPrice    - Maximum price filter (optional)
     * @param minQuantity - Minimum quantity filter (optional)
     * @param maxQuantity - Maximum quantity filter (optional)
     * @param startDate   - Filter products created after this date (optional)
     * @param endDate     - Filter products created before this date (optional)
     * @param pageable    - Pagination info
     * @return Page of ProductDTO matching the criteria
     */
    public Page<ProductDTO> searchAndFilter(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minQuantity,
            Integer maxQuantity,
            Instant startDate,
            Instant endDate,
            Pageable pageable) {

        log.info(
                "Searching products - keyword: {}, priceRange: {}-{}, quantityRange: {}-{}, dateRange: {} to {}",
                keyword, minPrice, maxPrice, minQuantity, maxQuantity, startDate, endDate);

        // Convert BigDecimal to Double for MongoDB query
        Double minPriceDouble = minPrice != null ? minPrice.doubleValue() : null;
        Double maxPriceDouble = maxPrice != null ? maxPrice.doubleValue() : null;

        // Use repository query method - filtering happens at database level
        Page<Product> products = productRepository.searchAndFilterProducts(
                keyword,
                minPriceDouble,
                maxPriceDouble,
                minQuantity,
                maxQuantity,
                startDate,
                endDate,
                pageable);

        log.info("Found {} products matching criteria", products.getTotalElements());

        // Convert Product entities to ProductDTO with seller info
        return products.map(product -> productService.getProductByProductID(product.getId()));
    }
}
