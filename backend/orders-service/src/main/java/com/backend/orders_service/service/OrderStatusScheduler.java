package com.backend.orders_service.service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.backend.orders_service.client.ProductInventoryClient;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.repository.OrderRepository;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusScheduler.class);

    // Configurable delays via application properties (in milliseconds)
    @Value("${app.order.status.min-delay-ms:30000}") // Default 30 seconds
    private long minDelayMs;

    @Value("${app.order.status.max-delay-ms:120000}") // Default 2 minutes
    private long maxDelayMs;

    private final TaskScheduler taskScheduler;
    private final OrderRepository orderRepository;
    private final ProductInventoryClient productInventoryClient;

    /**
     * Schedules a post-checkout status update with random delay jitter.
     * Delay is configurable via app.order.status.min-delay-ms and max-delay-ms
     * Uses ThreadLocalRandom which is safe here because this is for load
     * distribution,
     * not security-critical (no tokens, keys, or secrets are generated).
     */
    @SuppressWarnings("java:S2245") // ThreadLocalRandom is safe for scheduling jitter
    public void schedulePostCheckoutUpdate(@NotNull String orderId) {
        long delay = ThreadLocalRandom.current().nextLong(minDelayMs, maxDelayMs + 1);
        Instant runAt = Instant.now().plusMillis(delay);
        taskScheduler.schedule(() -> processOrder(orderId), runAt);
        log.debug("Scheduled status update for order {} in {} ms (range: {} - {})", orderId, delay, minDelayMs,
                maxDelayMs);
    }

    private void processOrder(@NotNull String orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() != OrderStatus.SHIPPING) {
                log.debug("Skipping scheduled update for order {} because status is {}", orderId, order.getStatus());
                return;
            }

            OrderStatus nextStatus = pickNextStatus();
            order.setStatus(nextStatus);
            order.setOrderDate(Instant.now());
            orderRepository.save(order);
            log.info("Order {} transitioned to {}", orderId, nextStatus);
        });
    }

    /**
     * Picks the next order status. Currently only returns DELIVERED.
     * Manual cancellation is handled through the cancel endpoint.
     * Uses ThreadLocalRandom which is safe here because this is demo/simulation
     * logic,
     * not security-critical (no tokens, keys, or secrets are generated).
     */
    @SuppressWarnings("java:S2245") // ThreadLocalRandom is safe for status simulation
    private OrderStatus pickNextStatus() {
        // Always return DELIVERED - cancellation is now handled manually via endpoint
        return OrderStatus.DELIVERED;
    }
}
