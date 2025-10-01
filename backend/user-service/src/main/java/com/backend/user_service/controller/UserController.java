package com.backend.user_service.controller;

import com.backend.user_service.dto.registerUserDTO;
import com.backend.user_service.service.UserService;
import jakarta.validation.Valid; // Assuming you have validation
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> handleUserRegistration(
            @RequestPart("userDto") @Valid registerUserDTO userDto,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {

        // This assumes your DTO class has a method to convert it to a User entity
        // If not, you'll need to create the User object manually here.
        userService.registerUser(userDto.ToUser(), avatarFile);

        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }
}