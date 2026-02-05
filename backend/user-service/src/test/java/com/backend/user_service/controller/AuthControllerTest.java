package com.backend.user_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.backend.common.dto.Role;
import com.backend.user_service.dto.LoginUserDTO;
import com.backend.user_service.dto.registerUserDTO;
import com.backend.user_service.model.User;
import com.backend.user_service.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthController authController;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setup() {
        // No special setup required; fields injected by Mockito
    }

    @Test
    void testHandleUserLogin_Success() {
        // Arrange
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("user@example.com");
        dto.setPassword("secret123");

        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

        Cookie jwtCookie = new Cookie("jwt", "token-value");
        when(userService.generateCookie(eq("user@example.com"))).thenReturn(jwtCookie);

        // Act
        ResponseEntity<Map<String, String>> result = authController.handleUserLogin(dto, response);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Login successful", result.getBody().get("message"));
        verify(response, times(1)).addCookie(eq(jwtCookie));
        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testHandleUserLogin_BadCredentials() {
        // Arrange
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("bad@example.com");
        dto.setPassword("wrong");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad creds"));

        // Act
        ResponseEntity<Map<String, String>> result = authController.handleUserLogin(dto, response);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals("Invalid email or password", result.getBody().get("error"));
        verify(response, never()).addCookie(any());
        verify(userService, never()).generateCookie(anyString());
    }

    @Test
    void testHandleUserRegistration_Success() {
        // Arrange
        registerUserDTO reg = registerUserDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("secret123")
                .role(Role.CLIENT)
                .build();

        // Act
        ResponseEntity<Map<String, String>> result = authController.handleUserRegistration(reg, null);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("User registered successfully", result.getBody().get("message"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService, times(1)).registerUser(userCaptor.capture(), isNull());
        User captured = userCaptor.getValue();
        assertEquals("John", captured.getFirstName());
        assertEquals("Doe", captured.getLastName());
        assertEquals("john.doe@example.com", captured.getEmail());
        assertEquals(Role.CLIENT, captured.getRole());
    }
}
