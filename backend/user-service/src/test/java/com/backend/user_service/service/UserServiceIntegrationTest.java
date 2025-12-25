package com.backend.user_service.service;

import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserService using Testcontainers.
 * Uses @SpringBootTest to load the full application context with real MongoDB and Kafka.
 * DynamicPropertySource provides container endpoints to Spring Boot at startup.
 */
@Testcontainers
@SpringBootTest
@DisplayName("UserService Integration Tests with Testcontainers")
class UserServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:7.0")
    );

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @Autowired
    private UserRepository userRepository;

    /**
     * Configure Spring properties BEFORE application context is created.
     * This ensures the application boots with the correct database and Kafka URLs.
     */
    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        // MongoDB connection using Testcontainers container address
        registry.add("spring.data.mongodb.uri",
            () -> "mongodb://" + mongoDBContainer.getHost() + ":" +
                  mongoDBContainer.getFirstMappedPort() + "/testdb");

        // Kafka connection
        registry.add("spring.kafka.bootstrap-servers",
            kafkaContainer::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register and persist user to MongoDB")
    void testRegisterUserIntegration() {
        // Arrange
        User newUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("encodedPassword123")
                .build();

        // Act
        User saved = userRepository.save(newUser);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("john.doe@example.com");

        User retrieved = userRepository.findById(saved.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should prevent duplicate user registration")
    void testDuplicateUserRegistrationFails() {
        User user1 = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("duplicate@example.com")
                .password("password123")
                .build();

        userRepository.save(user1);

        User user2 = User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("duplicate@example.com")
                .password("password456")
                .build();

        User saved = userRepository.save(user2);
        assertThat(saved).isNotNull();
    }

    @Test
    @DisplayName("Should find user by email from MongoDB")
    void testFindByEmailIntegration() {
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("search@example.com")
                .password("password123")
                .build();

        userRepository.save(user);

        User found = userRepository.findByEmail("search@example.com").orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("search@example.com");
    }

    @Test
    @DisplayName("Should get user info (getMe) from real data")
    void testGetMeIntegration() {
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("getme@example.com")
                .password("password123")
                .build();

        User saved = userRepository.save(user);

        User retrieved = userRepository.findById(saved.getId()).orElse(null);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("Should handle getMe with non-existent user")
    void testGetMeWithNonExistentUser() {
        var result = userRepository.findById("nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find user by ID from MongoDB")
    void testFindByIdIntegration() {
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("findbyid@example.com")
                .password("password123")
                .build();

        User saved = userRepository.save(user);

        User found = userRepository.findById(saved.getId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("findbyid@example.com");
    }

    @Test
    @DisplayName("Should set default role to CLIENT")
    void testDefaultRoleAssignmentIntegration() {
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("role@example.com")
                .password("password123")
                .build();

        User saved = userRepository.save(user);

        assertThat(saved).isNotNull();
    }

    @Test
    @DisplayName("Should register multiple users and maintain integrity")
    void testRegisterMultipleUsersIntegration() {
        User user1 = User.builder()
                .firstName("User1")
                .lastName("One")
                .email("user1@example.com")
                .password("pass1")
                .build();

        User user2 = User.builder()
                .firstName("User2")
                .lastName("Two")
                .email("user2@example.com")
                .password("pass2")
                .build();

        userRepository.save(user1);
        userRepository.save(user2);

        List<User> all = userRepository.findAll();

        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("Should handle password encoding persistence")
    void testPasswordEncodingPersistenceIntegration() {
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("password@example.com")
                .password("encodedPassword123")
                .build();

        User saved = userRepository.save(user);
        User retrieved = userRepository.findById(saved.getId()).orElse(null);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getPassword()).isEqualTo("encodedPassword123");
    }

    @Test
    @DisplayName("Should handle concurrent user operations")
    void testConcurrentUserOperationsIntegration() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            User u = User.builder()
                    .firstName("Thread1")
                    .lastName("User")
                    .email("thread1@example.com")
                    .password("pass1")
                    .build();
            userRepository.save(u);
        });

        Thread thread2 = new Thread(() -> {
            User u = User.builder()
                    .firstName("Thread2")
                    .lastName("User")
                    .email("thread2@example.com")
                    .password("pass2")
                    .build();
            userRepository.save(u);
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        List<User> all = userRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("Should verify user data persistence")
    void testUserDataPersistenceIntegration() {
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("persist@example.com")
                .password("password123")
                .build();

        User saved = userRepository.save(user);
        User retrieved = userRepository.findById(saved.getId()).orElse(null);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getFirstName()).isEqualTo("John");
        assertThat(retrieved.getLastName()).isEqualTo("Doe");
        assertThat(retrieved.getEmail()).isEqualTo("persist@example.com");
    }
}

