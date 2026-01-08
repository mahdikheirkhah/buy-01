package com.backend.orders_service.client;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.orders_service.model.OrderItem;

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

        webClientBuilder.build()
                .post()
                .uri("https://PRODUCT-SERVICE/api/products/adjust-stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private record StockAdjustmentRequest(String productId, int quantity) {
    }
}
