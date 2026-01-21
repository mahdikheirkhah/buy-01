package com.backend.product_service.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.product_service.dto.ProductCardDTO;
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
    public ResponseEntity<Page<ProductCardDTO>> searchProducts(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) Integer maxQuantity,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestHeader(value = "X-User-ID", required = false) String sellerId,
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Search request - keyword: {}, minPrice: {}, maxPrice: {}, minQty: {}, maxQty: {}",
                keyword, minPrice, maxPrice, minQuantity, maxQuantity);

        // Basic validation for non-negative values and ordering
        if ((minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)) {
            log.warn("Rejecting search: negative price bounds");
            return ResponseEntity.badRequest().body(Page.empty(pageable));
        }
        if ((minQuantity != null && minQuantity < 0)
                || (maxQuantity != null && maxQuantity < 0)) {
            log.warn("Rejecting search: negative quantity bounds");
            return ResponseEntity.badRequest().body(Page.empty(pageable));
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            log.warn("Rejecting search: minPrice > maxPrice");
            return ResponseEntity.badRequest().body(Page.empty(pageable));
        }
        if (minQuantity != null && maxQuantity != null && minQuantity > maxQuantity) {
            log.warn("Rejecting search: minQuantity > maxQuantity");
            return ResponseEntity.badRequest().body(Page.empty(pageable));
        }

        // Parse dates if provided (support both full ISO and plain yyyy-MM-dd from HTML
        // date inputs)
        Instant parsedStartDate;
        Instant parsedEndDate;
        try {
            parsedStartDate = parseDate(startDate);
            parsedEndDate = parseDate(endDate);
        } catch (IllegalArgumentException ex) {
            log.warn("Rejecting search: invalid date format start={} end={}", startDate, endDate);
            return ResponseEntity.badRequest().body(Page.empty(pageable));
        }

        if (parsedStartDate != null && parsedEndDate != null && parsedStartDate.isAfter(parsedEndDate)) {
            log.warn("Rejecting search: startDate after endDate");
            return ResponseEntity.badRequest().body(Page.empty(pageable));
        }

        Page<ProductCardDTO> results = productSearchService.searchAndFilter(
                keyword,
                minPrice,
                maxPrice,
                minQuantity,
                maxQuantity,
                parsedStartDate,
                parsedEndDate,
                sellerId,
                pageable);

        log.info("Search returned {} results on page {}", results.getNumberOfElements(), pageable.getPageNumber());
        return ResponseEntity.ok(results);
    }

    /**
     * Parse an ISO-8601 instant or a plain yyyy-MM-dd (HTML date input) string to
     * Instant at UTC start of day.
     */
    private Instant parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(date);
        } catch (Exception ignored) {
            // Try simple date without time
        }
        try {
            LocalDate localDate = LocalDate.parse(date);
            return localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid date format: " + date);
        }
    }
}
