package com.backend.product_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for product search and filtering
 * Supports keyword search by name, and filtering by price, quantity, and
 * creation date
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final ProductService productService;

    /**
     * Search products by keyword and apply filters
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

        // Get all products (in real app, use database query for efficiency)
        List<Product> allProducts = productRepository.findAll();

        // Filter products based on criteria
        List<ProductDTO> filtered = allProducts.stream()
                .filter(p -> matchesKeyword(p, keyword))
                .filter(p -> matchesPriceRange(p, minPrice, maxPrice))
                .filter(p -> matchesQuantityRange(p, minQuantity, maxQuantity))
                .filter(p -> matchesDateRange(p, startDate, endDate))
                .map(p -> productService.getProductByProductID(p.getId()))
                .collect(Collectors.toList());

        log.info("Found {} products matching criteria", filtered.size());

        // Apply pagination manually (since we're filtering in memory)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());

        List<ProductDTO> pageContent = start <= filtered.size()
                ? filtered.subList(start, end)
                : List.of();

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    /**
     * Search products by keyword only (no other filters)
     */
    public Page<ProductDTO> searchByKeyword(String keyword, Pageable pageable) {
        return searchAndFilter(keyword, null, null, null, null, null, null, pageable);
    }

    /**
     * Filter products by price range only
     */
    public Page<ProductDTO> filterByPrice(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return searchAndFilter(null, minPrice, maxPrice, null, null, null, null, pageable);
    }

    /**
     * Filter products by quantity range only
     */
    public Page<ProductDTO> filterByQuantity(Integer minQuantity, Integer maxQuantity, Pageable pageable) {
        return searchAndFilter(null, null, null, minQuantity, maxQuantity, null, null, pageable);
    }

    /**
     * Filter products by creation date range only
     */
    public Page<ProductDTO> filterByDate(Instant startDate, Instant endDate, Pageable pageable) {
        return searchAndFilter(null, null, null, null, null, startDate, endDate, pageable);
    }

    /**
     * Check if product matches keyword (searches name and description)
     */
    private boolean matchesKeyword(Product product, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }

        String lowerKeyword = keyword.toLowerCase();
        boolean nameMatches = product.getName() != null && product.getName().toLowerCase().contains(lowerKeyword);
        boolean descMatches = product.getDescription() != null
                && product.getDescription().toLowerCase().contains(lowerKeyword);

        return nameMatches || descMatches;
    }

    /**
     * Check if product price is within range
     */
    private boolean matchesPriceRange(Product product, BigDecimal minPrice, BigDecimal maxPrice) {
        if (product.getPrice() == null) {
            return true;
        }

        Double price = product.getPrice();

        if (minPrice != null && price < minPrice.doubleValue()) {
            return false;
        }

        if (maxPrice != null && price > maxPrice.doubleValue()) {
            return false;
        }

        return true;
    }

    /**
     * Check if product quantity is within range
     */
    private boolean matchesQuantityRange(Product product, Integer minQuantity, Integer maxQuantity) {
        if (product.getQuantity() == null) {
            return true;
        }

        if (minQuantity != null && product.getQuantity() < minQuantity) {
            return false;
        }

        if (maxQuantity != null && product.getQuantity() > maxQuantity) {
            return false;
        }

        return true;
    }

    /**
     * Check if product creation date is within range
     */
    private boolean matchesDateRange(Product product, Instant startDate, Instant endDate) {
        if (product.getCreatedAt() == null) {
            return true;
        }

        if (startDate != null && product.getCreatedAt().isBefore(startDate)) {
            return false;
        }

        if (endDate != null && product.getCreatedAt().isAfter(endDate)) {
            return false;
        }

        return true;
    }

}
