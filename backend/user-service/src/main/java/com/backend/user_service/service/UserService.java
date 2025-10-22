package com.backend.user_service.service;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.exception.CustomException;
import com.backend.common.util.JwtUtil;
import com.backend.user_service.dto.loginUserDTO;
import com.backend.user_service.dto.updateUserDTO;
import com.backend.user_service.model.Role;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserMapper;
import com.backend.user_service.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import lombok.Value;
import org.mapstruct.control.MappingControl;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.*;

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
    @Autowired
    public UserService(UserRepository userRepository, KafkaTemplate<String, String> kafkaTemplate,  PasswordEncoder passwordEncoder, WebClient.Builder webClientBuilder,  JwtUtil jwtUtil,  UserMapper userMapper) {
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

        if (user.getRole() == null){
            user.setRole(Role.CLIENT);
        }

        if (user.getRole() == Role.CLIENT){
            user.setAvatarUrl(null);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        //kafkaTemplate.send("user-registered-topic", savedUser.getEmail());
        if (savedUser.getRole() == Role.SELLER && avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = saveAvatar(avatarFile);
            savedUser.setAvatarUrl(avatarUrl);
        }
        return userRepository.save(savedUser);
    }

    private Optional<User> checkUserExistence(String email) {

        return  userRepository.findByEmail(email);
    }
    public InfoUserDTO getMe(String Id) {
        User user = userRepository.findById(Id)
                .orElseThrow(()-> new CustomException("Not Authorized", HttpStatus.FORBIDDEN));

        return  InfoUserDTO
                .builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
    private boolean checkPassword(String firstPassword, String secondPassword){
        return passwordEncoder.matches(firstPassword, secondPassword);
    }
    private String saveAvatar(MultipartFile avatarFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", avatarFile.getResource());
        String mediaResponse = webClientBuilder.build().post()
                .uri("https://media-service/api/media/upload/avatar")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(String.class)
                .block(); // .block() makes the call synchronous. A reactive chain is more advanced.

        if (mediaResponse == null || mediaResponse.isBlank()) {
            throw new CustomException("Failed to upload avatar image.", HttpStatus.BAD_REQUEST);
        }
        return mediaResponse;
    }

    public User loginUser(loginUserDTO loginUserDTO) {
        User user = checkUserExistence(loginUserDTO.getEmail())
                .orElseThrow(()->new CustomException("wrong email or password", HttpStatus.BAD_REQUEST));
        if (!checkPassword(loginUserDTO.getPassword(), user.getPassword())){
            throw new CustomException("wrong email or password", HttpStatus.BAD_REQUEST);
        }
        return user;
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

    private User checkUpdateUser(String loggedInUserId, String userEmail){
        User loggedInUser = userRepository.findById(loggedInUserId).
                orElseThrow(()-> new CustomException ("User not found with id: ", HttpStatus.FORBIDDEN));
        User updatesUser = userRepository.findByEmail(userEmail)
                .orElseThrow(()-> new CustomException ("User not found with email: ", HttpStatus.FORBIDDEN));
        if (!updatesUser.getId().equals(loggedInUser.getId())){
            throw new CustomException ("Access Denied", HttpStatus.FORBIDDEN);
        }
        return loggedInUser;
    }

    public List<InfoUserDTO> getUserByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new CustomException ("User not found with ids", HttpStatus.NOT_FOUND);
        }
        List<InfoUserDTO> users = new ArrayList<>();
        for (String id : ids) {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new CustomException ("User not found with id: " + id, HttpStatus.NOT_FOUND));

            users.add(InfoUserDTO
                    .builder()
                    .id(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .build());
        }
        return users;
    }
    public InfoUserDTO getUserByEmail(String email) {
        User user = checkUserExistence(email).
                orElseThrow(()->new CustomException ("User not found with email: " + email,  HttpStatus.NOT_FOUND));
        return InfoUserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(()-> new CustomException ("Access denied " , HttpStatus.FORBIDDEN));
        if (user.getRole().equals(Role.SELLER)){
            kafkaTemplate.send("user-deleted-topic", id);
            if(user.getAvatarUrl() != null){
                kafkaTemplate.send("user-avatar-deleted-topic", user.getAvatarUrl());
            }
        }
        userRepository.deleteById(user.getId());
    }
    public Cookie generateCookie(String email) {
        User user = userRepository.findByEmail(email)
                        .orElseThrow(()-> new CustomException("can not find the User", HttpStatus.NOT_FOUND));
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        String jwt = jwtUtil.generateToken(claims, user.getEmail());
        return createCookie(jwt, 24 * 60 * 60);
    }
    public Cookie generateEmptyCookie() {
        return createCookie(null, 0 );
    }
    public Cookie createCookie(String token, int maxAge) {
        Cookie jwtCookie = new Cookie("jwt", token);
//        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setAttribute("SameSite", "Lax");
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(maxAge); // 1 day
        return jwtCookie;
    }

    // ... other methods
}