package com.backend.product_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.mockito.Mockito;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/test",
        "spring.kafka.bootstrap-servers=",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.kafka.consumer.auto-startup=false",
        "spring.kafka.listener.auto-startup=false"
})
@AutoConfigureDataMongo
@ActiveProfiles("test")
class ProductServiceApplicationTests {

    /**
     * Provide a mock KafkaTemplate bean for unit tests
     * This prevents "No qualifying bean of type 'KafkaTemplate'" errors
     * when spring.kafka.bootstrap-servers is empty
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }

    @Test
    void contextLoads() {
        // This test verifies that the application context loads successfully
        // Using embedded MongoDB (Flapdoodle) for tests
    }
}
