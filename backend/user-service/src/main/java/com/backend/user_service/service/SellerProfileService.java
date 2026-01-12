package com.backend.user_service.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.backend.common.dto.SellerProfileDTO;
import com.backend.common.entity.SellerProfile;
import com.backend.common.repository.SellerProfileRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for managing seller profiles and performance metrics
 * Note: Sellers are users with seller role, so this service is in user-service
 */
@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final SellerProfileRepository sellerProfileRepository;

    /**
     * Get seller profile by sellerId
     */
    public SellerProfileDTO getSellerProfile(String sellerId) {
        SellerProfile profile = sellerProfileRepository.findBySellerId(sellerId)
                .orElseGet(() -> createDefaultProfile(sellerId));

        return mapToDTO(profile);
    }

    /**
     * Get seller statistics (for seller dashboard)
     */
    public SellerProfileDTO getSellerStatistics(String sellerId) {
        return getSellerProfile(sellerId);
    }

    /**
     * Create a default profile for a new seller
     */
    public SellerProfile createDefaultProfile(String sellerId) {
        SellerProfile profile = SellerProfile.builder()
                .sellerId(sellerId)
                .sellerName("New Seller")
                .totalRevenue(java.math.BigDecimal.ZERO)
                .totalSales(0)
                .totalOrders(0)
                .totalCustomers(0)
                .bestSellingProductId(null)
                .bestSellingProductName(null)
                .bestSellingProductCount(0)
                .averageRating(0.0)
                .totalReviews(0)
                .totalFiveStarReviews(0)
                .totalOneStarReviews(0)
                .isVerified(false)
                .isActive(true)
                .deliveryRating(0.0)
                .communicationRating(0.0)
                .returnRate(0)
                .cancellationRate(0)
                .joinDate(Instant.now())
                .lastSaleDate(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .followerCount(0)
                .build();

        return sellerProfileRepository.save(profile);
    }

    /**
     * Update seller profile info
     */
    public SellerProfileDTO updateProfile(String sellerId, SellerProfileDTO profileDTO) {
        SellerProfile profile = sellerProfileRepository.findBySellerId(sellerId)
                .orElseGet(() -> createDefaultProfile(sellerId));

        // Update fields
        if (profileDTO.getSellerName() != null) {
            profile.setSellerName(profileDTO.getSellerName());
        }
        if (profileDTO.getShopDescription() != null) {
            profile.setShopDescription(profileDTO.getShopDescription());
        }
        if (profileDTO.getShopLogoUrl() != null) {
            profile.setShopLogoUrl(profileDTO.getShopLogoUrl());
        }
        if (profileDTO.getCategories() != null) {
            profile.setCategories(profileDTO.getCategories());
        }

        profile.setUpdatedAt(Instant.now());
        SellerProfile updated = sellerProfileRepository.save(profile);

        return mapToDTO(updated);
    }

    /**
     * Update seller statistics (called from orders-service when order completes)
     */
    public void updateSalesStats(String sellerId, java.math.BigDecimal orderAmount, int itemCount) {
        Optional<SellerProfile> profileOpt = sellerProfileRepository.findBySellerId(sellerId);

        if (profileOpt.isPresent()) {
            SellerProfile profile = profileOpt.get();

            // Update totals
            profile.setTotalRevenue(profile.getTotalRevenue().add(orderAmount));
            profile.setTotalSales(profile.getTotalSales() + itemCount);
            profile.setTotalOrders(profile.getTotalOrders() + 1);
            profile.setLastSaleDate(Instant.now());
            profile.setUpdatedAt(Instant.now());

            sellerProfileRepository.save(profile);
        }
    }

    /**
     * Increment follower count
     */
    public void incrementFollowerCount(String sellerId) {
        Optional<SellerProfile> profileOpt = sellerProfileRepository.findBySellerId(sellerId);

        if (profileOpt.isPresent()) {
            SellerProfile profile = profileOpt.get();
            profile.setFollowerCount(profile.getFollowerCount() + 1);
            profile.setUpdatedAt(Instant.now());
            sellerProfileRepository.save(profile);
        }
    }

    /**
     * Decrement follower count
     */
    public void decrementFollowerCount(String sellerId) {
        Optional<SellerProfile> profileOpt = sellerProfileRepository.findBySellerId(sellerId);

        if (profileOpt.isPresent()) {
            SellerProfile profile = profileOpt.get();
            if (profile.getFollowerCount() > 0) {
                profile.setFollowerCount(profile.getFollowerCount() - 1);
                profile.setUpdatedAt(Instant.now());
                sellerProfileRepository.save(profile);
            }
        }
    }

    /**
     * Map SellerProfile entity to DTO
     */
    private SellerProfileDTO mapToDTO(SellerProfile profile) {
        return SellerProfileDTO.builder()
                .sellerId(profile.getSellerId())
                .sellerName(profile.getSellerName())
                .shopLogoUrl(profile.getShopLogoUrl())
                .shopDescription(profile.getShopDescription())
                .totalRevenue(profile.getTotalRevenue())
                .totalSales(profile.getTotalSales())
                .totalOrders(profile.getTotalOrders())
                .totalCustomers(profile.getTotalCustomers())
                .bestSellingProductId(profile.getBestSellingProductId())
                .bestSellingProductName(profile.getBestSellingProductName())
                .bestSellingProductCount(profile.getBestSellingProductCount())
                .averageRating(profile.getAverageRating())
                .totalReviews(profile.getTotalReviews())
                .totalFiveStarReviews(profile.getTotalFiveStarReviews())
                .isVerified(profile.getIsVerified())
                .isActive(profile.getIsActive())
                .deliveryRating(profile.getDeliveryRating())
                .communicationRating(profile.getCommunicationRating())
                .returnRate(profile.getReturnRate())
                .cancellationRate(profile.getCancellationRate())
                .joinDate(profile.getJoinDate())
                .lastSaleDate(profile.getLastSaleDate())
                .categories(profile.getCategories())
                .followerCount(profile.getFollowerCount())
                .build();
    }
}
