package com.backend.orders_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for calculating user and seller statistics from orders
 * Only counts DELIVERED orders for accurate user statistics
 * Single-pass calculation: iterate through orders once to compute all stats
 * Calls product-service only when needed to fetch product details
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatsService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String PRODUCT_SERVICE_URL = "http://product-service"; // Eureka service discovery

    // Cache for product details
    private Map<String, Map<String, Object>> productCache = new HashMap<>();

    /**
     * Calculate statistics for a specific user
     * ONLY counts DELIVERED orders
     * Returns: totalOrders, totalSpent, lastOrderDate, mostPurchasedProductId,
     * mostPurchasedProductName, mostPurchasedProductCount, totalQuantityBought
     */
    public Map<String, Object> calculateUserStats(String userId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get only DELIVERED orders for this user
            List<Order> userOrders = orderRepository.findByUserIdAndStatusOrderByOrderDateDesc(userId,
                    OrderStatus.DELIVERED);

            if (userOrders == null || userOrders.isEmpty()) {
                return initializeEmptyStats();
            }

            BigDecimal totalSpent = BigDecimal.ZERO;
            int totalOrders = 0;
            Instant lastOrderDate = null;
            int totalQuantityBought = 0;

            // Track most purchased product
            Map<String, Integer> productQuantityMap = new HashMap<>();
            Map<String, String> productNameMap = new HashMap<>();
            String mostPurchasedProductId = null;
            String mostPurchasedProductName = null;
            int mostPurchasedQuantity = 0;

            // Single-pass iteration through user's DELIVERED orders
            for (Order order : userOrders) {
                totalOrders++;

                // Sum up all item prices
                if (order.getItems() != null) {
                    for (var item : order.getItems()) {
                        BigDecimal price = getItemPrice(item, item.getProductId());
                        int quantity = item.getQuantity();
                        totalSpent = totalSpent.add(price.multiply(BigDecimal.valueOf(quantity)));
                        totalQuantityBought += quantity;

                        // Track most purchased product
                        String productId = item.getProductId();
                        String productName = item.getProductName();

                        // If productName is empty/null, fetch from product service
                        if (productName == null || productName.isEmpty()) {
                            productName = getProductName(productId);
                        }

                        int currentQuantity = productQuantityMap.getOrDefault(productId, 0);
                        int newQuantity = currentQuantity + quantity;
                        productQuantityMap.put(productId, newQuantity);
                        productNameMap.put(productId, productName);

                        // Update most purchased
                        if (newQuantity > mostPurchasedQuantity) {
                            mostPurchasedQuantity = newQuantity;
                            mostPurchasedProductId = productId;
                            mostPurchasedProductName = productName;
                        }
                    }
                }

                // Track latest order date
                Instant orderDate = order.getOrderDate();
                if (lastOrderDate == null || orderDate.isAfter(lastOrderDate)) {
                    lastOrderDate = orderDate;
                }
            }

            stats.put("totalOrders", totalOrders);
            stats.put("totalSpent", totalSpent);
            stats.put("lastOrderDate", lastOrderDate);
            stats.put("mostPurchasedProductId", mostPurchasedProductId);
            stats.put("mostPurchasedProductName", mostPurchasedProductName);
            stats.put("mostPurchasedProductCount", mostPurchasedQuantity);
            stats.put("totalQuantityBought", totalQuantityBought);

        } catch (Exception e) {
            log.error("Error calculating user stats for userId: {}", userId, e);
            stats.put("error", "Failed to calculate statistics");
            return initializeEmptyStats();
        }

        return stats;
    }

    /**
     * Calculate statistics for a specific seller
     * Counts DELIVERED orders for revenue and metrics
     * Also counts CANCELLED orders for tracking
     * Returns: totalRevenue, totalItemsSold, totalOrders, totalCustomers,
     * deliveredOrders,
     * cancelledOrders, lastDeliveredDate
     */
    public Map<String, Object> calculateSellerStats(String sellerId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get all orders (we'll filter by seller and status)
            List<Order> allOrders = orderRepository.findAll();

            if (allOrders == null || allOrders.isEmpty()) {
                log.warn("No orders found in database");
                return initializeEmptySellerStats();
            }

            log.info("Calculating stats for seller: {} from {} total orders", sellerId, allOrders.size());

            BigDecimal totalRevenue = BigDecimal.ZERO;
            int totalItemsSold = 0;
            Set<String> deliveredOrdersSet = new HashSet<>();
            Set<String> cancelledOrdersSet = new HashSet<>();
            Set<String> customersSet = new HashSet<>();
            Instant lastDeliveredDate = null;
            int itemsChecked = 0;
            int itemsMatched = 0;

            // Single-pass iteration through all orders
            for (Order order : allOrders) {
                String orderId = order.getId();
                String userId = order.getUserId();
                OrderStatus orderStatus = order.getStatus();

                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    for (var item : order.getItems()) {
                        itemsChecked++;
                        String itemSellerId = item.getSellerId();

                        // If sellerId is empty, or if we want to ensure accuracy,
                        // fetch it from product service
                        if (itemSellerId == null || itemSellerId.isEmpty()) {
                            log.info("Item sellerId is EMPTY for productId: {}, fetching from product service",
                                    item.getProductId());
                            itemSellerId = getProductSellerId(item.getProductId());
                        } else {
                            // Even if we have a sellerId, verify/fetch from product-service
                            // to ensure we have the correct one for stats calculation
                            log.info("Item has sellerId: {} for productId: {}, verifying from product service",
                                    itemSellerId, item.getProductId());
                            String productServiceSellerId = getProductSellerId(item.getProductId());
                            if (productServiceSellerId != null) {
                                itemSellerId = productServiceSellerId;
                                log.info("Updated sellerId from product-service to: {}", itemSellerId);
                            }
                        }

                        // Log for debugging - ALWAYS log to see what we're comparing
                        log.info(
                                "Item {} - productId: {}, sellerId from product-service: '{}', targetSellerId: '{}', status: {}, match: {}",
                                itemsChecked, item.getProductId(), itemSellerId, sellerId, orderStatus,
                                itemSellerId != null && itemSellerId.equals(sellerId));

                        // Only process items from this seller
                        if (itemSellerId == null || !itemSellerId.equals(sellerId)) {
                            continue;
                        }

                        itemsMatched++;

                        // Track order status
                        if (orderStatus == OrderStatus.DELIVERED) {
                            // Calculate revenue only for DELIVERED orders
                            BigDecimal price = getItemPrice(item, item.getProductId());
                            int quantity = item.getQuantity();
                            totalRevenue = totalRevenue.add(price.multiply(BigDecimal.valueOf(quantity)));

                            totalItemsSold += quantity;
                            deliveredOrdersSet.add(orderId);
                            customersSet.add(userId);

                            // Update last delivered date
                            Instant orderDate = order.getOrderDate();
                            if (lastDeliveredDate == null || orderDate.isAfter(lastDeliveredDate)) {
                                lastDeliveredDate = orderDate;
                            }

                            log.debug("Added item to seller stats - quantity: {}, price: {}, revenue increment: {}",
                                    quantity, price, price.multiply(BigDecimal.valueOf(quantity)));
                        } else if (orderStatus == OrderStatus.CANCELLED) {
                            // Track cancelled orders (don't count in revenue)
                            cancelledOrdersSet.add(orderId);
                        }
                    }
                }
            }

            log.info(
                    "Seller {} stats calculation - Checked {} items, matched {} items - Revenue: {}, Items: {}, Delivered Orders: {}, Cancelled Orders: {}, Customers: {}",
                    sellerId, itemsChecked, itemsMatched, totalRevenue, totalItemsSold, deliveredOrdersSet.size(),
                    cancelledOrdersSet.size(),
                    customersSet.size());

            // Calculate delivery rating (based on delivered orders)
            // Assuming delivery rating is 5.0 if all orders delivered, lower if some
            // cancelled
            double deliveryRating = 5.0;
            int totalOrders = deliveredOrdersSet.size() + cancelledOrdersSet.size();
            if (totalOrders > 0) {
                double deliveryRate = (double) deliveredOrdersSet.size() / totalOrders;
                // Delivery rating: 5.0 if 100%, 4.5 if 90%, 4.0 if 80%, etc.
                deliveryRating = Math.max(1.0, 5.0 * deliveryRate);
            }

            // Calculate cancellation rate as percentage
            double cancellationRatePercent = 0.0;
            if (totalOrders > 0) {
                cancellationRatePercent = (double) cancelledOrdersSet.size() / totalOrders * 100;
            }

            stats.put("totalRevenue", totalRevenue);
            stats.put("totalItemsSold", totalItemsSold);
            stats.put("totalDeliveredOrders", deliveredOrdersSet.size());
            stats.put("totalCancelledOrders", cancelledOrdersSet.size());
            stats.put("totalUniqueCustomers", customersSet.size());
            stats.put("lastDeliveredDate", lastDeliveredDate);
            stats.put("deliveryRating", deliveryRating);
            stats.put("cancellationRate", cancellationRatePercent);

        } catch (Exception e) {
            log.error("Error calculating seller stats for sellerId: {}", sellerId, e);
            stats.put("error", "Failed to calculate statistics");
            return initializeEmptySellerStats();
        }

        return stats;
    }

    /**
     * Fetch product price from cache or product service
     * Uses cached data when available to avoid redundant API calls
     */
    private BigDecimal getItemPrice(Object item, String productId) {
        try {
            // Handle JsonNode or OrderItem
            if (item instanceof JsonNode) {
                JsonNode itemNode = (JsonNode) item;
                if (itemNode.has("price") && !itemNode.get("price").isNull()) {
                    return BigDecimal.valueOf(itemNode.get("price").asDouble());
                }
            } else {
                // Handle OrderItem
                var orderItem = item;
                // Try to get price from item using reflection if available
                try {
                    java.lang.reflect.Field priceField = orderItem.getClass().getDeclaredField("price");
                    priceField.setAccessible(true);
                    Object price = priceField.get(orderItem);
                    if (price != null) {
                        return new BigDecimal(price.toString()); // String constructor is safe
                    }
                } catch (Exception e) {
                    // Continue to fetch from service
                }
            }

            // Fetch all product details at once (uses cache if available)
            Map<String, Object> product = getProductDetails(productId);
            if (product != null && product.containsKey("price")) {
                return new BigDecimal(product.get("price").toString()); // String constructor is safe
            }
        } catch (Exception e) {
            log.warn("Could not fetch price for product {}", productId, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Fetch seller ID from cache or product service
     * Uses cached data to avoid redundant API calls
     */
    private String getProductSellerId(String productId) {
        try {
            Map<String, Object> product = getProductDetails(productId);
            log.info("getProductSellerId for productId: {} - received product map: {}", productId, product);
            if (product != null && product.containsKey("sellerID")) {
                String sellerID = product.get("sellerID").toString();
                log.info("Found sellerID: {} for productId: {}", sellerID, productId);
                return sellerID;
            }
            log.warn("sellerID key not found in product map for productId: {}", productId);
        } catch (Exception e) {
            log.warn("Could not fetch sellerID for product {}", productId, e);
        }
        return null;
    }

    /**
     * Fetch product name from cache or product service
     * Uses cached data to avoid redundant API calls
     */
    private String getProductName(String productId) {
        try {
            Map<String, Object> product = getProductDetails(productId);
            if (product != null && product.containsKey("name")) {
                return product.get("name").toString();
            }
        } catch (Exception e) {
            log.warn("Could not fetch name for product {}", productId, e);
        }
        return "Unknown Product";
    }

    /**
     * Fetch ALL product details at once from cache or product service
     * This is a single API call per product that returns price, name, sellerId,
     * etc.
     * Significantly reduces API calls compared to fetching each field separately
     * OPTIMIZATION: Instead of 3+ separate calls, this makes 1 call and caches
     * result
     */
    private Map<String, Object> getProductDetails(String productId) {
        // Check cache first
        if (productCache.containsKey(productId)) {
            log.debug("Returning cached product for productId: {}", productId);
            return productCache.get(productId);
        }

        try {
            // Call the lightweight endpoint that returns only product DTO
            String url = PRODUCT_SERVICE_URL + "/api/products/simple/" + productId;
            log.info("Calling product-service URL: {}", url);

            org.springframework.http.ResponseEntity<Map> response = restTemplate.getForEntity(
                    url,
                    Map.class);

            Map<String, Object> product = response.getBody();

            log.info("Product service response body: {}", product);

            // Cache the result for future requests
            if (product != null) {
                productCache.put(productId, product);
                log.debug("Cached product details for productId: {}", productId);
            }

            return product;
        } catch (Exception e) {
            log.warn("Could not fetch product details for productId: {} - Error: {}", productId, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Initialize empty stats map
     */
    private Map<String, Object> initializeEmptyStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", 0);
        stats.put("totalSpent", BigDecimal.ZERO);
        stats.put("lastOrderDate", null);
        stats.put("mostPurchasedProductId", null);
        stats.put("mostPurchasedProductName", null);
        stats.put("mostPurchasedProductCount", 0);
        stats.put("totalQuantityBought", 0);
        return stats;
    }

    /**
     * Initialize empty seller stats map
     */
    private Map<String, Object> initializeEmptySellerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", BigDecimal.ZERO);
        stats.put("totalItemsSold", 0);
        stats.put("totalDeliveredOrders", 0);
        stats.put("totalCancelledOrders", 0);
        stats.put("totalUniqueCustomers", 0);
        stats.put("lastDeliveredDate", null);
        return stats;
    }
}
