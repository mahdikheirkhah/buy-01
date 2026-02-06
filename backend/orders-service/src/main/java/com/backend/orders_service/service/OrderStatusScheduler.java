package com.backend.orders_service.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final long MIN_DELAY_MS = Duration.ofSeconds(10).toMillis();
    private static final long MAX_DELAY_MS = Duration.ofSeconds(30).toMillis();

    private final TaskScheduler taskScheduler;
    private final OrderRepository orderRepository;
    private final ProductInventoryClient productInventoryClient;

    /**
     * Schedules a post-checkout status update with random delay jitter.
     * Uses ThreadLocalRandom which is safe here because this is for load
     * distribution,
     * not security-critical (no tokens, keys, or secrets are generated).
     */
    @SuppressWarnings("java:S2245") // ThreadLocalRandom is safe for scheduling jitter
    public void schedulePostCheckoutUpdate(@NotNull String orderId) {
        long delay = ThreadLocalRandom.current().nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1);
        Instant runAt = Instant.now().plusMillis(delay);
        taskScheduler.schedule(() -> processOrder(orderId), runAt);
        log.debug("Scheduled status update for order {} in {} ms", orderId, delay);
    }

    private void processOrder(@NotNull String orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() != OrderStatus.SHIPPING) {
                log.debug("Skipping scheduled update for order {} because status is {}", orderId, order.getStatus());
                return;
            }

            OrderStatus nextStatus = pickNextStatus();
            if (nextStatus == OrderStatus.CANCELLED) {
                try {
                    productInventoryClient.increaseStock(order.getItems());
                } catch (Exception ex) {
                    log.error("Failed to restock products for cancelled order {}", orderId, ex);
                }
            }

            order.setStatus(nextStatus);
            order.setOrderDate(Instant.now());
            orderRepository.save(order);
            log.info("Order {} transitioned to {}", orderId, nextStatus);
        });
    }

    /**
     * Picks the next order status with 75% delivered, 25% cancelled probability.
     * Uses ThreadLocalRandom which is safe here because this is demo/simulation
     * logic,
     * not security-critical (no tokens, keys, or secrets are generated).
     */
    @SuppressWarnings("java:S2245") // ThreadLocalRandom is safe for status simulation
    private OrderStatus pickNextStatus() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        return roll < 75 ? OrderStatus.DELIVERED : OrderStatus.CANCELLED;
    }
}
