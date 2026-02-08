package com.backend.orders_service.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderStatus;

/**
 * Custom repository interface for complex Order queries
 */
public interface OrderRepositoryCustom {
    /**
     * Search and filter orders for a user with optional criteria
     * All parameters are optional - only applied if not null
     */
    Page<Order> searchAndFilterOrdersByUser(
            String userId,
            String keyword,
            Double minPrice,
            Double maxPrice,
            Instant minUpdateDate,
            Instant maxUpdateDate,
            List<OrderStatus> statuses,
            Pageable pageable);

    /**
     * Get all non-pending orders with optional date and status filtering
     * Used by seller order service to fetch orders at DB level
     */
    List<Order> findNonPendingOrdersWithFilters(
            Instant minDate,
            Instant maxDate,
            List<OrderStatus> statuses);

    /**
     * Search and filter orders for a seller with dynamic criteria
     * Filters non-pending orders by date range and status at database level
     * Price filtering requires seller-specific item calculation (done in service
     * layer)
     *
     * @param keyword       - Search in order ID or product names (optional)
     * @param minPrice      - Minimum price for seller's items only (optional,
     *                      service-level)
     * @param maxPrice      - Maximum price for seller's items only (optional,
     *                      service-level)
     * @param minUpdateDate - Minimum update date (optional)
     * @param maxUpdateDate - Maximum update date (optional)
     * @param statuses      - List of order statuses to include (optional)
     * @param pageable      - Pagination info
     * @return Page of orders with database-level filters applied
     */
    Page<Order> searchAndFilterOrdersForSeller(
            String keyword,
            Double minPrice,
            Double maxPrice,
            Instant minUpdateDate,
            Instant maxUpdateDate,
            List<OrderStatus> statuses,
            Pageable pageable);
}