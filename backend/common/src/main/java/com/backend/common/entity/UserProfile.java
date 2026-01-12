package com.backend.common.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Profile Entity - Stores user statistics and preferences
 * Linked to User entity via userId
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_profiles")
public class UserProfile {

    @Id
    private String id;

    // Link to user
    private String userId;

    // Statistics
    private BigDecimal totalSpent; // Sum of all orders
    private Integer totalOrders; // Count of completed orders
    private Instant lastOrderDate; // Date of most recent order

    // Best products
    private String bestProductId; // Most frequently purchased product
    private String bestProductName; // Name of best product for quick access

    // Most bought category
    private String mostBoughtCategory; // Category with most purchases

    // Loyalty/Engagement
    private Integer loyaltyPoints; // Optional: loyalty points system
    private Boolean isActive; // Account active status

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;

    // Metadata
    private Integer preferredPaymentMethod; // 0 = Pay on Delivery, 1 = Card, etc.
    private List<String> savedAddresses; // List of saved shipping addresses
    private Integer totalReviews; // Count of product reviews written
    private Double averageRating; // Average rating given by user
}
