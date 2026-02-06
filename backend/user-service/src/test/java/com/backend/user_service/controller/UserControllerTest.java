package com.backend.user_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

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

import com.backend.common.dto.InfoUserDTO;
import com.backend.user_service.dto.UpdateUserDTO;
import com.backend.user_service.service.UserService;

import jakarta.servlet.http.Cookie;

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
                "test image content".getBytes());

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
                "test image content".getBytes());

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
                new byte[0]);

        doThrow(new IllegalArgumentException("Avatar file cannot be empty"))
                .when(userService).AvatarUpdate(anyString(), any());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userController.handleUserNewAvatar(emptyFile, userId);
        });
    }

    @Test
    void testGetCurrentUser_Success() {
        // Arrange
        InfoUserDTO userDTO = InfoUserDTO.builder()
                .id("user123")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();
        when(userService.getMe("user123")).thenReturn(userDTO);

        // Act
        ResponseEntity<InfoUserDTO> result = userController.getCurrentUser("user123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("john@example.com", result.getBody().getEmail());
        verify(userService).getMe("user123");
    }

    @Test
    void testGetUsersByIds_Success() {
        // Arrange
        InfoUserDTO userDTO = InfoUserDTO.builder()
                .id("user123")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();
        when(userService.getUserById("user123")).thenReturn(userDTO);

        // Act
        ResponseEntity<InfoUserDTO> result = userController.getUsersByIds("user123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("user123", result.getBody().getId());
        verify(userService).getUserById("user123");
    }

    @Test
    void testUpdateMe_Success() {
        // Arrange
        UpdateUserDTO updateDto = new UpdateUserDTO();
        updateDto.setFirstName("Jane");

        Cookie mockCookie = new Cookie("jwt", "token");
        UserService.UserUpdateResult updateResult = new UserService.UserUpdateResult(true, "jane@example.com");

        when(userService.updateUserInfo("user123", updateDto)).thenReturn(updateResult);
        when(userService.generateCookie("jane@example.com")).thenReturn(mockCookie);

        // Act
        ResponseEntity<Map<String, String>> result = userController.updateMe(updateDto, "user123", response);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("updated successfully", result.getBody().get("message"));
        verify(userService).updateUserInfo("user123", updateDto);
    }

    @Test
    void testDeleteUser_Success() {
        // Arrange
        doNothing().when(userService).deleteUser("user123", "password123");

        // Act
        ResponseEntity<Map<String, String>> result = userController.deleteUser("user123", "password123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("user deleted successfully", result.getBody().get("message"));
        verify(userService).deleteUser("user123", "password123");
    }

    @Test
    void testDeleteAvatar_Success() {
        // Arrange
        doNothing().when(userService).deleteAvatar("user123");

        // Act
        ResponseEntity<String> result = userController.deleteAvatar("user123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("avatar deleted successfully", result.getBody());
        verify(userService).deleteAvatar("user123");
    }

    @Test
    void testGetUsersByEmail_Success() {
        // Arrange
        InfoUserDTO userDTO = InfoUserDTO.builder()
                .id("user123")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();
        when(userService.getUserByEmail("john@example.com")).thenReturn(userDTO);

        // Act
        ResponseEntity<InfoUserDTO> result = userController.getUsersByEmail("john@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("john@example.com", result.getBody().getEmail());
        verify(userService).getUserByEmail("john@example.com");
    }

    // Removed registration test: UserController does not expose registration
    // endpoint.

    // Removed login success test: UserController does not expose login endpoint.

    // Removed login bad credentials test: UserController does not expose login
    // endpoint.
}