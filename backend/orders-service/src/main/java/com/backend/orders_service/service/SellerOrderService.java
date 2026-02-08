package com.backend.orders_service.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.orders_service.dto.SellerOrderDTO;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@Service
@RequiredArgsConstructor
public class SellerOrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private static final String PRODUCT_SERVICE_URL = "http://product-service"; // Eureka service discovery

    // Cache for product details to avoid redundant API calls
    private Map<String, Map<String, Object>> productCache = new HashMap<>();

    /**
     * Get all orders containing items from a specific seller
     * Orders are sorted by createdAt in descending order (newest to oldest)
     */
    public Page<SellerOrderDTO> getSellerOrders(String sellerId, int page, int size) {
        log.info("Getting orders for seller: {}", sellerId);

        // Get all non-pending orders
        List<Order> allOrders = orderRepository.findByStatusNot(com.backend.orders_service.model.OrderStatus.PENDING);
        log.info("Total non-pending orders in system: {}", allOrders.size());

        // Filter orders that contain items from this seller, sorted by createdAt
        // descending (newest first)
        List<SellerOrderDTO> sellerOrders = allOrders.stream()
                .map(order -> convertToSellerOrderDTO(order, sellerId))
                .filter(dto -> !dto.getItems().isEmpty()) // Only include orders with seller's items
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt())) // Sort newest to oldest
                .collect(Collectors.toList());

        log.info("Seller {} has {} orders with their products.", sellerId, sellerOrders.size());

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, sellerOrders.size());
        List<SellerOrderDTO> paginatedOrders = new ArrayList<>(sellerOrders.subList(start, end));

        return new PageImpl<>(paginatedOrders, PageRequest.of(page, size), sellerOrders.size());
    }

    /**
     * Convert Order to SellerOrderDTO, filtering items by sellerId
     * If sellerId is not stored in the item, fetches it from product service
     * Uses caching to avoid redundant API calls
     */
    private SellerOrderDTO convertToSellerOrderDTO(Order order, String sellerId) {
        log.info("Processing order {} with {} items for seller {}",
                order.getId(), order.getItems().size(), sellerId);

        // Filter items that belong to this seller
        List<OrderItem> sellerItems = order.getItems().stream()
                .filter(item -> {
                    String resolvedItemSellerId = resolveItemSellerId(item);
                    boolean matches = sellerId.equals(resolvedItemSellerId);
                    log.info("Item {} - resolvedSellerId: '{}', targetSellerId: '{}', match: {}",
                            item.getProductId(), resolvedItemSellerId, sellerId, matches);
                    return matches;
                })
                .collect(Collectors.toList());

        log.info("Order {} - found {} items for seller {}",
                order.getId(), sellerItems.size(), sellerId);

        return SellerOrderDTO.builder()
                .orderId(order.getId())
                .items(sellerItems)
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Resolve the seller ID for an order item
     * First checks if sellerId is stored in the item
     * If null/empty, fetches it from the product service using the product ID
     */
    private String resolveItemSellerId(OrderItem item) {
        String itemSellerId = item.getSellerId();
        String productId = item.getProductId();

        if (itemSellerId == null || itemSellerId.isEmpty()) {
            log.info("Item sellerId is EMPTY for productId: {}, fetching from product service", productId);
            return getProductSellerId(productId);
        }

        log.info("Item has sellerId: {} for productId: {}", itemSellerId, productId);
        return itemSellerId;
    }

    /**
     * Fetch seller ID from product service
     * Uses cached data to avoid redundant API calls
     */
    private String getProductSellerId(String productId) {
        try {
            Map<String, Object> product = getProductDetails(productId);
            log.info("getProductSellerId for productId: {} - received product map with keys: {}",
                    productId, product.keySet());

            if (!product.isEmpty() && product.containsKey("sellerID")) {
                String sellerID = product.get("sellerID").toString();
                log.info("Found sellerID: {} for productId: {}", sellerID, productId);
                return sellerID;
            }
            log.warn("sellerID key not found in product map for productId: {}", productId);
        } catch (Exception e) {
            log.warn("Could not fetch sellerID for product {}: {}", productId, e.getMessage());
        }
        return null;
    }

    /**
     * Fetch ALL product details from product service
     * Uses cache to avoid redundant API calls
     * Returns product details including price, name, and sellerID
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getProductDetails(String productId) {
        // Check cache first
        if (productCache.containsKey(productId)) {
            log.debug("Returning cached product for productId: {}", productId);
            return productCache.get(productId);
        }

        try {
            String url = PRODUCT_SERVICE_URL + "/api/products/simple/" + productId;
            log.info("Calling product-service URL: {}", url);

            org.springframework.http.ResponseEntity<Map<String, Object>> response = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .toEntity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            if (response != null && response.getBody() != null) {
                Map<String, Object> product = response.getBody();
                log.info("Product service response body: {}", product);

                // Cache the result for future requests
                productCache.put(productId, product);
                log.debug("Cached product details for productId: {}", productId);
                return product;
            }
        } catch (Exception e) {
            log.warn("Could not fetch product details for productId: {} - Error: {}",
                    productId, e.getMessage());
        }

        return java.util.Collections.emptyMap();
    }

}