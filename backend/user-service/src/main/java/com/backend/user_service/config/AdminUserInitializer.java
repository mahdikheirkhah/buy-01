package com.backend.user_service.config;

import com.backend.common.dto.Role;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@admin.com";

        // Check if the admin user already exists
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            System.out.println(">>> Admin user not found, creating one...");

            User adminUser = User
                    .builder()
                    .firstName("Admin")
                    .lastName("User")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.CLIENT)
                    .build();

            userRepository.save(adminUser);
            System.out.println(">>> Admin user created successfully!");
        } else {
            System.out.println(">>> Admin user already exists.");
        }
    }
}