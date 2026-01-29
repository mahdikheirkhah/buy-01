package com.backend.user_service.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backend.common.dto.InfoUserDTO;
import com.backend.user_service.dto.updateUserDTO;
import com.backend.user_service.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid; // Assuming you have validation

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public UserController(UserService userService,
            AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> handleUserLogout(HttpServletResponse response) {
        response.addCookie(userService.generateEmptyCookie());
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @PostMapping("/newAvatar")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> handleUserNewAvatar(
            @RequestPart(value = "avatarFile", required = true) MultipartFile avatarFile,
            @RequestHeader("X-User-ID") String userId) {
        userService.AvatarUpdate(userId, avatarFile);
        return ResponseEntity.ok(Map.of("message", "Avatar updated successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<InfoUserDTO> getCurrentUser(@RequestHeader("X-User-ID") String userId) {
        InfoUserDTO user = userService.getMe(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/seller")
    public ResponseEntity<InfoUserDTO> getUsersByIds(@RequestParam String id) {
        InfoUserDTO seller = userService.getUserById(id);
        return ResponseEntity.ok(seller);
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, String>> updateMe(
            @Valid @RequestBody updateUserDTO userUpdatedInfo,
            @RequestHeader("X-User-ID") String userId,
            HttpServletResponse response) {
        UserService.UserUpdateResult result = userService.updateUserInfo(userId, userUpdatedInfo);
        if (result.newJwtNeeded()) {
            Cookie jwtCookie = userService.generateCookie(result.userEmail());
            response.addCookie(jwtCookie);
        }
        return ResponseEntity.ok(Map.of("message", "updated successfully"));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteUser(@RequestHeader("X-User-ID") String userId,
            @RequestParam String password) {
        userService.deleteUser(userId, password);
        return ResponseEntity.ok(Map.of("message", "user deleted successfully"));
    }

    @DeleteMapping("/avatar")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> deleteAvatar(@RequestHeader("X-User-ID") String userId) {
        userService.deleteAvatar(userId);
        return ResponseEntity.ok("avatar deleted successfully");
    }

    @GetMapping("/email")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InfoUserDTO> getUsersByEmail(@RequestParam String email) {
        InfoUserDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
}