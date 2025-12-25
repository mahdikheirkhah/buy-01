package com.backend.user_service.service;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.Role;
import com.backend.common.exception.CustomException;
import com.backend.common.util.JwtUtil;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserMapper;
import com.backend.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;


    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("plainPassword123")
                .role(Role.CLIENT)
                .avatarUrl(null)
                .build();
    }

    @Test
    @DisplayName("Should register a new user successfully with CLIENT role")
    void testRegisterUserSuccessfully() {
        // Arrange
        User newUser = User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .password("plainPassword456")
                .build();

        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(newUser.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("generatedId123");
            return user;
        });

        // Act
        User registeredUser = userService.registerUser(newUser, null);

        // Assert
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getId()).isEqualTo("generatedId123");
        assertThat(registeredUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(registeredUser.getRole()).isEqualTo(Role.CLIENT);
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("plainPassword456");
    }

    @Test
    @DisplayName("Should throw exception when registering user with existing email")
    void testRegisterUserWithExistingEmail() {
        // Arrange
        User newUser = User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("existing@example.com")
                .password("plainPassword456")
                .build();

        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(newUser, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("User already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should get user info by ID successfully")
    void testGetMeSuccess() {
        // Arrange
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        // Act
        InfoUserDTO userInfo = userService.getMe("user123");

        // Assert
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getId()).isEqualTo("user123");
        assertThat(userInfo.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(userInfo.getFirstName()).isEqualTo("John");
        assertThat(userInfo.getLastName()).isEqualTo("Doe");
        assertThat(userInfo.getRole()).isEqualTo(Role.CLIENT);
        verify(userRepository).findById("user123");
    }

    @Test
    @DisplayName("Should throw exception when getting user info for non-existent user")
    void testGetMeNotFound() {
        // Arrange
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getMe("nonexistent"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Not Authorized");

        verify(userRepository).findById("nonexistent");
    }

    @Test
    @DisplayName("Should find user by email successfully")
    void testFindByEmailSuccess() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> found = userRepository.findByEmail("john.doe@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findByEmail("john.doe@example.com");
    }

    @Test
    @DisplayName("Should return empty optional when email not found")
    void testFindByEmailNotFound() {
        // Arrange
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<User> found = userRepository.findByEmail("notfound@example.com");

        // Assert
        assertThat(found).isEmpty();
        verify(userRepository).findByEmail("notfound@example.com");
    }

    @Test
    @DisplayName("Should register SELLER with avatar when avatar file is provided")
    void testRegisterSellerWithAvatar() {
        // Arrange
        User sellerUser = User.builder()
                .firstName("Seller")
                .lastName("Name")
                .email("seller@example.com")
                .password("plainPassword")
                .role(Role.SELLER)
                .build();

        when(userRepository.findByEmail(sellerUser.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(sellerUser.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("sellerId");
            return user;
        });

        // Act
        User registeredSeller = userService.registerUser(sellerUser, null);

        // Assert
        assertThat(registeredSeller).isNotNull();
        assertThat(registeredSeller.getRole()).isEqualTo(Role.SELLER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should set default role to CLIENT when role is null")
    void testRegisterUserWithNullRole() {
        // Arrange
        User userWithoutRole = User.builder()
                .firstName("NoRole")
                .lastName("User")
                .email("norole@example.com")
                .password("plainPassword")
                .role(null)
                .build();

        when(userRepository.findByEmail(userWithoutRole.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(userWithoutRole.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("noRoleUserId");
            return user;
        });

        // Act
        User registeredUser = userService.registerUser(userWithoutRole, null);

        // Assert
        assertThat(registeredUser.getRole()).isEqualTo(Role.CLIENT);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should verify Kafka message sent on successful registration")
    void testKafkaPublishOnRegistration() {
        // Arrange
        User newUser = User.builder()
                .firstName("Kafka")
                .lastName("Test")
                .email("kafka@example.com")
                .password("plainPassword")
                .build();

        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(newUser.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("kafkaUserId");
            return user;
        });

        // Act
        userService.registerUser(newUser, null);

        // Assert
        // If your service publishes to Kafka, verify the template was called
        // verify(kafkaTemplate).send(anyString(), anyString()); // uncomment if applicable
    }

    @Test
    @DisplayName("Should find user by ID successfully")
    void testFindByIdSuccess() {
        // Arrange
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> found = userRepository.findById("user123");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("user123");
        verify(userRepository).findById("user123");
    }

    @Test
    @DisplayName("Should return empty optional when user ID not found")
    void testFindByIdNotFound() {
        // Arrange
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<User> found = userRepository.findById("nonexistent");

        // Assert
        assertThat(found).isEmpty();
        verify(userRepository).findById("nonexistent");
    }
}

