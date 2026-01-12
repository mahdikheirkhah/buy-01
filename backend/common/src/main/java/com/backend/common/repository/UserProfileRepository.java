package com.backend.common.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.backend.common.entity.UserProfile;

/**
 * Repository for UserProfile entity
 */
@Repository
public interface UserProfileRepository extends MongoRepository<UserProfile, String> {

    /**
     * Find user profile by userId
     */
    Optional<UserProfile> findByUserId(String userId);

    /**
     * Delete profile by userId
     */
    void deleteByUserId(String userId);
}
