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
 * Seller Profile Entity - Stores seller statistics and performance metrics
 * Linked to User entity via sellerId (where user has SELLER role)
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "seller_profiles")
public class SellerProfile {

    @Id
    private String id;

    // Link to seller user
    private String sellerId;
    private String sellerName; // Name for quick access

    // Business Statistics
    private BigDecimal totalRevenue; // Sum of all sales
    private Integer totalSales; // Count of items sold
    private Integer totalOrders; // Count of completed orders
    private Integer totalCustomers; // Count of unique customers

    // Best Performing Products
    private String bestSellingProductId; // Most sold product
    private String bestSellingProductName; // Name for quick access
    private Integer bestSellingProductCount;// Units sold of best product

    // Ratings & Reviews
    private Double averageRating; // Average seller rating (1-5)
    private Integer totalReviews; // Count of reviews received
    private Integer totalFiveStarReviews; // Count of 5-star reviews
    private Integer totalOneStarReviews; // Count of 1-star reviews

    // Shop Info
    private String shopDescription; // Seller's business description
    private String shopLogoUrl; // Logo/avatar URL
    private Boolean isVerified; // Seller verification status
    private Boolean isActive; // Shop active status

    // Performance Metrics
    private Double deliveryRating; // On-time delivery rating
    private Double communicationRating; // Responsiveness rating
    private Integer returnRate; // Return percentage (0-100)
    private Integer cancellationRate; // Cancellation percentage (0-100)

    // Dates
    private Instant joinDate; // When seller registered
    private Instant lastSaleDate; // Most recent order completion
    private Instant createdAt;
    private Instant updatedAt;

    // Metadata
    private List<String> categories; // Product categories seller sells
    private Integer followerCount; // Number of followers
}
