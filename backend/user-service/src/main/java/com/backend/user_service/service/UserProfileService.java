package com.backend.user_service.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.backend.common.dto.UserProfileDTO;
import com.backend.common.entity.UserProfile;
import com.backend.common.repository.UserProfileRepository;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for managing user profiles and statistics
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final StatsCalculationService statsCalculationService;

    /**
     * Get user profile by userId
     */
    public UserProfileDTO getUserProfile(String userId) {
        // Find user to get basic info
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Find or create user profile
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId, user));

        return mapToDTO(profile, user);
    }

    /**
     * Get user statistics (for dashboard)
     * Calculates fresh stats from all orders
     */
    public UserProfileDTO getUserStatistics(String userId) {
        // Find user to get basic info
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Find or create user profile
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId, user));

        // Calculate fresh statistics from orders
        Map<String, Object> statsMap = statsCalculationService.calculateUserStats(userId);

        // Update profile with calculated stats
        profile.setTotalOrders((Integer) statsMap.getOrDefault("totalOrders", 0));
        profile.setTotalSpent((java.math.BigDecimal) statsMap.getOrDefault("totalSpent", java.math.BigDecimal.ZERO));
        profile.setLastOrderDate((Instant) statsMap.get("lastOrderDate"));
        profile.setUpdatedAt(Instant.now());

        // Save updated profile
        userProfileRepository.save(profile);

        return mapToDTO(profile, user);
    }

    /**
     * Create a default profile for a new user
     */
    public UserProfile createDefaultProfile(String userId, User user) {
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .totalSpent(java.math.BigDecimal.ZERO)
                .totalOrders(0)
                .lastOrderDate(null)
                .bestProductId(null)
                .bestProductName(null)
                .mostBoughtCategory(null)
                .loyaltyPoints(0)
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .averageRating(0.0)
                .totalReviews(0)
                .build();

        return userProfileRepository.save(profile);
    }

    /**
     * Update user profile
     */
    public UserProfileDTO updateProfile(String userId, UserProfileDTO profileDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultProfile(userId, user));

        // Update fields
        if (profileDTO.getSavedAddresses() != null) {
            profile.setSavedAddresses(profileDTO.getSavedAddresses());
        }
        if (profileDTO.getPreferredPaymentMethod() != null) {
            profile.setPreferredPaymentMethod(profileDTO.getPreferredPaymentMethod());
        }

        profile.setUpdatedAt(Instant.now());
        UserProfile updated = userProfileRepository.save(profile);

        return mapToDTO(updated, user);
    }

    /**
     * Map UserProfile entity to DTO with user info
     */
    private UserProfileDTO mapToDTO(UserProfile profile, User user) {
        return UserProfileDTO.builder()
                .userId(profile.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .totalSpent(profile.getTotalSpent())
                .totalOrders(profile.getTotalOrders())
                .lastOrderDate(profile.getLastOrderDate())
                .bestProductId(profile.getBestProductId())
                .bestProductName(profile.getBestProductName())
                .mostBoughtCategory(profile.getMostBoughtCategory())
                .loyaltyPoints(profile.getLoyaltyPoints())
                .savedAddresses(profile.getSavedAddresses())
                .totalReviews(profile.getTotalReviews())
                .averageRating(profile.getAverageRating())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    /**
     * Update profile statistics (called from other services)
     * This would be called by orders-service when an order is completed
     */
    public void updateProfileStats(String userId, java.math.BigDecimal orderAmount) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);

        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();

            // Update totals
            profile.setTotalSpent(profile.getTotalSpent().add(orderAmount));
            profile.setTotalOrders(profile.getTotalOrders() + 1);
            profile.setLastOrderDate(Instant.now());
            profile.setUpdatedAt(Instant.now());

            userProfileRepository.save(profile);
        }
    }
}
