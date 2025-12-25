package com.backend.media_service.service;

import com.backend.media_service.model.Media;
import com.backend.media_service.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MediaService using Testcontainers.
 * Uses @SpringBootTest to load the full application context with real MongoDB and Kafka.
 */
@Testcontainers
@SpringBootTest
@DisplayName("MediaService Integration Tests with Testcontainers")
class MediaServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:7.0")
    );

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @Autowired
    private MediaRepository mediaRepository;

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri",
            () -> "mongodb://" + mongoDBContainer.getHost() + ":" +
                  mongoDBContainer.getFirstMappedPort() + "/testdb");
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        mediaRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create and persist media to MongoDB")
    void testCreateMediaIntegration() {
        Media newMedia = new Media();
        newMedia.setImagePath("/api/media/files/image.jpg");
        newMedia.setProductID("product123");

        Media saved = mediaRepository.save(newMedia);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProductID()).isEqualTo("product123");

        Media retrieved = mediaRepository.findById(saved.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getImagePath()).isEqualTo("/api/media/files/image.jpg");
    }

    @Test
    @DisplayName("Should find media by product ID from MongoDB")
    void testFindByProductIDIntegration() {
        Media media1 = new Media();
        media1.setImagePath("/api/media/files/image1.jpg");
        media1.setProductID("product123");

        Media media2 = new Media();
        media2.setImagePath("/api/media/files/image2.jpg");
        media2.setProductID("product123");

        Media media3 = new Media();
        media3.setImagePath("/api/media/files/image3.jpg");
        media3.setProductID("product456");

        mediaRepository.saveAll(List.of(media1, media2, media3));

        List<Media> result = mediaRepository.findByProductID("product123");

        assertThat(result).hasSize(2);
        assertThat(result.stream().allMatch(m -> m.getProductID().equals("product123"))).isTrue();
    }

    @Test
    @DisplayName("Should find media by product ID with pagination")
    void testFindByProductIDWithPaginationIntegration() {
        for (int i = 0; i < 5; i++) {
            Media media = new Media();
            media.setImagePath("/api/media/files/image" + i + ".jpg");
            media.setProductID("product123");
            mediaRepository.save(media);
        }

        Pageable pageable = PageRequest.of(0, 2);
        List<Media> result = mediaRepository.findByProductID("product123", pageable);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should update media and persist changes")
    void testUpdateMediaIntegration() {
        Media media = new Media();
        media.setImagePath("/api/media/files/old.jpg");
        media.setProductID("product123");
        Media saved = mediaRepository.save(media);

        saved.setImagePath("/api/media/files/new.jpg");
        Media updated = mediaRepository.save(saved);

        Media retrieved = mediaRepository.findById(updated.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getImagePath()).isEqualTo("/api/media/files/new.jpg");
    }

    @Test
    @DisplayName("Should delete media from MongoDB")
    void testDeleteMediaIntegration() {
        Media media = new Media();
        media.setImagePath("/api/media/files/delete.jpg");
        media.setProductID("product123");
        Media saved = mediaRepository.save(media);

        mediaRepository.delete(saved);

        assertThat(mediaRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should maintain data consistency with multiple products")
    void testMultipleProductsDataConsistencyIntegration() {
        Media p1m1 = new Media();
        p1m1.setImagePath("/api/media/files/p1m1.jpg");
        p1m1.setProductID("product1");

        Media p1m2 = new Media();
        p1m2.setImagePath("/api/media/files/p1m2.jpg");
        p1m2.setProductID("product1");

        Media p2m1 = new Media();
        p2m1.setImagePath("/api/media/files/p2m1.jpg");
        p2m1.setProductID("product2");

        mediaRepository.saveAll(List.of(p1m1, p1m2, p2m1));

        List<Media> product1Media = mediaRepository.findByProductID("product1");
        List<Media> product2Media = mediaRepository.findByProductID("product2");

        assertThat(product1Media).hasSize(2);
        assertThat(product2Media).hasSize(1);
        assertThat(product1Media.stream().allMatch(m -> m.getProductID().equals("product1"))).isTrue();
        assertThat(product2Media.stream().allMatch(m -> m.getProductID().equals("product2"))).isTrue();
    }

    @Test
    @DisplayName("Should retrieve all media from collection")
    void testGetAllMediaIntegration() {
        Media m1 = new Media();
        m1.setImagePath("/api/media/files/image1.jpg");
        m1.setProductID("product1");

        Media m2 = new Media();
        m2.setImagePath("/api/media/files/image2.jpg");
        m2.setProductID("product2");

        Media m3 = new Media();
        m3.setImagePath("/api/media/files/image3.jpg");
        m3.setProductID("product3");

        mediaRepository.saveAll(List.of(m1, m2, m3));

        List<Media> allMedia = mediaRepository.findAll();

        assertThat(allMedia).hasSize(3);
    }

    @Test
    @DisplayName("Should handle concurrent media creation")
    void testConcurrentMediaCreationIntegration() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            Media m = new Media();
            m.setImagePath("/api/media/files/thread1.jpg");
            m.setProductID("product1");
            mediaRepository.save(m);
        });

        Thread thread2 = new Thread(() -> {
            Media m = new Media();
            m.setImagePath("/api/media/files/thread2.jpg");
            m.setProductID("product2");
            mediaRepository.save(m);
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        List<Media> allMedia = mediaRepository.findAll();
        assertThat(allMedia).hasSize(2);
    }

    @Test
    @DisplayName("Should preserve media timestamps")
    void testMediaTimestampsIntegration() {
        Media media = new Media();
        media.setImagePath("/api/media/files/timestamp.jpg");
        media.setProductID("product123");

        Media saved = mediaRepository.save(media);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Media retrieved = mediaRepository.findById(saved.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getCreatedAt()).isNotNull();
        assertThat(retrieved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle empty query results")
    void testEmptyQueryResultsIntegration() {
        List<Media> result = mediaRepository.findByProductID("nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should delete all media for a product")
    void testDeleteAllMediaForProductIntegration() {
        Media m1 = new Media();
        m1.setImagePath("/api/media/files/image1.jpg");
        m1.setProductID("product123");

        Media m2 = new Media();
        m2.setImagePath("/api/media/files/image2.jpg");
        m2.setProductID("product123");

        mediaRepository.saveAll(List.of(m1, m2));

        List<Media> mediaToDelete = mediaRepository.findByProductID("product123");
        mediaRepository.deleteAll(mediaToDelete);

        List<Media> result = mediaRepository.findByProductID("product123");
        assertThat(result).isEmpty();
    }
}

