package com.backend.orders_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.orders_service.dto.CreateOrderRequest;
import com.backend.orders_service.dto.UpdateOrderStatusRequest;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(orderService.createOrder(req));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable String orderId) {
        Order o = orderService.getOrderById(orderId);
        return o == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(o);
    }

    @GetMapping("/user/{userId}")
    public Page<Order> getUserOrders(@PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.getOrdersByUserId(userId, page, size);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest req) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, req.getStatus()));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancel(@PathVariable String orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/redo")
    public ResponseEntity<Order> redo(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.redoOrder(orderId));
    }
}
