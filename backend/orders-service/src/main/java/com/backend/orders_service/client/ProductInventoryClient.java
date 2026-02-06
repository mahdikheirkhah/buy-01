package com.backend.orders_service.client;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.backend.orders_service.model.OrderItem;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductInventoryClient {
    private final WebClient.Builder webClientBuilder;

    public void decreaseStock(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<StockAdjustmentRequest> payload = items.stream()
                .map(item -> new StockAdjustmentRequest(item.getProductId(), item.getQuantity()))
                .toList();

        try {
            webClientBuilder.build()
                    .post()
                    .uri("https://PRODUCT-SERVICE/api/products/adjust-stock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            // Re-throw the exception with proper status so it can be handled by GlobalExceptionHandler
            throw e;
        }
    }

    public void increaseStock(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<StockAdjustmentRequest> payload = items.stream()
                .map(item -> new StockAdjustmentRequest(item.getProductId(), item.getQuantity()))
                .toList();

        try {
            webClientBuilder.build()
                    .post()
                    .uri("https://PRODUCT-SERVICE/api/products/restock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            // Re-throw the exception with proper status so it can be handled by GlobalExceptionHandler
            throw e;
        }
    }

    /**
     * Get product details including current quantity available
     */
    public ProductDetail getProductDetails(String productId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri("https://PRODUCT-SERVICE/api/products/{productId}", productId)
                    .header("X-User-ID", "system") // Use system user for internal calls
                    .retrieve()
                    .bodyToMono(ProductDetail.class)
                    .block();
        } catch (WebClientResponseException e) {
            // Product not found or error getting details
            throw new RuntimeException("Failed to get product details for " + productId, e);
        }
    }

    /**
     * Get multiple product details in one call
     */
    public List<ProductDetail> getProductsDetails(List<String> productIds) {
        return productIds.stream()
                .map(this::getProductDetails)
                .collect(Collectors.toList());
    }

    private record StockAdjustmentRequest(String productId, int quantity) {
    }

    @Data
    public static class ProductDetail {
        @JsonProperty("productId")
        private String productId;
        private String name;
        private String description;
        private Double price;
        private Integer quantity;
        private String sellerFirstName;
        private String sellerLastName;
        private String sellerEmail;
    }
}
