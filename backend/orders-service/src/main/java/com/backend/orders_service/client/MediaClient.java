package com.backend.orders_service.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaClient {
    private final WebClient.Builder webClientBuilder;

    /**
     * Get the first image URL for a product by calling /urls endpoint with limit=1
     */
    public String getFirstImageUrl(String productId) {
        try {
            // Type reference for deserializing List<String>
            ParameterizedTypeReference<List<String>> listType = new ParameterizedTypeReference<>() {
            };

            List<String> urls = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("MEDIA-SERVICE")
                            .path("/api/media/product/{productId}/urls")
                            .queryParam("limit", 1)
                            .build(productId))
                    .retrieve()
                    .bodyToMono(listType)
                    .block();

            if (urls != null && !urls.isEmpty()) {
                return urls.get(0);
            }
            return null;
        } catch (Exception e) {
            // Log but don't throw - images are optional
            System.err.println("Error fetching image URL for product " + productId + ": " + e.getMessage());
            return null;
        }
    }
}