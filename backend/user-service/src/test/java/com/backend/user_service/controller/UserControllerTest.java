package com.backend.user_service.controller;

import com.backend.user_service.model.User;
import com.backend.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserController userController;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
    }

    @Test
    void testHandleUserLogout_Success() {
        // Arrange
        when(userService.generateEmptyCookie()).thenReturn(new jakarta.servlet.http.Cookie("token", ""));

        // Act
        ResponseEntity<Map<String, String>> result = userController.handleUserLogout(response);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Logout successful", result.getBody().get("message"));
        verify(userService, times(1)).generateEmptyCookie();
    }

    @Test
    void testHandleUserNewAvatar_Success() {
        // Arrange
        String userId = "user123";
        MockMultipartFile avatarFile = new MockMultipartFile(
            "avatar",
            "avatar.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        doNothing().when(userService).AvatarUpdate(anyString(), any());

        // Act
        ResponseEntity<Map<String, String>> result = userController.handleUserNewAvatar(avatarFile, userId);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Avatar updated successfully", result.getBody().get("message"));
        verify(userService, times(1)).AvatarUpdate(userId, avatarFile);
    }

    @Test
    void testHandleUserNewAvatar_InvalidUserId() {
        // Arrange
        String invalidUserId = null;
        MockMultipartFile avatarFile = new MockMultipartFile(
            "avatar",
            "avatar.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        doThrow(new IllegalArgumentException("User ID cannot be null"))
            .when(userService).AvatarUpdate(isNull(), any());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userController.handleUserNewAvatar(avatarFile, invalidUserId);
        });
    }

    @Test
    void testHandleUserNewAvatar_EmptyFile() {
        // Arrange
        String userId = "user123";
        MockMultipartFile emptyFile = new MockMultipartFile(
            "avatar",
            "avatar.jpg",
            "image/jpeg",
            new byte[0]
        );

        doThrow(new IllegalArgumentException("Avatar file cannot be empty"))
            .when(userService).AvatarUpdate(anyString(), any());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userController.handleUserNewAvatar(emptyFile, userId);
        });
    }
}

