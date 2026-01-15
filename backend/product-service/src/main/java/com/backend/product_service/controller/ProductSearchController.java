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
 * Controller for product search and filtering endpoints
 */
@RestController
@RequestMapping("/api/products/search")
@RequiredArgsConstructor
@Slf4j
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    /**
     * Search and filter products with all available filters
     * 
     * Query parameters:
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
     * Example:
     * GET /api/products/search?q=laptop&minPrice=500&maxPrice=1500&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<ProductDTO>> search(
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

    /**
     * Search products by keyword only
     * 
     * Example: GET /api/products/search/keyword?q=laptop
     */
    @GetMapping("/keyword")
    public ResponseEntity<Page<ProductDTO>> searchByKeyword(
            @RequestParam(name = "q") String keyword,
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Keyword search - q: {}", keyword);
        Page<ProductDTO> results = productSearchService.searchByKeyword(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Filter products by price range
     * 
     * Example: GET /api/products/search/price?minPrice=500&maxPrice=1500
     */
    @GetMapping("/price")
    public ResponseEntity<Page<ProductDTO>> filterByPrice(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, page = 0, sort = "price", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("Price filter - min: {}, max: {}", minPrice, maxPrice);
        Page<ProductDTO> results = productSearchService.filterByPrice(minPrice, maxPrice, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Filter products by quantity available
     * 
     * Example: GET /api/products/search/quantity?minQuantity=10&maxQuantity=100
     */
    @GetMapping("/quantity")
    public ResponseEntity<Page<ProductDTO>> filterByQuantity(
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) Integer maxQuantity,
            @PageableDefault(size = 20, page = 0, sort = "quantity", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Quantity filter - min: {}, max: {}", minQuantity, maxQuantity);
        Page<ProductDTO> results = productSearchService.filterByQuantity(minQuantity, maxQuantity, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Filter products by creation date range
     * 
     * Example: GET
     * /api/products/search/date?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z
     */
    @GetMapping("/date")
    public ResponseEntity<Page<ProductDTO>> filterByDate(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Date filter - start: {}, end: {}", startDate, endDate);

        Instant parsedStartDate = startDate != null ? Instant.parse(startDate) : null;
        Instant parsedEndDate = endDate != null ? Instant.parse(endDate) : null;

        Page<ProductDTO> results = productSearchService.filterByDate(parsedStartDate, parsedEndDate, pageable);
        return ResponseEntity.ok(results);
    }
}
