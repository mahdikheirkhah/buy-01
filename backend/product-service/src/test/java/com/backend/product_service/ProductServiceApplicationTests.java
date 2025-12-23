package com.backend.product_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/test",
        "spring.kafka.bootstrap-servers=",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.kafka.consumer.auto-startup=false",
        "spring.kafka.listener.auto-startup=false"
    }
)
@AutoConfigureDataMongo
@ActiveProfiles("test")
class ProductServiceApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the application context loads successfully
		// Using embedded MongoDB (Flapdoodle) for tests
	}
}
