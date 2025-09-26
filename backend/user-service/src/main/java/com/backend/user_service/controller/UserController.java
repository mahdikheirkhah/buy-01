package com.backend.user_service.controller;

import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserRepository;
import com.backend.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    @Autowired
    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public void handleUserRegistration(@RequestBody User user) {
        userService.registerUser(user);
    }
}
