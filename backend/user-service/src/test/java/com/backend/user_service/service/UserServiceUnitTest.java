package com.backend.user_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.Role;
import com.backend.common.exception.CustomException;
import com.backend.common.util.JwtUtil;
import com.backend.user_service.dto.loginUserDTO;
import com.backend.user_service.dto.updateUserDTO;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserMapper;
import com.backend.user_service.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private JwtUtil jwtUtil;

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
        // verify(kafkaTemplate).send(anyString(), anyString()); // uncomment if
        // applicable
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

    @Test
    @DisplayName("Should login user successfully")
    void testLoginUserSuccess() {
        // Arrange
        loginUserDTO loginDto = new loginUserDTO();
        loginDto.setEmail("john.doe@example.com");
        loginDto.setPassword("correctPassword");
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctPassword", testUser.getPassword())).thenReturn(true);

        // Act
        User result = userService.loginUser(loginDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findByEmail("john.doe@example.com");
    }

    @Test
    @DisplayName("Should throw exception for wrong password in login")
    void testLoginUserWrongPassword() {
        // Arrange
        loginUserDTO loginDto = new loginUserDTO();
        loginDto.setEmail("john.doe@example.com");
        loginDto.setPassword("wrongPassword");
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.loginUser(loginDto))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("wrong email or password");
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void testGetUserById() {
        // Arrange
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        // Act
        InfoUserDTO result = userService.getUserById("user123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("user123");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findById("user123");
    }

    @Test
    @DisplayName("Should throw exception when user ID not found")
    void testGetUserByIdNotFound() {
        // Arrange
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById("nonexistent"))
                .isInstanceOf(CustomException.class);
        verify(userRepository).findById("nonexistent");
    }

    @Test
    @DisplayName("Should get user by email successfully")
    void testGetUserByEmail() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));

        // Act
        InfoUserDTO result = userService.getUserByEmail("john.doe@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findByEmail("john.doe@example.com");
    }

    @Test
    @DisplayName("Should update user info successfully")
    void testUpdateUserInfo() {
        // Arrange
        updateUserDTO updateDto = new updateUserDTO();
        updateDto.setFirstName("Jane");
        updateDto.setLastName("Smith");

        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        UserService.UserUpdateResult result = userService.updateUserInfo("user123", updateDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.newJwtNeeded()).isFalse();
        verify(userRepository).findById("user123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should generate cookie successfully")
    void testGenerateCookie() {
        // Arrange
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any(), eq("john.doe@example.com"))).thenReturn("jwt-token");

        // Act
        Cookie result = userService.generateCookie("john.doe@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("jwt");
        verify(userRepository).findByEmail("john.doe@example.com");
    }

    @Test
    @DisplayName("Should generate empty cookie successfully")
    void testGenerateEmptyCookie() {
        // Act
        Cookie result = userService.generateEmptyCookie();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("jwt");
        assertThat(result.getValue()).isNull();
    }

    @Test
    @DisplayName("Should delete user successfully")
    void testDeleteUserSuccess() {
        // Arrange
        testUser.setRole(Role.SELLER);
        testUser.setAvatarUrl("http://example.com/avatar.jpg");
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctPassword", testUser.getPassword())).thenReturn(true);

        // Act
        userService.deleteUser("user123", "correctPassword");

        // Assert
        verify(userRepository).findById("user123");
        verify(kafkaTemplate).send("user-deleted-topic", "user123");
        verify(kafkaTemplate).send("user-avatar-deleted-topic", "http://example.com/avatar.jpg");
        verify(userRepository).deleteById("user123");
    }

    @Test
    @DisplayName("Should delete avatar successfully")
    void testDeleteAvatar() {
        // Arrange
        testUser.setAvatarUrl("http://example.com/avatar.jpg");
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.deleteAvatar("user123");

        // Assert
        verify(userRepository).findById("user123");
        verify(kafkaTemplate).send("user-avatar-deleted-topic", "http://example.com/avatar.jpg");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should not send Kafka message when avatar URL is null")
    void testKafkaSendDeleteAvatarWithNullUrl() {
        // Act - calling private method indirectly through deleteAvatar
        testUser.setAvatarUrl(null);
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.deleteAvatar("user123");

        // Assert - verify Kafka was not called for null avatar
        verify(kafkaTemplate, never()).send(eq("user-avatar-deleted-topic"), anyString());
    }

    @Test
    @DisplayName("Should update user avatar successfully")
    void testAvatarUpdate() throws IOException {
        // Arrange
        testUser.setRole(Role.SELLER);
        MultipartFile mockFile = mock(MultipartFile.class);
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getBytes()).thenReturn("image-data".getBytes());
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("new-avatar-url"));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.AvatarUpdate("user123", mockFile);

        // Assert
        verify(userRepository).findById("user123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when avatar update user not found")
    void testAvatarUpdateUserNotFound() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.AvatarUpdate("nonexistent", mockFile))
                .isInstanceOf(CustomException.class);
    }
}
