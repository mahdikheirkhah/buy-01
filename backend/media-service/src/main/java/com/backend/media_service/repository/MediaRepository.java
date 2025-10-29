package com.backend.media_service.repository;

import com.backend.media_service.model.Media;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MediaRepository extends MongoRepository<Media, String> {

    // This finds media for a product and applies sorting/limits
    List<Media> findByProductID(String productId, Pageable pageable);
    // Your existing "find all" method (if you had one)
    List<Media> findByProductID(String productId);
}