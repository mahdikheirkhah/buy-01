package com.backend.user_service.service;

import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Use constructor injection
    @Autowired
    public UserService(UserRepository userRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public User registerUser(User user) {
        User savedUser = userRepository.save(user);
        kafkaTemplate.send("user-registered-topic", savedUser.getEmail());

        return savedUser;
    }

    // ... other methods
}