package com.backend.user_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.backend.common.dto.Role;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserRepository;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password:CHANGE_ME_IN_PRODUCTION}")
    private String adminPassword;

    @Value("${app.admin.email:admin@admin.com}")
    private String adminEmail;

    @Autowired
    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if the admin user already exists
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            System.out.println(">>> Admin user not found, creating one...");

            // Warn if using default password
            if ("CHANGE_ME_IN_PRODUCTION".equals(adminPassword)) {
                System.err.println(
                        "⚠️  WARNING: Admin password is set to default! Set 'app.admin.password' environment variable or application.yml");
            }

            User adminUser = User
                    .builder()
                    .firstName("Admin")
                    .lastName("User")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(adminUser);
            System.out.println(">>> Admin user created successfully!");
        } else {
            System.out.println(">>> Admin user already exists.");
        }
    }
}