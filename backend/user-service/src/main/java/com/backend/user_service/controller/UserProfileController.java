package com.backend.user_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.common.dto.UserProfileDTO;
import com.backend.user_service.service.UserProfileService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Controller for User Profile endpoints
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private static final String USER_ID_HEADER = "X-User-ID";

    /**
     * Get authenticated user's profile
     * GET /api/users/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getAuthenticatedUserProfile(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            UserProfileDTO profile = userProfileService.getUserProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get user's statistics (same as profile for now, but can be extended)
     * GET /api/users/{userId}/statistics
     */
    @GetMapping("/{userId}/statistics")
    public ResponseEntity<UserProfileDTO> getUserStatistics(@PathVariable String userId) {
        try {
            UserProfileDTO statistics = userProfileService.getUserStatistics(userId);
            return ResponseEntity.ok(statistics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update authenticated user's profile
     * PUT /api/users/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDTO> updateUserProfile(
            @RequestBody UserProfileDTO profileDTO,
            HttpServletRequest request) {

        String userId = request.getHeader(USER_ID_HEADER);

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            UserProfileDTO updated = userProfileService.updateProfile(userId, profileDTO);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
