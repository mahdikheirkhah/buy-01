package com.backend.user_service.controller;

import com.backend.common.util.JwtUtil;
import com.backend.user_service.dto.loginUserDTO;
import com.backend.user_service.dto.registerUserDTO;
import com.backend.user_service.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid; // Assuming you have validation
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    @Autowired
    public UserController(UserService userService,  AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
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
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> handleUserLogin(@RequestBody loginUserDTO loginUserDTO, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginUserDTO.getEmail(), loginUserDTO.getPassword())
        );
        Cookie jwtCookie = userService.generateCookie(loginUserDTO.getEmail());
        response.addCookie(jwtCookie);
        return ResponseEntity.ok(Map.of("message", "Login successful"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> handleUserLogout(HttpServletResponse response) {
        response.addCookie(userService.generateEmptyCookie());
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }


}