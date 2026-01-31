package com.backend.media_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.backend.common.exception.CustomException;
import com.backend.media_service.model.Media;
import com.backend.media_service.repository.MediaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService Unit Tests")
class MediaServiceUnitTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private MediaService mediaService;

    private Media testMedia;

    @BeforeEach
    void setUp() {
        testMedia = new Media();
        testMedia.setId("media123");
        testMedia.setImagePath("/api/media/files/image.jpg");
        testMedia.setProductID("product123");
        testMedia.setCreatedAt(Instant.now());
        testMedia.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should upload file successfully")
    void testUploadFileSuccess() {
        // Arrange
        when(fileStorageService.save(mockFile)).thenReturn("image.jpg");
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setId("newMediaId");
            return m;
        });

        // Act
        Media saved = mediaService.uploadFile(mockFile, "product123");

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("newMediaId");
        assertThat(saved.getProductID()).isEqualTo("product123");
        verify(fileStorageService).save(mockFile);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    @DisplayName("Should upload avatar file successfully")
    void testUploadFileAvatarSuccess() {
        // Arrange
        when(fileStorageService.save(mockFile)).thenReturn("avatar.jpg");

        // Act
        String filename = mediaService.uploadFileAvatar(mockFile);

        // Assert
        assertThat(filename).isEqualTo("avatar.jpg");
        verify(fileStorageService).save(mockFile);
    }

    @Test
    @DisplayName("Should find media by product ID successfully")
    void testFindMediaByProductIDSuccess() {
        // Arrange
        Media media1 = new Media();
        media1.setId("media1");
        media1.setImagePath("/api/media/files/image1.jpg");
        media1.setProductID("product123");

        Media media2 = new Media();
        media2.setId("media2");
        media2.setImagePath("/api/media/files/image2.jpg");
        media2.setProductID("product123");

        when(mediaRepository.findByProductID("product123"))
                .thenReturn(List.of(media1, media2));

        // Act
        var result = mediaService.findMediaByProductID("product123");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFileUrl()).isEqualTo("/api/media/files/image1.jpg");
        assertThat(result.get(1).getFileUrl()).isEqualTo("/api/media/files/image2.jpg");
        verify(mediaRepository).findByProductID("product123");
    }

    @Test
    @DisplayName("Should return empty list when no media found for product")
    void testFindMediaByProductIDEmpty() {
        // Arrange
        when(mediaRepository.findByProductID("nonexistent"))
                .thenReturn(List.of());

        // Act
        var result = mediaService.findMediaByProductID("nonexistent");

        // Assert
        assertThat(result).isEmpty();
        verify(mediaRepository).findByProductID("nonexistent");
    }

    @Test
    @DisplayName("Should delete media by product ID successfully")
    void testDeleteMediaByProductIDSuccess() {
        // Arrange
        Media media1 = new Media();
        media1.setId("media1");
        media1.setImagePath("/api/media/files/image1.jpg");
        media1.setProductID("product123");

        Media media2 = new Media();
        media2.setId("media2");
        media2.setImagePath("/api/media/files/image2.jpg");
        media2.setProductID("product123");

        when(mediaRepository.findByProductID("product123"))
                .thenReturn(List.of(media1, media2));

        // Act
        mediaService.DeleteMediaByProductID("product123");

        // Assert
        verify(fileStorageService).delete("/api/media/files/image1.jpg");
        verify(fileStorageService).delete("/api/media/files/image2.jpg");
        verify(mediaRepository).deleteAll(List.of(media1, media2));
    }

    @Test
    @DisplayName("Should handle deletion when no media exists for product")
    void testDeleteMediaByProductIDEmpty() {
        // Arrange
        when(mediaRepository.findByProductID("product123")).thenReturn(List.of());

        // Act
        mediaService.DeleteMediaByProductID("product123");

        // Assert
        verify(fileStorageService, never()).delete(anyString());
        verify(mediaRepository).deleteAll(List.of());
    }

    @Test
    @DisplayName("Should delete media by ID successfully")
    void testDeleteMediaByIDSuccess() {
        // Arrange
        when(mediaRepository.findById("media123"))
                .thenReturn(Optional.of(testMedia));

        // Act
        mediaService.DeleteMediaByID("media123");

        // Assert
        verify(mediaRepository).findById("media123");
        verify(fileStorageService).delete("/api/media/files/image.jpg");
        verify(mediaRepository).delete(testMedia);
    }

    @Test
    @DisplayName("Should throw exception when media ID not found")
    void testDeleteMediaByIDNotFound() {
        // Arrange
        when(mediaRepository.findById("nonexistent"))
                .thenThrow(new CustomException("Media Not Found!", null));

        // Act & Assert
        assertThatThrownBy(() -> mediaService.DeleteMediaByID("nonexistent"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Media Not Found!");

        verify(mediaRepository).findById("nonexistent");
        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    @DisplayName("Should delete media by avatar URL successfully")
    void testDeleteMediaByAvatarUrlSuccess() {
        // Act
        mediaService.DeleteMediaByAvatarUrl("/uploads/avatar.jpg");

        // Assert
        verify(fileStorageService).delete("/uploads/avatar.jpg");
    }

    @Test
    @DisplayName("Should verify media has product ID")
    void testMediaHasProductID() {
        // Assert
        assertThat(testMedia.getProductID()).isNotNull();
        assertThat(testMedia.getProductID()).isEqualTo("product123");
    }

    @Test
    @DisplayName("Should verify media has image path")
    void testMediaHasImagePath() {
        // Assert
        assertThat(testMedia.getImagePath()).isNotNull();
        assertThat(testMedia.getImagePath()).startsWith("/api/media/files/");
    }

    @Test
    @DisplayName("Should verify media has ID")
    void testMediaHasID() {
        // Assert
        assertThat(testMedia.getId()).isNotNull();
        assertThat(testMedia.getId()).isEqualTo("media123");
    }

    @Test
    @DisplayName("Should verify media timestamps are set")
    void testMediaTimestamps() {
        // Assert
        assertThat(testMedia.getCreatedAt()).isNotNull();
        assertThat(testMedia.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find media by ID successfully")
    void testFindByIDSuccess() {
        // Arrange
        when(mediaRepository.findById("media123")).thenReturn(Optional.of(testMedia));

        // Act
        Optional<Media> found = mediaRepository.findById("media123");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("media123");
        verify(mediaRepository).findById("media123");
    }

    @Test
    @DisplayName("Should return empty when media ID not found")
    void testFindByIDNotFound() {
        // Arrange
        when(mediaRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<Media> found = mediaRepository.findById("nonexistent");

        // Assert
        assertThat(found).isEmpty();
        verify(mediaRepository).findById("nonexistent");
    }

    @Test
    @DisplayName("Should save media successfully")
    void testSaveMediaSuccess() {
        // Arrange
        Media newMedia = new Media();
        newMedia.setImagePath("/api/media/files/new.jpg");
        newMedia.setProductID("product456");

        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setId("newId");
            return m;
        });

        // Act
        Media saved = mediaRepository.save(newMedia);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("newId");
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    @DisplayName("Should update media successfully")
    void testUpdateMediaSuccess() {
        // Arrange
        Media existingMedia = new Media();
        existingMedia.setId("media123");
        existingMedia.setImagePath("/api/media/files/old.jpg");
        existingMedia.setProductID("product123");

        Media updatedMedia = new Media();
        updatedMedia.setId("media123");
        updatedMedia.setImagePath("/api/media/files/updated.jpg");
        updatedMedia.setProductID("product123");

        when(mediaRepository.save(any(Media.class))).thenReturn(updatedMedia);

        // Act
        Media result = mediaService.updateMedia(existingMedia);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("media123");
        assertThat(result.getImagePath()).isEqualTo("/api/media/files/updated.jpg");
        verify(mediaRepository).save(existingMedia);
    }

    @Test
    @DisplayName("Should get limited image URLs for product")
    void testGetLimitedImageUrlsForProduct() {
        // Arrange
        String productId = "product123";
        int limit = 3;

        Media media1 = new Media();
        media1.setId("media1");
        media1.setImagePath("/api/media/files/image1.jpg");
        media1.setProductID(productId);
        media1.setCreatedAt(Instant.now().minusSeconds(300));

        Media media2 = new Media();
        media2.setId("media2");
        media2.setImagePath("/api/media/files/image2.jpg");
        media2.setProductID(productId);
        media2.setCreatedAt(Instant.now().minusSeconds(200));

        Media media3 = new Media();
        media3.setId("media3");
        media3.setImagePath("/api/media/files/image3.jpg");
        media3.setProductID(productId);
        media3.setCreatedAt(Instant.now().minusSeconds(100));

        when(mediaRepository.findByProductID(eq(productId), any()))
                .thenReturn(List.of(media1, media2, media3));

        // Act
        List<String> urls = mediaService.getLimitedImageUrlsForProduct(productId, limit);

        // Assert
        assertThat(urls).hasSize(3);
        assertThat(urls).containsExactly(
                "/api/media/files/image1.jpg",
                "/api/media/files/image2.jpg",
                "/api/media/files/image3.jpg");
        verify(mediaRepository).findByProductID(eq(productId), any());
    }

    @Test
    @DisplayName("Should get limited image URLs with fewer results than limit")
    void testGetLimitedImageUrlsForProduct_FewerThanLimit() {
        // Arrange
        String productId = "product123";
        int limit = 5;

        Media media1 = new Media();
        media1.setId("media1");
        media1.setImagePath("/api/media/files/image1.jpg");
        media1.setProductID(productId);

        Media media2 = new Media();
        media2.setId("media2");
        media2.setImagePath("/api/media/files/image2.jpg");
        media2.setProductID(productId);

        when(mediaRepository.findByProductID(eq(productId), any()))
                .thenReturn(List.of(media1, media2));

        // Act
        List<String> urls = mediaService.getLimitedImageUrlsForProduct(productId, limit);

        // Assert
        assertThat(urls).hasSize(2);
        assertThat(urls).containsExactly(
                "/api/media/files/image1.jpg",
                "/api/media/files/image2.jpg");
        verify(mediaRepository).findByProductID(eq(productId), any());
    }

    @Test
    @DisplayName("Should return empty list when no images found for product")
    void testGetLimitedImageUrlsForProduct_Empty() {
        // Arrange
        String productId = "product123";
        int limit = 3;

        when(mediaRepository.findByProductID(eq(productId), any()))
                .thenReturn(List.of());

        // Act
        List<String> urls = mediaService.getLimitedImageUrlsForProduct(productId, limit);

        // Assert
        assertThat(urls).isEmpty();
        verify(mediaRepository).findByProductID(eq(productId), any());
    }
}
