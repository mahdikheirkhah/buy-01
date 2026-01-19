package com.backend.product_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for product search and filtering
 * Uses MongoDB queries for efficient filtering at database level
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final WebClient.Builder webClientBuilder;

    /**
     * Search and filter products with optional criteria
     * If all parameters are null, returns all products
     * 
     * @param keyword     - Search keyword (matches product name or description)
     * @param minPrice    - Minimum price filter (optional)
     * @param maxPrice    - Maximum price filter (optional)
     * @param minQuantity - Minimum quantity filter (optional)
     * @param maxQuantity - Maximum quantity filter (optional)
     * @param startDate   - Filter products created after this date (optional)
     * @param endDate     - Filter products created before this date (optional)
     * @param sellerId    - Current user ID to check if they created the product
     *                    (optional)
     * @param pageable    - Pagination info
     * @return Page of ProductCardDTO matching the criteria
     */
    public Page<ProductCardDTO> searchAndFilter(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minQuantity,
            Integer maxQuantity,
            Instant startDate,
            Instant endDate,
            String sellerId,
            Pageable pageable) {

        log.info(
                "Searching products - keyword: {}, priceRange: {}-{}, quantityRange: {}-{}, dateRange: {} to {}",
                keyword, minPrice, maxPrice, minQuantity, maxQuantity, startDate, endDate);

        // Convert BigDecimal to Double for MongoDB query
        Double minPriceDouble = minPrice != null ? minPrice.doubleValue() : null;
        Double maxPriceDouble = maxPrice != null ? maxPrice.doubleValue() : null;

        // Use repository query method - filtering happens at database level
        Page<Product> products = productRepository.searchAndFilterProducts(
                keyword,
                minPriceDouble,
                maxPriceDouble,
                minQuantity,
                maxQuantity,
                startDate,
                endDate,
                pageable);

        log.info("Found {} products matching criteria", products.getTotalElements());

        // Convert Product entities to ProductCardDTO with limited images
        return convertToCardDTOPage(products, sellerId);
    }

    /**
     * Convert Product page to ProductCardDTO page with limited images
     * Same pattern as ProductService.getAllProducts()
     */
    private Page<ProductCardDTO> convertToCardDTOPage(Page<Product> productPage, String sellerId) {
        return productPage.map(product -> {
            boolean isCreator = sellerId != null && product.getSellerID().equals(sellerId);
            List<String> limitedImages = getLimitedImageUrls(product.getId(), 3);

            return new ProductCardDTO(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getQuantity(),
                    isCreator,
                    limitedImages);
        });
    }

    /**
     * Fetch limited image URLs from Media Service
     * Same pattern as ProductService.getLimitedImageUrls()
     */
    private List<String> getLimitedImageUrls(String productId, int limit) {
        try {
            ParameterizedTypeReference<List<String>> listType = new ParameterizedTypeReference<>() {
            };

            return webClientBuilder.build().get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("MEDIA-SERVICE")
                            .path("/api/media/product/{productId}/urls")
                            .queryParam("limit", limit)
                            .build(productId))
                    .retrieve()
                    .bodyToMono(listType)
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch media URLs for product {}: {}", productId, e.getMessage());
            return List.of();
        }
    }
}
