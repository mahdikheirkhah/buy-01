package com.backend.orders_service.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductInventoryClient productInventoryClient;
    private final OrderStatusScheduler orderStatusScheduler;

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
     * NOTE: Stock validation should be performed at the frontend/gateway level
     * by calling the product-service to verify availability before calling this
     * method.
     */
    public Order addItemToOrder(String orderId, OrderItem item) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        // Only allow modifications to PENDING orders
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot modify order in status: " + order.getStatus());
        }

        // Merge with existing entry when the product is already in the cart
        order.getItems().stream()
                .filter(existing -> existing.getProductId().equals(item.getProductId()))
                .findFirst()
                .ifPresentOrElse(existing -> existing.setQuantity(existing.getQuantity() + item.getQuantity()),
                        () -> order.getItems().add(item));

        return orderRepository.save(order);
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
