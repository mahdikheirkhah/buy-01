package com.backend.orders_service.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;

/**
 * Shared validation methods for order search and filtering
 * Used by both OrderSearchService and SellerOrderSearchService
 */
@Slf4j
public class OrderSearchValidation {

    /**
     * Validate price range - ensures both are non-negative and min <= max
     * 
     * @return false if validation fails, true if valid or both null
     */
    public static boolean validatePriceRange(Double minPrice, Double maxPrice) {
        if ((minPrice != null && minPrice < 0) || (maxPrice != null && maxPrice < 0)) {
            log.warn("Rejecting search: negative price bounds - minPrice: {}, maxPrice: {}", minPrice, maxPrice);
            return false;
        }

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            log.warn("Rejecting search: minPrice > maxPrice - minPrice: {}, maxPrice: {}", minPrice, maxPrice);
            return false;
        }

        return true;
    }

    /**
     * Parse and validate date range
     * 
     * @return Instant array [minDate, maxDate] or null on error
     */
    public static Instant[] parseDateRange(String minUpdateDate, String maxUpdateDate) {
        Instant parsedMinDate = null;
        Instant parsedMaxDate = null;

        try {
            if (minUpdateDate != null && !minUpdateDate.isEmpty()) {
                parsedMinDate = parseDate(minUpdateDate);
            }
            if (maxUpdateDate != null && !maxUpdateDate.isEmpty()) {
                parsedMaxDate = parseDate(maxUpdateDate);
            }
        } catch (Exception e) {
            log.warn("Failed to parse dates: {}", e.getMessage());
            return null;
        }

        // Date ordering check
        if (parsedMinDate != null && parsedMaxDate != null && parsedMinDate.isAfter(parsedMaxDate)) {
            log.warn("Rejecting search: minDate > maxDate - minDate: {}, maxDate: {}", parsedMinDate,
                    parsedMaxDate);
            return null;
        }

        return new Instant[] { parsedMinDate, parsedMaxDate };
    }

    /**
     * Parse date string in multiple formats (ISO 8601 or yyyy-MM-dd)
     */
    public static Instant parseDate(String dateString) {
        try {
            // Try full ISO 8601 format first
            return Instant.parse(dateString);
        } catch (Exception e1) {
            try {
                // Try yyyy-MM-dd format (from HTML date input)
                LocalDate localDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
                return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (Exception e2) {
                log.warn("Could not parse date: {}", dateString);
                throw new IllegalArgumentException("Invalid date format: " + dateString);
            }
        }
    }

    /**
     * Validate all search parameters
     * 
     * @return true if all parameters are valid
     */
    public static boolean validateSearchParameters(
            Double minPrice,
            Double maxPrice,
            String minUpdateDate,
            String maxUpdateDate) {

        // Validate price range
        if (!validatePriceRange(minPrice, maxPrice)) {
            return false;
        }

        // Validate and parse date range
        Instant[] dateRange = parseDateRange(minUpdateDate, maxUpdateDate);
        if ((minUpdateDate != null || maxUpdateDate != null) && dateRange == null) {
            return false;
        }

        return true;
    }
}