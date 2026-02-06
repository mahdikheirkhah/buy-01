package com.backend.common.repository;

import com.backend.common.entity.SellerProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for SellerProfile entity
 */
@Repository
public interface SellerProfileRepository extends MongoRepository<SellerProfile, String> {

    /**
     * Find seller profile by sellerId
     */
    Optional<SellerProfile> findBySellerId(String sellerId);

    /**
     * Delete profile by sellerId
     */
    void deleteBySellerId(String sellerId);
}
