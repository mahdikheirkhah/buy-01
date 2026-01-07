package com.backend.orders_service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.backend.common.event.PaymentEvent;
import com.backend.common.event.PaymentEvent.PaymentStatus;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentEventListener {
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventListener.class);
    private final OrderRepository orderRepository;

    @KafkaListener(topics = "payment-events", groupId = "orders-service-group")
    public void handlePaymentEvent(PaymentEvent event) {
        logger.info("Received payment event for order: {} with status: {}", event.getOrderId(), event.getStatus());

        if (event.getStatus() == PaymentStatus.SUCCESS) {
            handlePaymentSuccess(event);
        } else if (event.getStatus() == PaymentStatus.FAILED) {
            handlePaymentFailure(event);
        }
    }

    private void handlePaymentSuccess(PaymentEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);

        if (order == null) {
            logger.warn("Order not found: {}", event.getOrderId());
            return;
        }

        // Update order status from PENDING to PROCESSING when payment is successful
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);
            logger.info("Order {} status updated to PROCESSING after successful payment", event.getOrderId());
        }
    }

    private void handlePaymentFailure(PaymentEvent event) {
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);

        if (order == null) {
            logger.warn("Order not found: {}", event.getOrderId());
            return;
        }

        // Optionally cancel or mark as failed if needed
        logger.warn("Payment failed for order: {}. Order status: {}", event.getOrderId(), order.getStatus());
    }
}
