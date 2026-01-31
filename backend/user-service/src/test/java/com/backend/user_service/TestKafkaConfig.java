package com.backend.user_service;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Test configuration class that provides mock Kafka beans for unit tests.
 * This class is separate from the test class to comply with Spring Boot's
 * requirement that @SpringBootTest classes cannot define @Bean methods.
 */
@Configuration
public class TestKafkaConfig {

    /**
     * Provide a mock KafkaTemplate bean for unit tests.
     * This prevents "No qualifying bean of type 'KafkaTemplate'" errors
     * when spring.kafka.bootstrap-servers is empty during testing.
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }
}
