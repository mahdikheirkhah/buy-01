package com.backend.orders_service.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom repository implementation for complex Order queries
 * Uses MongoTemplate to dynamically build queries based on provided filters
 */
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    /**
     * Search and filter orders for a user with dynamic criteria
     * Only applies filters for non-null parameters
     * Database-level filtering for: user, isRemoved, keyword, date range, status
     * In-memory filtering for: price range (requires calculated totals)
     *
     * @param userId        - User ID (required)
     * @param keyword       - Search in order ID or product names (optional)
     * @param minPrice      - Minimum order total price (optional)
     * @param maxPrice      - Maximum order total price (optional)
     * @param minUpdateDate - Minimum update date (optional)
     * @param maxUpdateDate - Maximum update date (optional)
     * @param statuses      - List of order statuses to include (optional)
     * @param pageable      - Pagination info
     * @return Page of orders matching the criteria
     */
    @Override
    public Page<Order> searchAndFilterOrdersByUser(
            String userId,
            String keyword,
            Double minPrice,
            Double maxPrice,
            Instant minUpdateDate,
            Instant maxUpdateDate,
            List<OrderStatus> statuses,
            Pageable pageable) {

        log.info("Searching orders for userId: {} - keyword: {}, price: {}-{}, dates: {} to {}, statuses: {}",
                userId, keyword, minPrice, maxPrice, minUpdateDate, maxUpdateDate, statuses);

        // Build criteria dynamically at database level
        List<Criteria> criteria = new ArrayList<>();

        // User filter (required)
        criteria.add(Criteria.where("userId").is(userId));
        criteria.add(Criteria.where("isRemoved").is(false));

        // Keyword filter - search in order ID or product names (case-insensitive)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String sanitized = keyword.trim();
            Pattern pattern = Pattern.compile(Pattern.quote(sanitized), Pattern.CASE_INSENSITIVE);

            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("id").regex(pattern),
                    Criteria.where("items.productName").regex(pattern));
            criteria.add(keywordCriteria);
        }

        // Date range filters on updatedAt field
        if (minUpdateDate != null && maxUpdateDate != null) {
            criteria.add(Criteria.where("updatedAt").gte(minUpdateDate).lte(maxUpdateDate));
        } else if (minUpdateDate != null) {
            criteria.add(Criteria.where("updatedAt").gte(minUpdateDate));
        } else if (maxUpdateDate != null) {
            criteria.add(Criteria.where("updatedAt").lte(maxUpdateDate));
        }

        // Status filter
        if (statuses != null && !statuses.isEmpty()) {
            criteria.add(Criteria.where("status").in(statuses));
        }

        // Combine all criteria with AND
        Query query = new Query();
        if (!criteria.isEmpty()) {
            Criteria combinedCriteria = new Criteria().andOperator(criteria.toArray(new Criteria[0]));
            query.addCriteria(combinedCriteria);
        }

        // Apply sorting and pagination
        query.with(pageable);

        // Execute count query for pagination
        long total = mongoTemplate.count(query, Order.class);

        // Execute search query
        List<Order> results = mongoTemplate.find(query, Order.class);

        log.info("Found {} orders matching criteria (out of {} total before filtering)", results.size(), total);

        // Price range filtering is done in-memory since it requires calculating order
        // totals
        if (minPrice != null || maxPrice != null) {
            results = filterByPriceRange(results, minPrice, maxPrice);
        }

        log.info("After price filtering: {} orders", results.size());

        return new PageImpl<>(results, pageable, results.size());
    }

    /**
     * Get all non-pending orders with optional date and status filtering at DB
     * level
     * Used by seller order service for efficient fetching
     *
     * @param minDate  - Minimum update date (optional)
     * @param maxDate  - Maximum update date (optional)
     * @param statuses - List of statuses to include (optional)
     * @return List of orders matching the criteria
     */
    @Override
    public List<Order> findNonPendingOrdersWithFilters(
            Instant minDate,
            Instant maxDate,
            List<OrderStatus> statuses) {

        log.info("Fetching non-pending orders - dates: {} to {}, statuses: {}", minDate, maxDate, statuses);

        List<Criteria> criteria = new ArrayList<>();

        // Exclude PENDING status
        criteria.add(Criteria.where("status").ne(OrderStatus.PENDING));

        // Date range filters on updatedAt field
        if (minDate != null && maxDate != null) {
            criteria.add(Criteria.where("updatedAt").gte(minDate).lte(maxDate));
        } else if (minDate != null) {
            criteria.add(Criteria.where("updatedAt").gte(minDate));
        } else if (maxDate != null) {
            criteria.add(Criteria.where("updatedAt").lte(maxDate));
        }

        // Status filter (additional to non-pending)
        if (statuses != null && !statuses.isEmpty()) {
            criteria.add(Criteria.where("status").in(statuses));
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            Criteria combinedCriteria = new Criteria().andOperator(criteria.toArray(new Criteria[0]));
            query.addCriteria(combinedCriteria);
        }

        List<Order> results = mongoTemplate.find(query, Order.class);
        log.info("Found {} non-pending orders", results.size());

        return results;
    }

    /**
     * Search and filter orders for a seller with dynamic criteria
     * Database-level filtering for: non-pending status, keyword, date range, status
     * Service-level filtering for: seller item filtering, price range for seller
     * items only
     *
     * @param keyword       - Search in order ID or product names (optional)
     * @param minPrice      - Minimum price (service-level filtering for seller
     *                      items)
     * @param maxPrice      - Maximum price (service-level filtering for seller
     *                      items)
     * @param minUpdateDate - Minimum update date (optional)
     * @param maxUpdateDate - Maximum update date (optional)
     * @param statuses      - List of order statuses to include (optional)
     * @param pageable      - Pagination info
     * @return Page of orders with database-level filters applied
     */
    @Override
    public Page<Order> searchAndFilterOrdersForSeller(
            String keyword,
            Double minPrice,
            Double maxPrice,
            Instant minUpdateDate,
            Instant maxUpdateDate,
            List<OrderStatus> statuses,
            Pageable pageable) {

        log.info("Searching seller orders - keyword: {}, dates: {} to {}, statuses: {}",
                keyword, minUpdateDate, maxUpdateDate, statuses);

        List<Criteria> criteria = new ArrayList<>();

        // Exclude PENDING status
        criteria.add(Criteria.where("status").ne(OrderStatus.PENDING));

        // Keyword filter - search in order ID or product names (case-insensitive)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String sanitized = keyword.trim();
            Pattern pattern = Pattern.compile(Pattern.quote(sanitized), Pattern.CASE_INSENSITIVE);

            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("id").regex(pattern),
                    Criteria.where("items.productName").regex(pattern));
            criteria.add(keywordCriteria);
        }

        // Date range filters on updatedAt field
        if (minUpdateDate != null && maxUpdateDate != null) {
            criteria.add(Criteria.where("updatedAt").gte(minUpdateDate).lte(maxUpdateDate));
        } else if (minUpdateDate != null) {
            criteria.add(Criteria.where("updatedAt").gte(minUpdateDate));
        } else if (maxUpdateDate != null) {
            criteria.add(Criteria.where("updatedAt").lte(maxUpdateDate));
        }

        // Status filter (additional to non-pending)
        if (statuses != null && !statuses.isEmpty()) {
            criteria.add(Criteria.where("status").in(statuses));
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            Criteria combinedCriteria = new Criteria().andOperator(criteria.toArray(new Criteria[0]));
            query.addCriteria(combinedCriteria);
        }

        query.with(pageable);

        long total = mongoTemplate.count(query, Order.class);
        List<Order> results = mongoTemplate.find(query, Order.class);

        log.info("Found {} seller orders at DB level (before service-level seller item & price filtering)",
                results.size());

        // Price range filtering will be done at service level after resolving seller
        // IDs and filtering items
        return new PageImpl<>(results, pageable, total);
    }

    /**
     * Filter orders by price range (in-memory)
     * Calculates total price for each order
     */
    private List<Order> filterByPriceRange(List<Order> orders, Double minPrice, Double maxPrice) {
        return orders.stream()
                .filter(order -> {
                    double totalPrice = order.getItems().stream()
                            .mapToDouble(item -> {
                                double itemPrice = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
                                int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                                return itemPrice * quantity;
                            })
                            .sum();

                    if (minPrice != null && totalPrice < minPrice)
                        return false;
                    if (maxPrice != null && totalPrice > maxPrice)
                        return false;
                    return true;
                })
                .toList();
    }
}