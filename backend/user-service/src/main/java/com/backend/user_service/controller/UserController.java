package com.backend.user_service.controller;

import com.backend.common.util.JwtUtil;
import com.backend.common.dto.InfoUserDTO;
import com.backend.user_service.dto.loginUserDTO;
import com.backend.user_service.dto.registerUserDTO;
import com.backend.user_service.dto.updateUserDTO;
import com.backend.user_service.model.User;
import com.backend.user_service.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid; // Assuming you have validation
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
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

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> handleUserRegistration(
            @RequestPart("userDto") @Valid registerUserDTO userDto,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {
        userService.registerUser(userDto.ToUser(), avatarFile);
        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> handleUserLogin(@RequestBody loginUserDTO loginUserDTO,
                                                               HttpServletResponse response) {
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

    @GetMapping("/me")
    public ResponseEntity<InfoUserDTO> getCurrentUser(@RequestHeader("X-User-ID") String userId) {
        InfoUserDTO user = userService.getMe(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/batch")
    public ResponseEntity<List<InfoUserDTO>> getUsersByIds(@RequestParam List<String> ids) {
        List<InfoUserDTO> users = userService.getUserByIds(ids);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/email")
    public ResponseEntity<InfoUserDTO> getUsersByEmail(@RequestParam String email) {
        InfoUserDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
    @PutMapping("/me")
    public ResponseEntity<Map<String, String>> updateMe(@Valid @RequestBody updateUserDTO user) {

    }

}