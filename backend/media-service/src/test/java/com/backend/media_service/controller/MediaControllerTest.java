package com.backend.media_service.controller;

import com.backend.media_service.model.Media;
import com.backend.media_service.service.FileStorageService;
import com.backend.media_service.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MediaService mediaService;

    @Mock
    private Resource resource;

    @InjectMocks
    private MediaController mediaController;

    private MockMultipartFile mockFile;
    private Media mockMedia;

    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile(
            "file",
            "test-image.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        mockMedia = new Media();
        mockMedia.setId("test-media-id");
        mockMedia.setImagePath("test-image.jpg");
        mockMedia.setProductID("test-product-id");
    }

    @Test
    void testUploadFile_Success() {
        // Arrange
        String productId = "test-product-id";
        when(mediaService.uploadFile(any(MultipartFile.class), anyString())).thenReturn(mockMedia);

        // Act
        ResponseEntity<?> response = mediaController.uploadFile(mockFile, productId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(mediaService, times(1)).uploadFile(mockFile, productId);
    }

    @Test
    void testUploadFileForAvatar_Success() {
        // Arrange
        String expectedFileName = "avatar-test.jpg";
        when(mediaService.uploadFileAvatar(any(MultipartFile.class))).thenReturn(expectedFileName);

        // Act
        ResponseEntity<String> response = mediaController.uploadFileForAvatar(mockFile);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains(expectedFileName));
        verify(mediaService, times(1)).uploadFileAvatar(mockFile);
    }

    @Test
    void testGetFile_Success() throws Exception {
        // Arrange
        String filename = "test-image.jpg";
        java.io.File mockFile = mock(java.io.File.class);
        java.nio.file.Path mockPath = mock(java.nio.file.Path.class);

        when(fileStorageService.load(anyString())).thenReturn(resource);
        when(resource.getFile()).thenReturn(mockFile);
        when(mockFile.toPath()).thenReturn(mockPath);

        // Act
        ResponseEntity<Resource> response = mediaController.getFile(filename);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(fileStorageService, times(1)).load(filename);
    }

    @Test
    void testUploadFile_InvalidFile() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );
        String productId = "test-product-id";

        when(mediaService.uploadFile(any(MultipartFile.class), anyString()))
            .thenThrow(new RuntimeException("File is empty"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            mediaController.uploadFile(emptyFile, productId);
        });
    }

    @Test
    void testGetFile_FileNotFound() {
        // Arrange
        String filename = "non-existent.jpg";
        when(fileStorageService.load(anyString()))
            .thenThrow(new RuntimeException("File not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            mediaController.getFile(filename);
        });
    }
}

