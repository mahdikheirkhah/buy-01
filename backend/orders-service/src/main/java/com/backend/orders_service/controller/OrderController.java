package com.backend.orders_service.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.backend.common.dto.Role;
import com.backend.orders_service.dto.CheckoutRequest;
import com.backend.orders_service.dto.CreateOrderRequest;
import com.backend.orders_service.dto.UpdateOrderStatusRequest;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.service.OrderSearchService;
import com.backend.orders_service.service.OrderService;
import com.backend.orders_service.service.OrderStatsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
@Slf4j
public class OrderController {
    private final OrderService orderService;
    private final OrderStatsService orderStatsService;
    private final OrderSearchService orderSearchService;

    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String CLIENT_ROLE = "CLIENT";

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(orderService.createOrder(req));
    }

    @PostMapping("/{orderId}/checkout")
    public ResponseEntity<Order> checkout(@PathVariable String orderId,
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest httpRequest) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!hasAccessToOrder(order, httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Order checkedOut = orderService.checkoutOrder(orderId, request);
            return ResponseEntity.ok(checkedOut);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable String orderId, HttpServletRequest request) {
        log.info("=== ORDER DETAIL REQUEST START ===");
        log.info("orderId: {}", orderId);

        // Log all headers for debugging
        String userRole = request.getHeader(USER_ROLE_HEADER);
        String requestingUserId = request.getHeader(USER_ID_HEADER);
        log.info("X-User-Role header: '{}'", userRole);
        log.info("X-User-ID header: '{}'", requestingUserId);

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            log.warn("Order {} not found", orderId);
            return ResponseEntity.notFound().build();
        }

        log.info("Order found with {} items", order.getItems().size());

        // Handle seller order detail - check for both "ROLE_SELLER" and "SELLER"
        boolean isSeller = userRole != null && (userRole.equalsIgnoreCase("ROLE_SELLER")
                || userRole.equalsIgnoreCase("SELLER") || userRole.contains("SELLER"));

        log.info("Is seller check - userRole: '{}', isSeller: {}", userRole, isSeller);

        if (isSeller) {
            log.info("Processing as SELLER - Seller ID: {} requesting order detail for order {}", requestingUserId,
                    orderId);

            // Get seller's view of the order (only their items)
            com.backend.orders_service.dto.SellerOrderDTO sellerOrderDetail = orderSearchService
                    .getSellerOrderDetail(orderId, requestingUserId);

            log.info("SellerOrderDTO result: {}", sellerOrderDetail == null ? "NULL"
                    : "NOT NULL with " + sellerOrderDetail.getItems().size() + " items");

            if (sellerOrderDetail == null) {
                log.warn("UNAUTHORIZED - Seller {} has NO items in order {} - returning 403 FORBIDDEN",
                        requestingUserId, orderId);
                log.info("=== ORDER DETAIL REQUEST END (FORBIDDEN) ===");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        java.util.Map.of("error", "You don't have permission to view this order"));
            }

            log.info("AUTHORIZED - Returning order detail for seller {} with {} items", requestingUserId,
                    sellerOrderDetail.getItems().size());
            log.info("=== ORDER DETAIL REQUEST END (SUCCESS) ===");
            return ResponseEntity.ok(sellerOrderDetail);
        }

        log.info("Processing as CLIENT - Client {} requesting order detail", requestingUserId);
        // Handle client order detail
        if (!hasAccessToOrder(order, request)) {
            log.warn("CLIENT UNAUTHORIZED - {} does not own order {}", requestingUserId, orderId);
            log.info("=== ORDER DETAIL REQUEST END (FORBIDDEN) ===");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("CLIENT AUTHORIZED - Returning full order detail");
        log.info("=== ORDER DETAIL REQUEST END (SUCCESS) ===");
        return ResponseEntity.ok(order);
    }

    @GetMapping("/user/{userId}")
    public Object getUserOrders(@PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String minUpdateDate,
            @RequestParam(required = false) String maxUpdateDate,
            @RequestParam(required = false) List<OrderStatus> statuses,
            HttpServletRequest request) {

        // Get user role from header
        String userRole = request.getHeader("X-User-Role");
        String requestingUserId = request.getHeader("X-User-ID");

        // Check authorization
        if (!isAdmin(userRole) && !requestingUserId.equals(userId)) {
            return Page.empty();
        }

        // If user is SELLER, get seller orders with search/filters
        if (userRole != null && userRole.contains(Role.SELLER.toString())) {
            return orderSearchService.searchAndFilterSellerOrders(
                    userId,
                    keyword,
                    minPrice,
                    maxPrice,
                    minUpdateDate,
                    maxUpdateDate,
                    statuses,
                    page,
                    size);
        }

        // Otherwise, get client orders with search/filters (default behavior)
        return orderSearchService.searchAndFilterOrders(
                userId,
                keyword,
                minPrice,
                maxPrice,
                minUpdateDate,
                maxUpdateDate,
                statuses,
                page,
                size);
    }

    @GetMapping("/user/{userId}/cart")
    public ResponseEntity<Order> getActiveCart(@PathVariable String userId, HttpServletRequest request) {
        String requestingUserId = request.getHeader(USER_ID_HEADER);
        String userRole = request.getHeader(USER_ROLE_HEADER);

        if (!isAdmin(userRole) && !userId.equals(requestingUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Order> pendingCart = orderService.findLatestPendingOrder(userId);
        return pendingCart.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Get all orders - for statistics calculation (internal use only)
     * Only accessible from internal services
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_ADMIN')") // Restrict to admin or internal services
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> allOrders = orderService.getAllOrders();
        return ResponseEntity.ok(allOrders);
    }

    /**
     * Get statistics for a specific user
     * Fetches all orders for the user and calculates statistics
     * Returns: totalOrders, totalSpent, lastOrderDate, mostPurchasedProductId,
     * mostPurchasedProductName, mostPurchasedProductCount, totalQuantityBought
     */
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<java.util.Map<String, Object>> getUserStats(@PathVariable String userId) {
        java.util.Map<String, Object> stats = orderStatsService.calculateUserStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get statistics for a specific seller
     * Fetches all orders and filters by seller
     * Returns: totalRevenue, totalSales, totalOrders, totalCustomers, lastSaleDate
     */
    @GetMapping("/seller/{sellerId}/stats")
    public ResponseEntity<java.util.Map<String, Object>> getSellerStats(@PathVariable String sellerId) {
        java.util.Map<String, Object> stats = orderStatsService.calculateSellerStats(sellerId);
        return ResponseEntity.ok(stats);
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
    @PreAuthorize("hasRole('ROLE_CLIENT') || hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> cancel(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Check authorization - user must own the order
        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    java.util.Map.of("error", "You don't have permission to cancel this order"));
        }

        try {
            orderService.cancelOrder(orderId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            // Order is not in SHIPPING status
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of("error", e.getMessage()));
        } catch (com.backend.common.exception.CustomException e) {
            // Stock restoration failed
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/redo")
    @PreAuthorize("hasRole('ROLE_CLIENT') || hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> redo(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Check authorization
        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        com.backend.orders_service.dto.RedoOrderResponse response = orderService.redoOrder(orderId);

        // If no items could be added, return error response
        if (response.getOrder() == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.ok(response);
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

    @PutMapping("/{orderId}/items/{productId}")
    public ResponseEntity<Order> updateOrderItem(@PathVariable String orderId,
            @PathVariable String productId,
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
            Order updated = orderService.updateOrderItem(orderId, productId, item);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{orderId}/items/{productId}")
    public ResponseEntity<Order> removeItemFromOrder(@PathVariable String orderId,
            @PathVariable String productId,
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
            Order updated = orderService.removeItemFromOrder(orderId, productId);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{orderId}/items")
    public ResponseEntity<Order> clearOrderItems(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Order updated = orderService.clearOrderItems(orderId);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{orderId}/remove")
    @PreAuthorize("hasRole('ROLE_CLIENT') || hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> removeOrder(@PathVariable String orderId, HttpServletRequest request) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // Check authorization
        if (!hasAccessToOrder(order, request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            orderService.removeOrder(orderId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            // Order is not in DELIVERED or CANCELLED status
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    java.util.Map.of("error", e.getMessage()));
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