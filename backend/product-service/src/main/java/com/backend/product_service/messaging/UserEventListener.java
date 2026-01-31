package com.backend.product_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    @KafkaListener(topics = "user-registered-topic", groupId = "product-service-group")
    public void handleUserRegistration(String email) {
        log.info("Received user registration event for email: {}", email);

        // Here you could add logic to create a seller profile,
        // initialize product catalogs, etc.
    }
}