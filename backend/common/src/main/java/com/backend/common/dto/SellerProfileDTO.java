package com.backend.common.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Seller Profile
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerProfileDTO {

    private String sellerId;
    private String sellerName;
    private String shopLogoUrl;
    private String shopDescription;

    // Business Statistics
    private BigDecimal totalRevenue;
    private Integer totalSales;
    private Integer totalOrders;
    private Integer totalCustomers;

    // Best Selling Product
    private String bestSellingProductId;
    private String bestSellingProductName;
    private Integer bestSellingProductCount;

    // Ratings & Reviews
    private Double averageRating;
    private Integer totalReviews;
    private Integer totalFiveStarReviews;

    // Shop Info
    private Boolean isVerified;
    private Boolean isActive;

    // Performance Metrics
    private Double deliveryRating;
    private Double communicationRating;
    private Integer returnRate;
    private Integer cancellationRate;

    // Dates
    private Instant joinDate;
    private Instant lastSaleDate;

    // Metadata
    private List<String> categories;
    private Integer followerCount;
}
