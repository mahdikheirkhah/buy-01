package com.backend.user_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.common.dto.SellerProfileDTO;
import com.backend.user_service.service.SellerProfileService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Controller for Seller Profile endpoints
 * Note: Sellers are users with seller role, so this controller is in
 * user-service
 */
@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    /**
     * Get authenticated seller's profile
     * GET /api/sellers/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<SellerProfileDTO> getAuthenticatedSellerProfile(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        String userRole = request.getHeader(USER_ROLE_HEADER);

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Check if user is a seller
        if (userRole == null || !userRole.equals("SELLER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            SellerProfileDTO profile = sellerProfileService.getSellerProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get public seller profile by sellerId
     * GET /api/sellers/{sellerId}/profile
     */
    @GetMapping("/{sellerId}/profile")
    public ResponseEntity<SellerProfileDTO> getSellerProfile(@PathVariable String sellerId) {
        try {
            SellerProfileDTO profile = sellerProfileService.getSellerProfile(sellerId);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get seller's statistics
     * GET /api/sellers/{sellerId}/statistics
     */
    @GetMapping("/{sellerId}/statistics")
    public ResponseEntity<SellerProfileDTO> getSellerStatistics(@PathVariable String sellerId) {
        try {
            SellerProfileDTO statistics = sellerProfileService.getSellerStatistics(sellerId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update authenticated seller's profile
     * PUT /api/sellers/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<SellerProfileDTO> updateSellerProfile(
            @RequestBody SellerProfileDTO profileDTO,
            HttpServletRequest request) {

        String userId = request.getHeader(USER_ID_HEADER);
        String userRole = request.getHeader(USER_ROLE_HEADER);

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Check if user is a seller
        if (userRole == null || !userRole.equals("SELLER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            SellerProfileDTO updated = sellerProfileService.updateProfile(userId, profileDTO);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
