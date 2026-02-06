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
    private static final String CANNOT_MODIFY_ORDER_MSG = "Cannot modify order in status: ";

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
        return orderRepository.findByUserIdAndIsRemovedFalse(userId, p);
    }

    public Optional<Order> findLatestPendingOrder(String userId) {
        return orderRepository.findFirstByUserIdAndStatusOrderByOrderDateDesc(userId, OrderStatus.PENDING);
    }

    /**
     * Validates that the order is in PENDING status, throws exception otherwise.
     */
    private void validatePendingStatus(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(CANNOT_MODIFY_ORDER_MSG + order.getStatus());
        }
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

        // Check if order is removed
        if (order.isRemoved()) {
            throw new IllegalStateException("Cannot cancel a removed order");
        }

        // Only allow cancellation if order is in SHIPPING status
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new IllegalStateException(
                    "Order can only be cancelled when in SHIPPING status. Current status: " + order.getStatus());
        }

        // Restore stock for all items
        try {
            productInventoryClient.increaseStock(order.getItems());
            log.info("Successfully restored stock for cancelled order {}", orderId);
        } catch (Exception ex) {
            log.error("Failed to restore stock for cancelled order {}", orderId, ex);
            throw new CustomException("Failed to restore stock for order cancellation",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} has been cancelled", orderId);
    }

    public void removeOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        // Only allow removal if order is DELIVERED or CANCELLED
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Order can only be removed when in DELIVERED or CANCELLED status. Current status: "
                            + order.getStatus());
        }

        order.setRemoved(true);
        orderRepository.save(order);
        log.info("Order {} has been marked as removed", orderId);
    }

    public com.backend.orders_service.dto.RedoOrderResponse redoOrder(String orderId) {
        Order existing = orderRepository.findById(orderId).orElseThrow();

        // Check if order is removed
        if (existing.isRemoved()) {
            throw new IllegalStateException("Cannot reorder a removed order");
        }

        java.util.List<String> outOfStockProducts = new java.util.ArrayList<>();
        java.util.List<String> partiallyFilledProducts = new java.util.ArrayList<>();
        java.util.List<OrderItem> adjustedItems = new java.util.ArrayList<>();

        // Check stock availability for each item
        for (OrderItem originalItem : existing.getItems()) {
            try {
                ProductInventoryClient.ProductDetail productDetail = productInventoryClient
                        .getProductDetails(originalItem.getProductId());

                int availableQuantity = productDetail.getQuantity() != null ? productDetail.getQuantity() : 0;
                String productName = productDetail.getName() != null ? productDetail.getName()
                        : originalItem.getProductName();

                if (availableQuantity == 0) {
                    // Product is out of stock
                    outOfStockProducts.add(String.format("'%s' is out of stock", productName));
                } else if (availableQuantity < originalItem.getQuantity()) {
                    // Partial stock available
                    partiallyFilledProducts.add(String.format("'%s' has only %d available instead of %d",
                            productName, availableQuantity, originalItem.getQuantity()));

                    OrderItem adjustedItem = new OrderItem(
                            originalItem.getProductId(),
                            availableQuantity,
                            originalItem.getPrice(),
                            originalItem.getSellerId(),
                            productName);
                    adjustedItems.add(adjustedItem);
                } else {
                    // Full stock available - copy original item
                    OrderItem copiedItem = new OrderItem(
                            originalItem.getProductId(),
                            originalItem.getQuantity(),
                            originalItem.getPrice(),
                            originalItem.getSellerId(),
                            productName);
                    adjustedItems.add(copiedItem);
                }
            } catch (Exception e) {
                log.warn("Failed to check stock for product {}: {}", originalItem.getProductId(), e.getMessage());
                // If we can't check stock, treat as out of stock for safety
                outOfStockProducts.add(String.format("'%s' could not be verified (may be unavailable)",
                        originalItem.getProductName() != null ? originalItem.getProductName()
                                : originalItem.getProductId()));
            }
        }

        // Build response message
        String message;
        if (adjustedItems.isEmpty()) {
            message = "No items could be added to cart. All products are out of stock.";
        } else if (outOfStockProducts.isEmpty() && partiallyFilledProducts.isEmpty()) {
            message = "All items successfully added to cart";
        } else {
            message = "Some items could not be fully added to cart";
        }

        // Only create/update order if there are items to add
        Order order = null;
        if (!adjustedItems.isEmpty()) {
            // Find or create pending order for user
            Optional<Order> pendingOrder = findLatestPendingOrder(existing.getUserId());

            if (pendingOrder.isPresent()) {
                // Add items to existing pending order
                final Order existingOrder = pendingOrder.get();
                for (OrderItem newItem : adjustedItems) {
                    // Merge with existing items if same product
                    existingOrder.getItems().stream()
                            .filter(existingItem -> existingItem.getProductId().equals(newItem.getProductId()))
                            .findFirst()
                            .ifPresentOrElse(
                                    existingItem -> existingItem
                                            .setQuantity(existingItem.getQuantity() + newItem.getQuantity()),
                                    () -> existingOrder.getItems().add(newItem));
                }
                order = orderRepository.save(existingOrder);
            } else {
                // Create new pending order
                order = Order.builder()
                        .userId(existing.getUserId())
                        .shippingAddress(existing.getShippingAddress())
                        .items(adjustedItems)
                        .paymentMethod(existing.getPaymentMethod())
                        .status(OrderStatus.PENDING)
                        .orderDate(Instant.now())
                        .build();
                order = orderRepository.save(order);
            }
        }

        return com.backend.orders_service.dto.RedoOrderResponse.builder()
                .order(order)
                .message(message)
                .outOfStockProducts(outOfStockProducts)
                .partiallyFilledProducts(partiallyFilledProducts)
                .build();
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

        // Check if order is removed
        if (order.isRemoved()) {
            throw new IllegalStateException("Cannot add items to a removed order");
        }

        // Only allow modifications to PENDING orders
        validatePendingStatus(order);

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
            Map<String, Object> product = fetchProductFromService(item.getProductId());
            if (product != null) {
                applyProductDetails(item, product);
            } else {
                log.warn("Product not found from API for productId: {}", item.getProductId());
                setDefaultProductDetails(item);
            }
        } catch (Exception e) {
            log.error("Failed to fetch product details for productId: {}, Exception: {}", item.getProductId(),
                    e.getMessage(), e);
            setDefaultProductDetails(item);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchProductFromService(String productId) {
        String productUrl = PRODUCT_SERVICE_URL + "/api/products/" + productId;
        log.info("Fetching product details from: {}", productUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-ID", "system-service");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(productUrl, org.springframework.http.HttpMethod.GET,
                entity, Map.class).getBody();
    }

    private void applyProductDetails(OrderItem item, Map<String, Object> product) {
        log.info("Product found: {}", product);

        if (product.containsKey("price")) {
            Object priceObj = product.get("price");
            item.setPrice(new BigDecimal(priceObj.toString()));
            log.info("Set price: {} for productId: {}", priceObj, item.getProductId());
        }

        if (product.containsKey("sellerId")) {
            String sellerId = product.get("sellerId").toString();
            item.setSellerId(sellerId);
            log.info("Set sellerId: {} for productId: {}", sellerId, item.getProductId());
        }

        if (product.containsKey("name")) {
            String productName = product.get("name").toString();
            item.setProductName(productName);
            log.info("Set productName: {} for productId: {}", productName, item.getProductId());
        }

        log.info("Successfully populated product details for productId: {}", item.getProductId());
    }

    private void setDefaultProductDetails(OrderItem item) {
        if (item.getPrice() == null) {
            item.setPrice(BigDecimal.ZERO);
        }
        if (item.getProductName() == null) {
            item.setProductName("Unknown Product");
        }
        if (item.getSellerId() == null) {
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

        validatePendingStatus(order);

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

        validatePendingStatus(order);

        // Remove item by productId
        boolean removed = order.getItems().removeIf(item -> item.getProductId().equals(productId));

        if (!removed) {
            throw new IllegalArgumentException("Product not found in order: " + productId);
        }

        return orderRepository.save(order);
    }

    public Order clearOrderItems(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        validatePendingStatus(order);

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
            throw new IllegalStateException(
                    "Payment processing failed. Please check your card details and try again. If the problem persists, please contact your bank or try a different payment method.");
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

    /**
     * Simulates payment processing with 80% success rate.
     * Uses ThreadLocalRandom which is safe here because this is demo/simulation
     * logic,
     * not security-critical (no tokens, keys, or secrets are generated).
     */
    @SuppressWarnings("java:S2245") // ThreadLocalRandom is safe for non-security simulation
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