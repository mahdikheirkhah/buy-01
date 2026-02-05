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
    private static final String PRODUCT_SERVICE_URL = "http://product-service"; // Eureka service discovery
    private static final String PRICE_KEY = "price";

    // Cache for product details
    private Map<String, Map<String, Object>> productCache = new HashMap<>();

    /**
     * Calculate statistics for a specific user
     * ONLY counts DELIVERED orders
     * Returns: totalOrders, totalSpent, lastOrderDate, mostPurchasedProductId,
     * mostPurchasedProductName, mostPurchasedProductCount, totalQuantityBought
     */
    public Map<String, Object> calculateUserStats(String userId) {
        try {
            List<Order> userOrders = orderRepository.findByUserIdAndStatusOrderByOrderDateDesc(userId,
                    OrderStatus.DELIVERED);

            if (userOrders == null || userOrders.isEmpty()) {
                return initializeEmptyStats();
            }

            UserStatsAccumulator accumulator = new UserStatsAccumulator();
            processUserOrders(userOrders, accumulator);
            return accumulator.toMap();

        } catch (Exception e) {
            log.error("Error calculating user stats for userId: {}", userId, e);
            return initializeEmptyStats();
        }
    }

    private void processUserOrders(List<Order> orders, UserStatsAccumulator acc) {
        for (Order order : orders) {
            acc.totalOrders++;
            acc.updateLastOrderDate(order.getOrderDate());

            if (order.getItems() != null) {
                for (var item : order.getItems()) {
                    processUserOrderItem(item, acc);
                }
            }
        }
    }

    private void processUserOrderItem(Object item, UserStatsAccumulator acc) {
        String productId = getItemProductId(item);
        BigDecimal price = getItemPrice(item, productId);
        int quantity = getItemQuantity(item);

        acc.totalSpent = acc.totalSpent.add(price.multiply(BigDecimal.valueOf(quantity)));
        acc.totalQuantityBought += quantity;

        String productName = getItemProductName(item);
        if (productName == null || productName.isEmpty()) {
            productName = getProductName(productId);
        }

        acc.trackProduct(productId, productName, quantity);
    }

    private String getItemProductId(Object item) {
        try {
            java.lang.reflect.Method method = item.getClass().getMethod("getProductId");
            return (String) method.invoke(item);
        } catch (Exception e) {
            return "";
        }
    }

    private int getItemQuantity(Object item) {
        try {
            java.lang.reflect.Method method = item.getClass().getMethod("getQuantity");
            return (int) method.invoke(item);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getItemProductName(Object item) {
        try {
            java.lang.reflect.Method method = item.getClass().getMethod("getProductName");
            return (String) method.invoke(item);
        } catch (Exception e) {
            return null;
        }
    }

    private String getItemSellerId(Object item) {
        try {
            java.lang.reflect.Method method = item.getClass().getMethod("getSellerId");
            return (String) method.invoke(item);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Helper class to accumulate user statistics
     */
    private static class UserStatsAccumulator {
        BigDecimal totalSpent = BigDecimal.ZERO;
        int totalOrders = 0;
        Instant lastOrderDate = null;
        int totalQuantityBought = 0;
        Map<String, Integer> productQuantityMap = new HashMap<>();
        Map<String, String> productNameMap = new HashMap<>();
        String mostPurchasedProductId = null;
        String mostPurchasedProductName = null;
        int mostPurchasedQuantity = 0;

        void updateLastOrderDate(Instant orderDate) {
            if (lastOrderDate == null || orderDate.isAfter(lastOrderDate)) {
                lastOrderDate = orderDate;
            }
        }

        void trackProduct(String productId, String productName, int quantity) {
            int newQuantity = productQuantityMap.getOrDefault(productId, 0) + quantity;
            productQuantityMap.put(productId, newQuantity);
            productNameMap.put(productId, productName);

            if (newQuantity > mostPurchasedQuantity) {
                mostPurchasedQuantity = newQuantity;
                mostPurchasedProductId = productId;
                mostPurchasedProductName = productName;
            }
        }

        Map<String, Object> toMap() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalOrders", totalOrders);
            stats.put("totalSpent", totalSpent);
            stats.put("lastOrderDate", lastOrderDate);
            stats.put("mostPurchasedProductId", mostPurchasedProductId);
            stats.put("mostPurchasedProductName", mostPurchasedProductName);
            stats.put("mostPurchasedProductCount", mostPurchasedQuantity);
            stats.put("totalQuantityBought", totalQuantityBought);
            return stats;
        }
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
        try {
            List<Order> allOrders = orderRepository.findAll();

            if (allOrders == null || allOrders.isEmpty()) {
                log.warn("No orders found in database");
                return initializeEmptySellerStats();
            }

            log.info("Calculating stats for seller: {} from {} total orders", sellerId, allOrders.size());

            SellerStatsAccumulator acc = new SellerStatsAccumulator();
            processSellerOrders(allOrders, sellerId, acc);

            log.info(
                    "Seller {} stats calculation - Checked {} items, matched {} items - Revenue: {}, Items: {}, Delivered Orders: {}, Cancelled Orders: {}, Customers: {}",
                    sellerId, acc.itemsChecked, acc.itemsMatched, acc.totalRevenue, acc.totalItemsSold,
                    acc.deliveredOrdersSet.size(), acc.cancelledOrdersSet.size(), acc.customersSet.size());

            return acc.toMap();

        } catch (Exception e) {
            log.error("Error calculating seller stats for sellerId: {}", sellerId, e);
            return initializeEmptySellerStats();
        }
    }

    private void processSellerOrders(List<Order> orders, String sellerId, SellerStatsAccumulator acc) {
        for (Order order : orders) {
            if (order.getItems() == null || order.getItems().isEmpty()) {
                continue;
            }
            for (var item : order.getItems()) {
                processSellerOrderItem(item, order, sellerId, acc);
            }
        }
    }

    private void processSellerOrderItem(Object item, Order order, String sellerId, SellerStatsAccumulator acc) {
        acc.itemsChecked++;
        String itemSellerId = resolveItemSellerId(item);
        String productId = getItemProductId(item);

        log.info("Item {} - productId: {}, sellerId: '{}', targetSellerId: '{}', status: {}, match: {}",
                acc.itemsChecked, productId, itemSellerId, sellerId, order.getStatus(),
                sellerId.equals(itemSellerId));

        if (!sellerId.equals(itemSellerId)) {
            return;
        }

        acc.itemsMatched++;
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.DELIVERED) {
            processDeliveredItem(item, order, acc);
        } else if (status == OrderStatus.CANCELLED) {
            acc.cancelledOrdersSet.add(order.getId());
        }
    }

    private String resolveItemSellerId(Object item) {
        String itemSellerId = getItemSellerId(item);
        String productId = getItemProductId(item);

        if (itemSellerId == null || itemSellerId.isEmpty()) {
            log.info("Item sellerId is EMPTY for productId: {}, fetching from product service", productId);
            return getProductSellerId(productId);
        }

        log.info("Item has sellerId: {} for productId: {}, verifying from product service", itemSellerId, productId);
        String productServiceSellerId = getProductSellerId(productId);
        if (productServiceSellerId != null) {
            log.info("Updated sellerId from product-service to: {}", productServiceSellerId);
            return productServiceSellerId;
        }
        return itemSellerId;
    }

    private void processDeliveredItem(Object item, Order order, SellerStatsAccumulator acc) {
        String productId = getItemProductId(item);
        BigDecimal price = getItemPrice(item, productId);
        int quantity = getItemQuantity(item);

        acc.totalRevenue = acc.totalRevenue.add(price.multiply(BigDecimal.valueOf(quantity)));
        acc.totalItemsSold += quantity;
        acc.deliveredOrdersSet.add(order.getId());
        acc.customersSet.add(order.getUserId());
        acc.updateLastDeliveredDate(order.getOrderDate());

        log.debug("Added item to seller stats - quantity: {}, price: {}, revenue increment: {}",
                quantity, price, price.multiply(BigDecimal.valueOf(quantity)));
    }

    /**
     * Helper class to accumulate seller statistics
     */
    private static class SellerStatsAccumulator {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalItemsSold = 0;
        Set<String> deliveredOrdersSet = new HashSet<>();
        Set<String> cancelledOrdersSet = new HashSet<>();
        Set<String> customersSet = new HashSet<>();
        Instant lastDeliveredDate = null;
        int itemsChecked = 0;
        int itemsMatched = 0;

        void updateLastDeliveredDate(Instant orderDate) {
            if (lastDeliveredDate == null || orderDate.isAfter(lastDeliveredDate)) {
                lastDeliveredDate = orderDate;
            }
        }

        Map<String, Object> toMap() {
            Map<String, Object> stats = new HashMap<>();
            int totalOrders = deliveredOrdersSet.size() + cancelledOrdersSet.size();

            double deliveryRating = 5.0;
            if (totalOrders > 0) {
                double deliveryRate = (double) deliveredOrdersSet.size() / totalOrders;
                deliveryRating = Math.max(1.0, 5.0 * deliveryRate);
            }

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
            return stats;
        }
    }

    /**
     * Fetch product price from cache or product service
     * Uses cached data when available to avoid redundant API calls
     */
    private BigDecimal getItemPrice(Object item, String productId) {
        try {
            // Handle JsonNode or OrderItem using pattern matching
            if (item instanceof JsonNode itemNode) {
                if (itemNode.has(PRICE_KEY) && !itemNode.get(PRICE_KEY).isNull()) {
                    return BigDecimal.valueOf(itemNode.get(PRICE_KEY).asDouble());
                }
            } else {
                // Handle OrderItem - try to extract price
                BigDecimal extractedPrice = extractPriceFromItem(item);
                if (extractedPrice != null) {
                    return extractedPrice;
                }
            }

            // Fetch all product details at once (uses cache if available)
            Map<String, Object> product = getProductDetails(productId);
            if (product != null && product.containsKey(PRICE_KEY)) {
                return new BigDecimal(product.get(PRICE_KEY).toString());
            }
        } catch (Exception e) {
            log.warn("Could not fetch price for product {}", productId, e);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Extract price from OrderItem object
     */
    private BigDecimal extractPriceFromItem(Object orderItem) {
        try {
            java.lang.reflect.Method getPriceMethod = orderItem.getClass().getMethod("getPrice");
            Object price = getPriceMethod.invoke(orderItem);
            if (price instanceof BigDecimal bd) {
                return bd;
            }
            if (price != null) {
                return new BigDecimal(price.toString());
            }
        } catch (Exception e) {
            log.debug("Could not extract price from item: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Fetch seller ID from cache or product service
     * Uses cached data to avoid redundant API calls
     */
    private String getProductSellerId(String productId) {
        try {
            Map<String, Object> product = getProductDetails(productId);
            log.info("getProductSellerId for productId: {} - received product map: {}", productId, product);
            if (!product.isEmpty() && product.containsKey("sellerID")) {
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
            if (!product.isEmpty() && product.containsKey("name")) {
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
    @SuppressWarnings("unchecked")
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

            org.springframework.http.ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> product = response.getBody();

            log.info("Product service response body: {}", product);

            // Cache the result for future requests
            if (product != null) {
                productCache.put(productId, product);
                log.debug("Cached product details for productId: {}", productId);
                return product;
            }
        } catch (Exception e) {
            log.warn("Could not fetch product details for productId: {} - Error: {}", productId, e.getMessage(), e);
        }

        return java.util.Collections.emptyMap();
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
