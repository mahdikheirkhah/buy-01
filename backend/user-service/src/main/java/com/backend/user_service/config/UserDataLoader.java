package com.backend.user_service.config;

import com.backend.common.dto.Role;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("dev")
public class UserDataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    // Pre-defined seller IDs that other services can use
    public static final Long PRE_DEFINED_SELLER_1_ID = 1001L;
    public static final Long PRE_DEFINED_SELLER_2_ID = 1002L;
    public static final Long PRE_DEFINED_SELLER_3_ID = 1003L;
    public static final Long PRE_DEFINED_SELLER_4_ID = 1004L;
    public static final Long PRE_DEFINED_SELLER_5_ID = 1005L;

    @Override
    public void run(String... args) throws Exception {
        userRepository.deleteAll();

        // Create sellers with known IDs
        User seller1 = User.builder()
                .id(String.valueOf(PRE_DEFINED_SELLER_1_ID)) // Force the ID
                .firstName("IKEA")
                .lastName("IKEA")
                .email("IKEA@example.com")
                .password("password123")
                .role(Role.SELLER)
                .build();
        User seller2 = User.builder()
                .id(String.valueOf(PRE_DEFINED_SELLER_2_ID)) // Force the ID
                .firstName("Patagonia")
                .lastName("Patagonia")
                .email("Patagonia@example.com")
                .password("password123")
                .role(Role.SELLER)
                .build();
        User seller3 = User.builder()
                .id(String.valueOf(PRE_DEFINED_SELLER_2_ID)) // Force the ID
                .firstName("Patagonia")
                .lastName("Patagonia")
                .email("Patagonia@example.com")
                .password("password123")
                .role(Role.SELLER)
                .build();
        User seller4 = User.builder()
                .id(String.valueOf(PRE_DEFINED_SELLER_2_ID)) // Force the ID
                .firstName("Patagonia")
                .lastName("Patagonia")
                .email("Patagonia@example.com")
                .password("password123")
                .role(Role.SELLER)
                .build();
        User seller5 = User.builder()
                .id(String.valueOf(PRE_DEFINED_SELLER_2_ID)) // Force the ID
                .firstName("Patagonia")
                .lastName("Patagonia")
                .email("Patagonia@example.com")
                .password("password123")
                .role(Role.SELLER)
                .build();
        userRepository.saveAll(List.of(seller1, seller2));
        System.out.println("Demo sellers created with IDs: " +
                PRE_DEFINED_SELLER_1_ID + ", " + PRE_DEFINED_SELLER_2_ID);
    }
}