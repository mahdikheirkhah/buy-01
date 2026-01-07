package com.backend.orders_service.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.backend.orders_service.dto.CreateOrderRequest;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    public Order createOrder(CreateOrderRequest req) {
        Order order = Order.builder()
                .userId(req.getUserId())
                .shippingAddress(req.getShippingAddress())
                .items(req.getItems())
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
        Pageable p = PageRequest.of(page, size);
        return orderRepository.findByUserId(userId, p);
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

        order.getItems().add(item);
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
}
