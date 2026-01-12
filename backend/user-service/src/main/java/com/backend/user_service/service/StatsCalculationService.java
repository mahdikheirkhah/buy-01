package com.backend.user_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for calculating user and seller statistics from orders
 * Single-pass calculation: iterate through all orders once to compute all stats
 * Handles both old orders (without price/sellerId) and new orders (with
 * price/sellerId)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsCalculationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String ORDERS_SERVICE_URL = "http://orders-service:8081"; // Internal Docker URL
    private static final String PRODUCT_SERVICE_URL = "http://product-service:8081"; // Internal Docker URL

    // Cache for product prices and seller IDs
    private Map<String, Map<String, Object>> productCache = new HashMap<>();

    /**
     * Calculate statistics for a specific user from all their orders
     * Returns: totalOrders, totalSpent, lastOrderDate
     * 
     * Single-pass algorithm:
     * 1. Fetch all orders
     * 2. Iterate once through all orders
     * 3. For each order:
     * - If order.userId == targetUserId:
     * - Sum up all item (price * quantity)
     * - Count orders
     * - Track latest order date
     */
    public Map<String, Object> calculateUserStats(String userId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<?> allOrdersData = restTemplate.getForObject(
                    ORDERS_SERVICE_URL + "/api/orders/all",
                    List.class);

            if (allOrdersData == null || allOrdersData.isEmpty()) {
                stats.put("totalOrders", 0);
                stats.put("totalSpent", BigDecimal.ZERO);
                stats.put("lastOrderDate", null);
                return stats;
            }

            BigDecimal totalSpent = BigDecimal.ZERO;
            int totalOrders = 0;
            Instant lastOrderDate = null;

            // Single-pass iteration
            for (Object orderObj : allOrdersData) {
                JsonNode orderNode = objectMapper.convertValue(orderObj, JsonNode.class);
                String orderUserId = orderNode.get("userId").asText();

                // Only process orders from this user
                if (!orderUserId.equals(userId)) {
                    continue;
                }

                totalOrders++;

                // Sum up all item prices
                JsonNode itemsNode = orderNode.get("items");
                if (itemsNode != null && itemsNode.isArray()) {
                    for (JsonNode item : itemsNode) {
                        BigDecimal price = getItemPrice(item);
                        int quantity = item.get("quantity").asInt();
                        totalSpent = totalSpent.add(price.multiply(BigDecimal.valueOf(quantity)));
                    }
                }

                // Track latest order date
                JsonNode orderDateNode = orderNode.get("orderDate");
                Instant orderDate;
                if (orderDateNode.isNumber()) {
                    orderDate = Instant.ofEpochMilli(orderDateNode.asLong());
                } else {
                    // Handle ISO format date string
                    orderDate = Instant.parse(orderDateNode.asText());
                }

                if (lastOrderDate == null || orderDate.isAfter(lastOrderDate)) {
                    lastOrderDate = orderDate;
                }
            }

            stats.put("totalOrders", totalOrders);
            stats.put("totalSpent", totalSpent);
            stats.put("lastOrderDate", lastOrderDate);

        } catch (Exception e) {
            log.error("Error calculating user stats for userId: {}", userId, e);
            stats.put("error", "Failed to calculate statistics");
            stats.put("totalOrders", 0);
            stats.put("totalSpent", BigDecimal.ZERO);
            stats.put("lastOrderDate", null);
        }

        return stats;
    }

    /**
     * Calculate statistics for a specific seller from all orders
     * Returns: totalRevenue, totalSales, totalOrders, totalCustomers, lastSaleDate
     * Handles both old orders (without sellerId in items) and new orders (with
     * sellerId)
     */
    public Map<String, Object> calculateSellerStats(String sellerId) {
        Map<String, Object> stats = new HashMap<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalSales = 0; // Total quantity of products sold
        Set<String> ordersSet = new HashSet<>(); // Track unique orders
        Set<String> customersSet = new HashSet<>(); // Track unique customers
        Instant lastSaleDate = null;

        try {
            List<?> allOrdersData = restTemplate.getForObject(
                    ORDERS_SERVICE_URL + "/api/orders/all",
                    List.class);

            if (allOrdersData == null || allOrdersData.isEmpty()) {
                stats.put("totalRevenue", BigDecimal.ZERO);
                stats.put("totalSales", 0);
                stats.put("totalOrders", 0);
                stats.put("totalCustomers", 0);
                stats.put("lastSaleDate", null);
                stats.put("deliveryRating", 5.0);
                stats.put("returnRate", 0.0);
                stats.put("cancellationRate", 0.0);
                return stats;
            }

            // SINGLE PASS through all orders
            for (Object orderObj : allOrdersData) {
                JsonNode orderNode = objectMapper.convertValue(orderObj, JsonNode.class);
                String orderId = orderNode.get("_id").asText(); // Use _id for MongoDB documents
                String userId = orderNode.get("userId").asText();

                JsonNode orderDateNode = orderNode.get("orderDate");
                Instant orderDate;
                if (orderDateNode.isNumber()) {
                    orderDate = Instant.ofEpochMilli(orderDateNode.asLong());
                } else {
                    orderDate = Instant.parse(orderDateNode.asText());
                }

                // Iterate through items in this order
                JsonNode itemsNode = orderNode.get("items");
                if (itemsNode != null && itemsNode.isArray()) {
                    for (JsonNode item : itemsNode) {
                        String itemSellerId = item.get("sellerId").asText(null);
                        String productId = item.get("productId").asText();

                        // If sellerId is missing, fetch it from the product
                        if (itemSellerId == null) {
                            itemSellerId = getProductSellerId(productId);
                        }

                        // Only process items from this seller
                        if (itemSellerId == null || !itemSellerId.equals(sellerId)) {
                            continue;
                        }

                        // Calculate revenue for this item
                        BigDecimal price = getItemPrice(item, productId);
                        int quantity = item.get("quantity").asInt();
                        totalRevenue = totalRevenue.add(price.multiply(BigDecimal.valueOf(quantity)));

                        // Count total sales (units sold)
                        totalSales += quantity;

                        // Track unique orders and customers
                        ordersSet.add(orderId);
                        customersSet.add(userId);

                        // Update last sale date
                        if (lastSaleDate == null || orderDate.isAfter(lastSaleDate)) {
                            lastSaleDate = orderDate;
                        }
                    }
                }
            }

            stats.put("totalRevenue", totalRevenue);
            stats.put("totalSales", totalSales);
            stats.put("totalOrders", ordersSet.size());
            stats.put("totalCustomers", customersSet.size());
            stats.put("lastSaleDate", lastSaleDate);

            // TODO: Calculate these from order reviews/return data
            stats.put("deliveryRating", 5.0);
            stats.put("returnRate", 0.0);
            stats.put("cancellationRate", 0.0);

        } catch (Exception e) {
            log.error("Error calculating seller stats for sellerId: {}", sellerId, e);
            stats.put("error", "Failed to calculate statistics");
            stats.put("totalRevenue", BigDecimal.ZERO);
            stats.put("totalSales", 0);
            stats.put("totalOrders", 0);
            stats.put("totalCustomers", 0);
            stats.put("lastSaleDate", null);
            stats.put("deliveryRating", 5.0);
            stats.put("returnRate", 0.0);
            stats.put("cancellationRate", 0.0);
        }

        return stats;
    }

    /**
     * Helper method to get item price
     * First checks if price is in the item, otherwise fetches from product service
     */
    private BigDecimal getItemPrice(JsonNode item) {
        if (item.has("price") && !item.get("price").isNull()) {
            return new BigDecimal(item.get("price").asDouble());
        }
        String productId = item.get("productId").asText();
        return getProductPrice(productId);
    }

    /**
     * Helper method to get item price with productId fallback
     */
    private BigDecimal getItemPrice(JsonNode item, String productId) {
        if (item.has("price") && !item.get("price").isNull()) {
            return new BigDecimal(item.get("price").asDouble());
        }
        return getProductPrice(productId);
    }

    /**
     * Fetch product price from product service or cache
     */
    private BigDecimal getProductPrice(String productId) {
        try {
            if (productCache.containsKey(productId)) {
                Object price = productCache.get(productId).get("price");
                if (price != null) {
                    return new BigDecimal(price.toString());
                }
            }

            // Fetch from product service
            Map<String, Object> product = restTemplate.getForObject(
                    PRODUCT_SERVICE_URL + "/api/products/" + productId,
                    Map.class);

            if (product != null && product.containsKey("price")) {
                productCache.put(productId, product);
                return new BigDecimal(product.get("price").toString());
            }
        } catch (Exception e) {
            log.warn("Could not fetch price for product {}", productId, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Fetch seller ID from product service or cache
     */
    private String getProductSellerId(String productId) {
        try {
            if (productCache.containsKey(productId)) {
                Object sellerId = productCache.get(productId).get("sellerId");
                if (sellerId != null) {
                    return sellerId.toString();
                }
            }

            // Fetch from product service
            Map<String, Object> product = restTemplate.getForObject(
                    PRODUCT_SERVICE_URL + "/api/products/" + productId,
                    Map.class);

            if (product != null && product.containsKey("sellerId")) {
                productCache.put(productId, product);
                return product.get("sellerId").toString();
            }
        } catch (Exception e) {
            log.warn("Could not fetch sellerId for product {}", productId, e);
        }
        return null;
    }
}
