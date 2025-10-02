package com.backend.user_service.service;

import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.exception.CustomException;
import com.backend.user_service.dto.loginUserDTO;
import com.backend.user_service.model.Role;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserRepository;
import org.apache.kafka.common.config.types.Password;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PasswordEncoder passwordEncoder;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public UserService(UserRepository userRepository, KafkaTemplate<String, String> kafkaTemplate,  PasswordEncoder passwordEncoder, WebClient.Builder webClientBuilder) {
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.passwordEncoder = passwordEncoder;
        this.webClientBuilder = webClientBuilder;
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
        } else {
            if (avatarFile != null && !avatarFile.isEmpty()) {
                String avatarUrl = saveAvatar(avatarFile);
                user.setAvatarUrl(avatarUrl);
            }
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        kafkaTemplate.send("user-registered-topic", savedUser.getEmail());
        return savedUser;
    }

    private Optional<User> checkUserExistence(String email) {
        return  userRepository.findByEmail(email);
    }
    private boolean checkPassword(String firstPassword, String secondPassword){
        return passwordEncoder.matches(firstPassword, secondPassword);
    }
    private String saveAvatar(MultipartFile avatarFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", avatarFile.getResource());
        // Use WebClient to make a POST request to the media-service
        // Note: The URL uses the service name 'media-service', not 'localhost'. Eureka handles it.
        MediaUploadResponseDTO mediaResponse = webClientBuilder.build().post()
                .uri("http://media-service/api/media/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(MediaUploadResponseDTO.class)
                .block(); // .block() makes the call synchronous. A reactive chain is more advanced.

        if (mediaResponse == null || mediaResponse.getFileUrl() == null) {
            throw new CustomException("Failed to upload avatar image.", HttpStatus.BAD_REQUEST);
        }
        return mediaResponse.getFileUrl();
    }

    public void loginUser(loginUserDTO loginUserDTO) {
        User user = checkUserExistence(loginUserDTO.getEmail())
                .orElseThrow(()->new CustomException("wrong email or password", HttpStatus.BAD_REQUEST));
        if (!checkPassword(loginUserDTO.getPassword(), user.getPassword())){
            throw new CustomException("wrong email or password", HttpStatus.BAD_REQUEST);
        }
        // save the JWT in the cookie

    }


    // ... other methods
}