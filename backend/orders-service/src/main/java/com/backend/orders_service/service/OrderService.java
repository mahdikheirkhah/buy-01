package com.backend.orders_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.backend.common.exception.CustomException;
import com.backend.orders_service.client.ProductInventoryClient;
import com.backend.orders_service.dto.CheckoutRequest;
import com.backend.orders_service.dto.CreateOrderRequest;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.model.PaymentMethod;
import com.backend.orders_service.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductInventoryClient productInventoryClient;
    private final OrderStatusScheduler orderStatusScheduler;
    private final RestTemplate restTemplate;
    private static final String PRODUCT_SERVICE_URL = "http://product-service"; // Eureka service discovery
    private static final String PRODUCT_CACHE_SIZE = "100"; // Max products to cache

    public Order createOrder(CreateOrderRequest req) {
        Order order = Order.builder()
                .userId(req.getUserId())
                .shippingAddress(req.getShippingAddress())
                .items(req.getItems() != null ? new ArrayList<>(req.getItems()) : new ArrayList<>())
                .paymentMethod(req.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .orderDate(Instant.now())
                .build();
        return orderRepository.save(order);
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    public Page<Order> getOrdersByUserId(String userId, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt", "orderDate", "createdAt");
        Pageable p = PageRequest.of(page, size, sort);
        return orderRepository.findByUserId(userId, p);
    }

    public Optional<Order> findLatestPendingOrder(String userId) {
        return orderRepository.findFirstByUserIdAndStatusOrderByOrderDateDesc(userId, OrderStatus.PENDING);
    }

    /**
     * Get all orders for statistics calculation
     * Used internally by stats services
     */
    public java.util.List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order updateOrderStatus(String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
        return orderRepository.save(order);
    }

    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    public Order redoOrder(String orderId) {
        Order existing = orderRepository.findById(orderId).orElseThrow();
        Order copy = Order.builder()
                .userId(existing.getUserId())
                .shippingAddress(existing.getShippingAddress())
                .items(existing.getItems())
                .paymentMethod(existing.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .orderDate(Instant.now())
                .build();
        return orderRepository.save(copy);
    }

    // ────────────────────────────────────────────────────────────────
    // ORDER ITEM MANAGEMENT
    // ────────────────────────────────────────────────────────────────

    /**
     * Add an item to an order.
     * Fetches product details (price, seller, name) once and populates OrderItem
     * NOTE: Stock validation should be performed at the frontend/gateway level
     * by calling the product-service to verify availability before calling this
     * method.
     */
    public Order addItemToOrder(String orderId, OrderItem item) {
        log.info("Adding item to order - orderId: {}, productId: {}, quantity: {}", orderId, item.getProductId(),
                item.getQuantity());

        Order order = orderRepository.findById(orderId).orElseThrow();

        // Only allow modifications to PENDING orders
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot modify order in status: " + order.getStatus());
        }

        // Populate product details if not already set
        if (item.getPrice() == null || item.getSellerId() == null || item.getProductName() == null) {
            log.info("Product details not set, fetching from product-service. Price: {}, SellerId: {}, ProductName: {}",
                    item.getPrice(), item.getSellerId(), item.getProductName());
            populateProductDetails(item);
        } else {
            log.info("Product details already set - Price: {}, SellerId: {}, ProductName: {}",
                    item.getPrice(), item.getSellerId(), item.getProductName());
        }

        // Merge with existing entry when the product is already in the cart
        order.getItems().stream()
                .filter(existing -> existing.getProductId().equals(item.getProductId()))
                .findFirst()
                .ifPresentOrElse(existing -> existing.setQuantity(existing.getQuantity() + item.getQuantity()),
                        () -> order.getItems().add(item));

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved successfully with {} items", savedOrder.getItems().size());
        return savedOrder;
    }

    /**
     * Fetch all product details at once from product-service
     * Populates price, sellerId, and productName in the OrderItem
     */
    private void populateProductDetails(OrderItem item) {
        try {
            String productUrl = PRODUCT_SERVICE_URL + "/api/products/" + item.getProductId();
            log.info("Fetching product details from: {}", productUrl);

            // Create headers with X-User-ID (required by product-service)
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-ID", "system-service"); // Use system user ID for internal service calls
            HttpEntity<String> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> product = restTemplate.exchange(productUrl, org.springframework.http.HttpMethod.GET,
                    entity, Map.class).getBody();

            if (product != null) {
                log.info("Product found: {}", product);

                // Set price
                if (product.containsKey("price")) {
                    Object priceObj = product.get("price");
                    item.setPrice(new BigDecimal(priceObj.toString())); // String constructor is safe
                    log.info("Set price: {} for productId: {}", priceObj, item.getProductId());
                }

                // Set sellerId
                if (product.containsKey("sellerId")) {
                    String sellerId = product.get("sellerId").toString();
                    item.setSellerId(sellerId);
                    log.info("Set sellerId: {} for productId: {}", sellerId, item.getProductId());
                }

                // Set product name
                if (product.containsKey("name")) {
                    String productName = product.get("name").toString();
                    item.setProductName(productName);
                    log.info("Set productName: {} for productId: {}", productName, item.getProductId());
                }

                log.info("Successfully populated product details for productId: {}", item.getProductId());
            } else {
                log.warn("Product not found from API for productId: {}", item.getProductId());
                if (item.getPrice() == null)
                    item.setPrice(BigDecimal.ZERO);
                if (item.getProductName() == null)
                    item.setProductName("Unknown Product");
                if (item.getSellerId() == null)
                    item.setSellerId("Unknown Seller");
            }
        } catch (Exception e) {
            log.error("Failed to fetch product details for productId: {}, Exception: {}", item.getProductId(),
                    e.getMessage(), e);
            // Set defaults if fetch fails
            if (item.getPrice() == null)
                item.setPrice(BigDecimal.ZERO);
            if (item.getProductName() == null)
                item.setProductName("Unknown Product");
            if (item.getSellerId() == null)
                item.setSellerId("Unknown Seller");
        }
    }

    /**
     * Update an existing item in an order.
     * NOTE: Stock validation should be performed at the frontend/gateway level
     * by calling the product-service to verify availability before calling this
     * method.
     */
    public Order updateOrderItem(String orderId, String productId, OrderItem updatedItem) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot modify order in status: " + order.getStatus());
        }

        // Find item by productId
        boolean found = false;
        for (int i = 0; i < order.getItems().size(); i++) {
            if (order.getItems().get(i).getProductId().equals(productId)) {
                order.getItems().set(i, updatedItem);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Product not found in order: " + productId);
        }

        return orderRepository.save(order);
    }

    public Order removeItemFromOrder(String orderId, String productId) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot modify order in status: " + order.getStatus());
        }

        // Remove item by productId
        boolean removed = order.getItems().removeIf(item -> item.getProductId().equals(productId));

        if (!removed) {
            throw new IllegalArgumentException("Product not found in order: " + productId);
        }

        return orderRepository.save(order);
    }

    public Order clearOrderItems(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot modify order in status: " + order.getStatus());
        }

        order.setItems(new ArrayList<>());
        return orderRepository.save(order);
    }

    public Order checkoutOrder(String orderId, CheckoutRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be checked out");
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty order");
        }

        order.setShippingAddress(request.getShippingAddress());
        order.setPaymentMethod(request.getPaymentMethod());

        if (request.getPaymentMethod() == PaymentMethod.CARD && !simulatePayment()) {
            throw new IllegalStateException("Payment was declined");
        }

        try {
            productInventoryClient.decreaseStock(order.getItems());
        } catch (WebClientResponseException e) {
            // Extract the error message from the product service response
            String errorMessage = extractErrorMessage(e);
            throw new CustomException(errorMessage, HttpStatus.BAD_REQUEST);
        }

        order.setStatus(OrderStatus.SHIPPING);
        order.setOrderDate(Instant.now());

        Order saved = orderRepository.save(order);
        orderStatusScheduler.schedulePostCheckoutUpdate(saved.getId());

        // Create a new empty PENDING order for the user's next cart
        // This clears the old cart after successful payment
        Order newCart = Order.builder()
                .userId(order.getUserId())
                .shippingAddress("")
                .items(new ArrayList<>())
                .paymentMethod(PaymentMethod.CARD)
                .status(OrderStatus.PENDING)
                .orderDate(Instant.now())
                .build();
        orderRepository.save(newCart);
        log.info("New empty cart created for user {} after checkout of order {}", order.getUserId(), orderId);

        return saved;
    }

    private boolean simulatePayment() {
        return ThreadLocalRandom.current().nextInt(100) < 80;
    }

    private String extractErrorMessage(WebClientResponseException e) {
        try {
            String responseBody = e.getResponseBodyAsString();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(responseBody);

            // Try to get message field first
            if (jsonNode.has("message")) {
                return jsonNode.get("message").asText();
            }
            // Fallback to error field
            if (jsonNode.has("error")) {
                return jsonNode.get("error").asText();
            }
            // Fallback to the full response
            return responseBody;
        } catch (Exception ex) {
            // If parsing fails, return the exception message
            return e.getMessage();
        }
    }
}
