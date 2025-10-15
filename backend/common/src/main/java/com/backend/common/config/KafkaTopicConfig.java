package com.backend.common.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }

    // This bean will create the 'product-deleted-topic' if it doesn't exist.
    @Bean
    public NewTopic productDeletedTopic() {
        return new NewTopic("product-deleted-topic", 1, (short) 1);
    }

    // This bean will create the 'user-deleted-topic'
    @Bean
    public NewTopic userDeletedTopic() {
        return new NewTopic("user-deleted-topic", 1, (short) 1);
    }

    // This bean will create the 'user-avatar-deleted-topic'
    @Bean
    public NewTopic userAvatarDeletedTopic() {
        return new NewTopic("user-avatar-deleted-topic", 1, (short) 1);
    }
}