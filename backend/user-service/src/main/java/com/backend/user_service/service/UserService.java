package com.backend.user_service.service;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.Role;
import com.backend.common.exception.CustomException;
import com.backend.common.util.JwtUtil;
import com.backend.user_service.dto.loginUserDTO;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserMapper;
import com.backend.user_service.repository.UserRepository;

import jakarta.servlet.http.Cookie;

@Service
public class UserService implements UserDetailsService {
    @org.springframework.beans.factory.annotation.Value("${jwt.expiration}")
    private long expiration;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PasswordEncoder passwordEncoder;
    private final WebClient.Builder webClientBuilder;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private static final String msg_not_auth = "Not Authorized";

    @Autowired
    public UserService(UserRepository userRepository, KafkaTemplate<String, String> kafkaTemplate,
            PasswordEncoder passwordEncoder, WebClient.Builder webClientBuilder, JwtUtil jwtUtil,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.passwordEncoder = passwordEncoder;
        this.webClientBuilder = webClientBuilder;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    public User registerUser(User user, MultipartFile avatarFile) {
        if (checkUserExistence(user.getEmail()).isPresent()) {
            throw new CustomException("User already exists please go to the login page", HttpStatus.BAD_REQUEST);
        }
        if (user.getRole() == null) {
            user.setRole(Role.CLIENT);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setAvatarUrl(checkAvatarFile(user.getRole(), avatarFile));
        return userRepository.save(user);
    }

    private Optional<User> checkUserExistence(String email) {

        return userRepository.findByEmail(email);
    }

    public InfoUserDTO getMe(String Id) {
        User user = userRepository.findById(Id)
                .orElseThrow(() -> new CustomException(msg_not_auth, HttpStatus.FORBIDDEN));

        return InfoUserDTO
                .builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private boolean checkPassword(String firstPassword, String secondPassword) {
        return !passwordEncoder.matches(firstPassword, secondPassword);
    }

    public void AvatarUpdate(String userID, MultipartFile avatarFile) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new CustomException(msg_not_auth, HttpStatus.FORBIDDEN));
        kafkaSendDeleteAvatar(user.getAvatarUrl());
        user.setAvatarUrl(checkAvatarFile(user.getRole(), avatarFile));
        userRepository.save(user);
    }

    private String checkAvatarFile(Role role, MultipartFile avatarFile) {
        if (role.equals(Role.SELLER) && avatarFile != null && !avatarFile.isEmpty()) {
            return saveAvatar(avatarFile);
        }
        return null;
    }

    private String saveAvatar(MultipartFile avatarFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        try {
            // ✅ Create a ByteArrayResource that WebClient can reliably send
            ByteArrayResource fileResource = new ByteArrayResource(avatarFile.getBytes()) {
                @Override
                public String getFilename() {
                    // This is crucial for the receiving service
                    return avatarFile.getOriginalFilename();
                }
            };

            body.add("file", fileResource);

        } catch (IOException e) {
            throw new CustomException("Failed to read avatar file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String mediaResponse = webClientBuilder.build().post()
                .uri("https://MEDIA-SERVICE/api/media/upload/avatar") // This is correct with @LoadBalanced
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(String.class)
                .block(); // .block() is fine for now

        if (mediaResponse == null || mediaResponse.isBlank()) {
            throw new CustomException("Failed to upload avatar image.", HttpStatus.BAD_REQUEST);
        }
        return mediaResponse;
    }

    public User loginUser(loginUserDTO loginUserDTO) {
        User user = checkUserExistence(loginUserDTO.getEmail())
                .orElseThrow(() -> new CustomException("wrong email or password", HttpStatus.BAD_REQUEST));
        if (checkPassword(loginUserDTO.getPassword(), user.getPassword())) {
            throw new CustomException("wrong email or password", HttpStatus.BAD_REQUEST);
        }
        return user;
    }

    public record UserUpdateResult(boolean newJwtNeeded, String userEmail) {
    }

    public UserUpdateResult updateUserInfo(String userID, updateUserDTO userUpdatedInfo) {

        User user = userRepository.findById(userID)
                .orElseThrow(() -> new CustomException(msg_not_auth, HttpStatus.FORBIDDEN));

        boolean newJwt = false;

        // --- 1. Non-sensitive updates (can be done without password) ---
        if (userUpdatedInfo.getFirstName() != null && !userUpdatedInfo.getFirstName().isBlank()) {
            user.setFirstName(userUpdatedInfo.getFirstName());
        }
        if (userUpdatedInfo.getLastName() != null && !userUpdatedInfo.getLastName().isBlank()) {
            user.setLastName(userUpdatedInfo.getLastName());
        }

        // --- 2. Check if any sensitive fields need to be updated ---
        boolean isEmailChange = userUpdatedInfo.getEmail() != null &&
                !userUpdatedInfo.getEmail().isBlank() &&
                !user.getEmail().equals(userUpdatedInfo.getEmail());

        boolean isPasswordChange = userUpdatedInfo.getNewPassword() != null &&
                !userUpdatedInfo.getNewPassword().isBlank();

        // If they are changing *either* email or password, they MUST provide the
        // correct current password
        if (isEmailChange || isPasswordChange) {

            // Check if current password was provided or is correct
            if (userUpdatedInfo.getCurrentPassword() == null
                    || checkPassword(userUpdatedInfo.getCurrentPassword(), user.getPassword())) {
                throw new CustomException("Invalid current password", HttpStatus.FORBIDDEN);
            }

            // Now that we're authenticated, perform the updates
            if (isEmailChange) {
                user.setEmail(userUpdatedInfo.getEmail());
                newJwt = true;
            }
            if (isPasswordChange) {
                user.setPassword(passwordEncoder.encode(userUpdatedInfo.getNewPassword()));
                newJwt = true;
            }
        }

        userRepository.save(user);

        // Always return the user's *final* email, in case it was changed
        return new UserUpdateResult(newJwt, user.getEmail());
    }

    public void updateUser(updateUserDTO userForm, String loggedInUserId, String userEmail, MultipartFile avatarFile) {
        User user = checkUpdateUser(loggedInUserId, userEmail);
        String oldAvatarUrl = user.getAvatarUrl();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = saveAvatar(avatarFile);
        }
        userMapper.updateUserFromDto(userForm, user);

    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        System.out.println(email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found with email: " + email, HttpStatus.NOT_FOUND));

        // ✅ FIX: Create an authority from the user's role
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(authority) // Add the user's real permission
        );
    }

    private User checkUpdateUser(String loggedInUserId, String userEmail) {
        User loggedInUser = userRepository.findById(loggedInUserId)
                .orElseThrow(() -> new CustomException("User not found with id: ", HttpStatus.FORBIDDEN));
        User updatesUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException("User not found with email: ", HttpStatus.FORBIDDEN));
        if (!updatesUser.getId().equals(loggedInUser.getId())) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return loggedInUser;
    }

    public InfoUserDTO getUserById(String id) {
        if (id == null || id.isBlank()) {
            throw new CustomException("User not found with ids", HttpStatus.NOT_FOUND);
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User not found with id: ", HttpStatus.NOT_FOUND));
        return InfoUserDTO
                .builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public InfoUserDTO getUserByEmail(String email) {
        User user = checkUserExistence(email)
                .orElseThrow(() -> new CustomException("User not found with email: " + email, HttpStatus.NOT_FOUND));
        return InfoUserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public void deleteUser(String id, String password) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("Access denied ", HttpStatus.FORBIDDEN));
        if (checkPassword(password, user.getPassword())) {
            throw new CustomException("Wrong password", HttpStatus.BAD_REQUEST);
        }
        if (user.getRole().equals(Role.SELLER)) {
            kafkaTemplate.send("user-deleted-topic", id);
            kafkaSendDeleteAvatar(user.getAvatarUrl());
        }
        userRepository.deleteById(user.getId());
    }

    public void deleteAvatar(String sellerId) {
        User user = userRepository.findById(sellerId)
                .orElseThrow(() -> new CustomException("Access denied ", HttpStatus.FORBIDDEN));
        kafkaSendDeleteAvatar(user.getAvatarUrl());
        user.setAvatarUrl(null);
        userRepository.save(user);
    }

    private void kafkaSendDeleteAvatar(String avatarUrl) {
        if (avatarUrl != null) {
            kafkaTemplate.send("user-avatar-deleted-topic", avatarUrl);
        }
    }

    public Cookie generateCookie(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("can not find the User", HttpStatus.NOT_FOUND));
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        String jwt = jwtUtil.generateToken(claims, user.getEmail());
        return createCookie(jwt, 24 * 60 * 60);
    }

    public Cookie generateEmptyCookie() {
        return createCookie(null, 0);
    }

    public Cookie createCookie(String token, int maxAge) {
        Cookie jwtCookie = new Cookie("jwt", token);
        // jwtCookie.setHttpOnly(true); // <-- Comment out or remove this line
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(maxAge);
        jwtCookie.setAttribute("SameSite", "Lax"); // Keep SameSite
        return jwtCookie;
    }

    // ... other methods XDD
}