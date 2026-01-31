package com.backend.user_service.repository;

import com.backend.user_service.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User,String> {
    public Optional<User> findByEmail(String username);
}
