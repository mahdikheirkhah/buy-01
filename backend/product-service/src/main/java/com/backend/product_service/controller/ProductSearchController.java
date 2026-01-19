package com.backend.product_service.controller;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.service.ProductSearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for product search and filtering
 * Single endpoint handles all cases: all products, search, and filters
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    /**
     * Search and filter products with optional criteria
     * If no parameters are provided, returns all products
     * 
     * Query parameters (all optional):
     * - q: Keyword search (searches in name and description)
     * - minPrice: Minimum price
     * - maxPrice: Maximum price
     * - minQuantity: Minimum quantity available
     * - maxQuantity: Maximum quantity available
     * - startDate: Filter products created after this date (ISO 8601 format)
     * - endDate: Filter products created before this date (ISO 8601 format)
     * - page: Page number (0-indexed, default 0)
     * - size: Page size (default 20)
     * - sort: Sort criteria (e.g., createdAt,desc or price,asc)
     * 
     * Examples:
     * - GET /api/products/search - Returns all products
     * - GET /api/products/search?q=laptop - Search by keyword
     * - GET /api/products/search?minPrice=500&maxPrice=1500 - Price filter
     * - GET /api/products/search?q=laptop&minPrice=500&maxPrice=1500 - Combined
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDTO>> searchProducts(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) Integer maxQuantity,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Search request - keyword: {}, minPrice: {}, maxPrice: {}, minQty: {}, maxQty: {}",
                keyword, minPrice, maxPrice, minQuantity, maxQuantity);

        // Parse dates if provided
        Instant parsedStartDate = startDate != null ? Instant.parse(startDate) : null;
        Instant parsedEndDate = endDate != null ? Instant.parse(endDate) : null;

        Page<ProductDTO> results = productSearchService.searchAndFilter(
                keyword,
                minPrice,
                maxPrice,
                minQuantity,
                maxQuantity,
                parsedStartDate,
                parsedEndDate,
                pageable);

        log.info("Search returned {} results on page {}", results.getNumberOfElements(), pageable.getPageNumber());
        return ResponseEntity.ok(results);
    }
}
