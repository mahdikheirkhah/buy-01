package com.backend.orders_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;

    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String CLIENT_ROLE = "CLIENT";

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(orderService.createOrder(req));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable String orderId, HttpServletRequest request) {
        Order o = orderService.getOrderById(orderId);
        if (o == null) {
            return ResponseEntity.notFound().build();
        }

        // Check authorization
        if (!hasAccessToOrder(o, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(o);
    }

    @GetMapping("/user/{userId}")
    public Page<Order> getUserOrders(@PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        // Check authorization - only allow if requesting user is the same as userId or
        // is admin
        String requestingUserId = request.getHeader(USER_ID_HEADER);
        String userRole = request.getHeader(USER_ROLE_HEADER);

        if (!isAdmin(userRole) && !requestingUserId.equals(userId)) {
            return Page.empty();
        }

        return orderService.getOrdersByUserId(userId, page, size);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest req,
            HttpServletRequest request) {

        // Only admin can update order status
        String userRole = request.getHeader(USER_ROLE_HEADER);
        if (!isAdmin(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, req.getStatus()));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancel(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Check authorization
        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/redo")
    public ResponseEntity<Order> redo(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Check authorization
        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(orderService.redoOrder(orderId));
    }

    // ────────────────────────────────────────────────────────────────
    // ORDER ITEM MANAGEMENT
    // ────────────────────────────────────────────────────────────────

    @PostMapping("/{orderId}/items")
    public ResponseEntity<Order> addItemToOrder(@PathVariable String orderId,
            @Valid @RequestBody OrderItem item,
            HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Check authorization
        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Order updated = orderService.addItemToOrder(orderId, item);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{orderId}/items/{itemIndex}")
    public ResponseEntity<Order> updateOrderItem(@PathVariable String orderId,
            @PathVariable int itemIndex,
            @Valid @RequestBody OrderItem item,
            HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Check authorization
        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Order updated = orderService.updateOrderItem(orderId, itemIndex, item);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException | IndexOutOfBoundsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{orderId}/items/{itemIndex}")
    public ResponseEntity<Order> removeItemFromOrder(@PathVariable String orderId,
            @PathVariable int itemIndex,
            HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Check authorization
        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Order updated = orderService.removeItemFromOrder(orderId, itemIndex);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException | IndexOutOfBoundsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // ────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ────────────────────────────────────────────────────────────────

    /**
     * Check if the requester has access to the order.
     * Allows access if: user is order owner OR user is admin
     */
    private boolean hasAccessToOrder(Order order, HttpServletRequest request) {
        String requestingUserId = request.getHeader(USER_ID_HEADER);
        String userRole = request.getHeader(USER_ROLE_HEADER);

        return isAdmin(userRole) || requestingUserId.equals(order.getUserId());
    }

    private boolean isAdmin(String userRole) {
        return userRole != null && userRole.equalsIgnoreCase(ADMIN_ROLE);
    }
}
