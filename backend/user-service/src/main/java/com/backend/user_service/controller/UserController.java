package com.backend.user_service.controller;

import com.backend.user_service.dto.registerUserDTO;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserRepository;
import com.backend.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/register")
    public ResponseEntity<String> handleUserRegistration(@RequestBody registerUserDTO user, @RequestPart("avatarFile") MultipartFile avatarFile) {
        userService.registerUser(user.ToUser(), avatarFile);
        return new ResponseEntity<>("User registered successfully", HttpStatus.OK);
    }

}
