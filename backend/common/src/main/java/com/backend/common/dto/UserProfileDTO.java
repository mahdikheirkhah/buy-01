package com.backend.common.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for User Profile
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String avatarUrl;

    // Statistics
    private BigDecimal totalSpent;
    private Integer totalOrders;
    private Instant lastOrderDate;

    // Best products
    private String bestProductId;
    private String bestProductName;

    // Most bought category
    private String mostBoughtCategory;

    // Loyalty
    private Integer loyaltyPoints;

    // Metadata
    private List<String> savedAddresses;
    private Integer preferredPaymentMethod; // 0 = Pay on Delivery, 1 = Card, etc.
    private Integer totalReviews;
    private Double averageRating;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
