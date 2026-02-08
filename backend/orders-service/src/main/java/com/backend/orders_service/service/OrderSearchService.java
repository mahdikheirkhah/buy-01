package com.backend.orders_service.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.orders_service.dto.SellerOrderDTO;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified service for order search and filtering
 * Handles both client (user) and seller order searches
 * - Client orders: all items in the order with price calculated for all items
 * - Seller orders: only items belonging to the seller with price calculated for
 * seller's items only
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSearchService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private static final String PRODUCT_SERVICE_URL = "http://product-service";

    // Cache for product details
    private Map<String, Map<String, Object>> productCache = new HashMap<>();

    // ==================== CLIENT ORDER SEARCH ====================

    /**
     * Search and filter orders for a client
     * All parameters are optional - if none are provided, returns all non-removed
     * orders
     *
     * @param userId        - The client's ID
     * @param keyword       - Search keyword (matches order ID or product name)
     *                      (optional)
     * @param minPrice      - Minimum order total price (optional)
     * @param maxPrice      - Maximum order total price (optional)
     * @param minUpdateDate - Min last update date (ISO format or yyyy-MM-dd)
     *                      (optional)
     * @param maxUpdateDate - Max last update date (ISO format or yyyy-MM-dd)
     *                      (optional)
     * @param statuses      - List of order statuses to filter by (optional)
     * @param page          - Page number (0-indexed)
     * @param size          - Page size
     * @return Page of filtered orders
     */
    public Page<Order> searchAndFilterOrders(
            String userId,
            String keyword,
            Double minPrice,
            Double maxPrice,
            String minUpdateDate,
            String maxUpdateDate,
            List<OrderStatus> statuses,
            int page,
            int size) {

        log.info("Searching orders for userId: {} - keyword: {}, priceRange: {}-{}, dateRange: {} to {}, statuses: {}",
                userId, keyword, minPrice, maxPrice, minUpdateDate, maxUpdateDate, statuses);

        // Validate all search parameters using shared validation
        if (!OrderSearchValidation.validateSearchParameters(minPrice, maxPrice, minUpdateDate, maxUpdateDate)) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // Parse dates using shared validation
        Instant[] dateRange = OrderSearchValidation.parseDateRange(minUpdateDate, maxUpdateDate);
        Instant parsedMinDate = dateRange != null ? dateRange[0] : null;
        Instant parsedMaxDate = dateRange != null ? dateRange[1] : null;

        // Use custom repository for database-level filtering
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        // Call custom repository method for database-level filtering
        // This applies filters at the database level for efficiency
        Page<Order> filteredOrders = orderRepository.searchAndFilterOrdersByUser(
                userId,
                keyword,
                minPrice,
                maxPrice,
                parsedMinDate,
                parsedMaxDate,
                statuses,
                pageable);

        log.info("Found {} orders matching criteria (out of {} total)", filteredOrders.getNumberOfElements(),
                filteredOrders.getTotalElements());

        return filteredOrders;
    }

    // ==================== SELLER ORDER SEARCH ====================

    /**
     * Search and filter orders for a specific seller
     * Filters items by seller and calculates total price based only on seller's
     * items
     * 
     * @param sellerId      - The seller's user ID
     * @param keyword       - Search keyword (matches order ID or product name)
     * @param minPrice      - Minimum total price (for seller's items only)
     * @param maxPrice      - Maximum total price (for seller's items only)
     * @param minUpdateDate - Min last update date
     * @param maxUpdateDate - Max last update date
     * @param statuses      - List of order statuses to filter by
     * @param page          - Page number (0-indexed)
     * @param size          - Page size
     * @return Page of filtered seller orders
     */
    public Page<SellerOrderDTO> searchAndFilterSellerOrders(
            String sellerId,
            String keyword,
            Double minPrice,
            Double maxPrice,
            String minUpdateDate,
            String maxUpdateDate,
            List<OrderStatus> statuses,
            int page,
            int size) {

        log.info("========== SELLER ORDER SEARCH START ==========");
        log.info("Seller ID: {}", sellerId);
        log.info("Keyword: {}", keyword);
        log.info("Price Range: {} - {}", minPrice, maxPrice);
        log.info("Date Range: {} to {}", minUpdateDate, maxUpdateDate);
        log.info("Statuses: {}", statuses);
        log.info("Page: {}, Size: {}", page, size);

        // Validate all search parameters using shared validation
        if (!OrderSearchValidation.validateSearchParameters(minPrice, maxPrice, minUpdateDate, maxUpdateDate)) {
            log.warn("Invalid search parameters - returning empty results");
            Pageable pageable = PageRequest.of(page, size);
            return new PageImpl<>(new java.util.ArrayList<>(), pageable, 0);
        }

        // Parse dates using shared validation
        Instant[] dateRange = OrderSearchValidation.parseDateRange(minUpdateDate, maxUpdateDate);
        Instant parsedMinDate = dateRange != null ? dateRange[0] : null;
        Instant parsedMaxDate = dateRange != null ? dateRange[1] : null;
        log.info("Parsed date range: {} to {}", parsedMinDate, parsedMaxDate);

        // Create pageable for initial database query
        Pageable pageable = PageRequest.of(page, size);
        log.info("Created pageable: page={}, size={}", page, size);

        // Use custom repository method for database-level filtering (non-pending,
        // keyword, date, status)
        // Price filtering will be done at service level after resolving seller IDs
        log.info("Calling searchAndFilterOrdersForSeller on repository...");
        Page<Order> dbFilteredOrders = orderRepository.searchAndFilterOrdersForSeller(
                keyword,
                minPrice,
                maxPrice,
                parsedMinDate,
                parsedMaxDate,
                statuses,
                pageable);

        log.info("DATABASE QUERY RESULT: {} orders fetched at page {}", dbFilteredOrders.getNumberOfElements(), page);
        log.info("Total elements in database matching criteria: {}", dbFilteredOrders.getTotalElements());

        // Create final local variables for use in lambda expressions
        final Double finalMinPrice = minPrice;
        final Double finalMaxPrice = maxPrice;

        // Filter and convert orders: apply seller filter, then price range filter
        // This resolves seller IDs and filters items by seller, then checks price range
        log.info("Starting stream processing for {} orders", dbFilteredOrders.getContent().size());
        List<SellerOrderDTO> filteredOrders = dbFilteredOrders.getContent().stream()
                .peek(order -> log.debug("Processing order: {} with {} items", order.getId(), order.getItems().size()))
                .map(order -> {
                    log.debug("Converting to SellerOrderDTO for seller: {}", sellerId);
                    return convertToSellerOrderDTO(order, sellerId);
                })
                .peek(dto -> log.debug("After conversion - seller items: {}", dto.getItems().size()))
                .filter(dto -> {
                    boolean hasItems = !dto.getItems().isEmpty();
                    if (!hasItems) {
                        log.debug("Filtering out order {} - no seller items", dto.getId());
                    }
                    return hasItems;
                })
                .peek(dto -> {
                    double price = dto.getItems().stream()
                            .mapToDouble(item -> item.getPrice().doubleValue() * item.getQuantity())
                            .sum();
                    log.debug("Order {} - seller items total: {}", dto.getId(), price);
                })
                .filter(dto -> {
                    boolean matches = matchesSellerPriceRange(dto, finalMinPrice, finalMaxPrice);
                    if (!matches) {
                        double price = dto.getItems().stream()
                                .mapToDouble(item -> item.getPrice().doubleValue() * item.getQuantity())
                                .sum();
                        log.debug("Filtering out order {} - price {} not in range {}-{}", dto.getId(), price,
                                finalMinPrice, finalMaxPrice);
                    }
                    return matches;
                })
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .collect(Collectors.toList());

        log.info("FINAL RESULT: {} seller orders after all filtering and processing", filteredOrders.size());
        log.info("========== SELLER ORDER SEARCH END ==========");

        // Return paginated results
        return new PageImpl<>(filteredOrders, pageable, dbFilteredOrders.getTotalElements());
    }

    // ==================== SELLER ORDER HELPER METHODS ====================

    /**
     * Convert Order to SellerOrderDTO, filtering items by sellerId
     * Resolves seller IDs from product service if not stored in item
     * Public method for seller order operations
     */
    public SellerOrderDTO convertToSellerOrderDTO(Order order, String sellerId) {
        log.debug("=== CONVERT TO SELLER DTO START ===");
        log.debug("Order ID: {}, Seller ID: {}, Total items in order: {}", order.getId(), sellerId,
                order.getItems().size());

        List<OrderItem> sellerItems = order.getItems().stream()
                .peek(item -> log.debug("  Checking item for product: {}", item.getProductId()))
                .filter(item -> {
                    String resolvedItemSellerId = resolveItemSellerId(item);
                    boolean matches = sellerId.equals(resolvedItemSellerId);
                    log.debug("    Item seller: {}, Target seller: {}, Match: {}", resolvedItemSellerId, sellerId,
                            matches);
                    return matches;
                })
                .collect(Collectors.toList());

        log.debug("Seller items count: {} out of {}", sellerItems.size(), order.getItems().size());

        SellerOrderDTO dto = new SellerOrderDTO();
        dto.setId(order.getId());
        dto.setOrderId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setItems(sellerItems);
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        log.debug("=== CONVERT TO SELLER DTO END ===");
        return dto;
    }

    /**
     * Resolve the seller ID for an order item
     * First checks if sellerId is stored in the item
     * If null/empty, attempts to fetch from product service
     * If product service fails, returns null (item won't match any seller)
     */
    private String resolveItemSellerId(OrderItem item) {
        String itemSellerId = item.getSellerId();
        String productId = item.getProductId();
        String productName = item.getProductName();

        log.debug("    ===== RESOLVE ITEM SELLER ID START =====");
        log.debug("    Product: '{}' (ID: {})", productName, productId);
        log.debug("    Item.sellerId: {}", itemSellerId == null ? "NULL/EMPTY" : itemSellerId);

        if (itemSellerId == null || itemSellerId.isEmpty()) {
            log.debug("    >>> ATTEMPT 1: Item has NO seller ID stored");
            log.debug("    >>> ATTEMPT 2: Will try to fetch from product service");
            String resolvedId = getProductSellerId(productId);
            if (resolvedId == null) {
                log.debug("    >>> RESULT: Could not resolve seller ID from product service - returning NULL");
                log.debug("    >>> This item will NOT match any seller");
                log.debug("    ===== RESOLVE ITEM SELLER ID END =====");
                return null;
            }
            log.debug("    >>> RESULT: Resolved seller ID from product service: {}", resolvedId);
            log.debug("    ===== RESOLVE ITEM SELLER ID END =====");
            return resolvedId;
        }

        log.debug("    >>> RESULT: Using seller ID from item: {}", itemSellerId);
        log.debug("    ===== RESOLVE ITEM SELLER ID END =====");
        return itemSellerId;
    }

    /**
     * Fetch seller ID from product service
     * Uses cached data to avoid redundant API calls
     */
    private String getProductSellerId(String productId) {
        log.debug("      getProductSellerId() called for: {}", productId);
        try {
            Map<String, Object> product = getProductDetails(productId);

            if (product == null) {
                log.warn("      !!! FAILED: Product details is null - product service returned null or error");
                return null;
            }

            log.debug("      SUCCESS: Received product map with keys: {}", product.keySet());

            if (!product.isEmpty() && product.containsKey("sellerID")) {
                String sellerID = product.get("sellerID").toString();
                log.debug("      !!! FOUND: sellerID = {} in product map", sellerID);
                return sellerID;
            }

            log.warn("      !!! NOT FOUND: sellerID key not found in product map. Available keys: {}",
                    product.keySet());
        } catch (Exception e) {
            log.warn("      !!! ERROR: Could not fetch sellerID: {}", e.getMessage());
        }
        log.debug("      Returning NULL");
        return null;
    }

    /**
     * Fetch product details from product service using the lightweight /simple
     * endpoint
     * Uses cache to avoid redundant API calls
     * Returns: {productId, productName, price, sellerID}
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getProductDetails(String productId) {
        if (productCache.containsKey(productId)) {
            log.debug("        CACHE HIT: Found product {} in cache", productId);
            return productCache.get(productId);
        }

        try {
            log.debug("        CACHE MISS: Fetching product {} from product service...", productId);
            String url = PRODUCT_SERVICE_URL + "/api/products/simple/" + productId;
            log.debug("        URL: {}", url);
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                log.debug("        SUCCESS: Fetched product {} - response keys: {}", productId, response.keySet());
                productCache.put(productId, response);
                return response;
            }
            log.warn("        ERROR: Product service returned null response for {}", productId);
            return null;
        } catch (Exception e) {
            log.warn("        ERROR: Failed to fetch product {}: {} - {}", productId, e.getClass().getSimpleName(),
                    e.getMessage());
            return null;
        }
    }

    /**
     * Check if seller order matches price range (only seller's items count)
     * Sums only the items belonging to this seller
     */
    private boolean matchesSellerPriceRange(SellerOrderDTO order, Double minPrice, Double maxPrice) {
        if (minPrice == null && maxPrice == null) {
            log.debug("No price range filters - all orders match");
            return true;
        }

        // Calculate total price for seller's items only
        double sellerTotal = order.getItems().stream()
                .mapToDouble(item -> {
                    double itemTotal = item.getPrice().doubleValue() * item.getQuantity();
                    log.debug("    Item: {} x {} = {}", item.getProductName(), item.getQuantity(), itemTotal);
                    return itemTotal;
                })
                .sum();

        log.debug("Order {} - seller items total: {}", order.getId(), sellerTotal);

        if (minPrice != null && sellerTotal < minPrice) {
            log.debug("  Price {} is below minimum {}", sellerTotal, minPrice);
            return false;
        }

        if (maxPrice != null && sellerTotal > maxPrice) {
            log.debug("  Price {} is above maximum {}", sellerTotal, maxPrice);
            return false;
        }

        log.debug("  Price {} matches range {}-{}", sellerTotal, minPrice, maxPrice);
        return true;
    }

    // ==================== SELLER ORDER SERVICE METHODS ====================

    /**
     * Get all orders containing items from a specific seller
     * Delegates to searchAndFilterSellerOrders with no filters
     */
    public Page<SellerOrderDTO> getSellerOrders(String sellerId, int page, int size) {
        log.info("=========== GET SELLER ORDERS START ===========");
        log.info("Seller ID: {}", sellerId);
        log.info("Page: {}, Size: {}", page, size);
        log.info("Note: Calling searchAndFilterSellerOrders with NO filters");
        Page<SellerOrderDTO> result = searchAndFilterSellerOrders(
                sellerId,
                null, // no keyword search
                null, // no min price
                null, // no max price
                null, // no min date
                null, // no max date
                null, // no status filter
                page,
                size);
        log.info("GET SELLER ORDERS RESULT: {} orders returned", result.getNumberOfElements());
        log.info("=========== GET SELLER ORDERS END ===========");
        return result;
    }

    /**
     * Get a specific order with only the seller's items
     * Returns null if the seller has no items in this order
     */
    public SellerOrderDTO getSellerOrderDetail(String orderId, String sellerId) {
        log.info("=== GET SELLER ORDER DETAIL START ===");
        log.info("Getting order detail for seller: {} - orderId: {}", sellerId, orderId);

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order not found: {}", orderId);
            log.info("=== GET SELLER ORDER DETAIL END (ORDER NOT FOUND) ===");
            return null;
        }

        log.info("Order found - total items in order: {}", order.getItems().size());

        // Reuse the conversion logic
        SellerOrderDTO sellerOrderDTO = convertToSellerOrderDTO(order, sellerId);

        log.info("After filtering - seller items count: {}", sellerOrderDTO.getItems().size());

        // If seller has no items in this order, return null
        if (sellerOrderDTO.getItems().isEmpty()) {
            log.warn("RESULT: Seller {} has NO authorized items in order {} - returning NULL", sellerId, orderId);
            log.info("=== GET SELLER ORDER DETAIL END (NO ITEMS) ===");
            return null;
        }

        log.info("RESULT: Order detail found for seller {} - order {} has {} authorized items",
                sellerId, orderId, sellerOrderDTO.getItems().size());
        log.info("=== GET SELLER ORDER DETAIL END (SUCCESS) ===");
        return sellerOrderDTO;
    }
}