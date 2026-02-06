package com.backend.orders_service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.backend.common.event.PaymentEvent;
import com.backend.common.event.PaymentEvent.PaymentStatus;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.model.PaymentMethod;
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

        // Update order status based on payment method
        if (order.getStatus() == OrderStatus.PENDING) {
            if (order.getPaymentMethod() == PaymentMethod.PAY_ON_DELIVERY) {
                // For COD, mark as DELIVERED immediately after payment confirmation
                order.setStatus(OrderStatus.DELIVERED);
                logger.info("Order {} status updated to DELIVERED (Pay on Delivery)", event.getOrderId());
            } else if (order.getPaymentMethod() == PaymentMethod.CARD) {
                // For card payments, move to PROCESSING for fulfillment
                order.setStatus(OrderStatus.PROCESSING);
                logger.info("Order {} status updated to PROCESSING (Card Payment)", event.getOrderId());
            }
            orderRepository.save(order);
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
