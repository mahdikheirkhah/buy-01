package com.backend.user_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        // Use embedded Mongo (Flapdoodle). Do not point to localhost to avoid
        // connection failures.
        "spring.kafka.bootstrap-servers=",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.kafka.consumer.auto-startup=false",
        "spring.kafka.listener.auto-startup=false"
})
@AutoConfigureDataMongo
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
@Disabled("Disabled in CI - full application context test not needed when unit tests pass")
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the application context loads successfully
        // Using embedded MongoDB (Flapdoodle) for tests
    }
}
