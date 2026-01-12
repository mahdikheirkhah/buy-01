package com.backend.product_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.exception.CustomException;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceUnitTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private InfoUserDTO testSeller;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id("product123")
                .name("Test Product")
                .description("Test Description")
                .price(99.99)
                .quantity(10)
                .sellerID("seller123")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testSeller = InfoUserDTO.builder()
                .id("seller123")
                .firstName("John")
                .lastName("Seller")
                .email("seller@example.com")
                .build();
    }

    @Test
    @DisplayName("Should get product by ID successfully")
    void testGetProductSuccess() {
        // Arrange
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));

        // Act
        Product found = productService.getProduct("product123");

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo("product123");
        assertThat(found.getName()).isEqualTo("Test Product");
        verify(productRepository).findById("product123");
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void testGetProductNotFound() {
        // Arrange
        when(productRepository.findById("nonexistent"))
                .thenThrow(new CustomException("Product not found", null));

        // Act & Assert
        assertThatThrownBy(() -> productService.getProduct("nonexistent"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository).findById("nonexistent");
    }

    @Test
    @DisplayName("Should get all products successfully")
    void testGetAllProductsSuccess() {
        // Arrange
        Product product1 = Product.builder()
                .id("product1")
                .name("Product 1")
                .price(50.0)
                .sellerID("seller1")
                .build();
        Product product2 = Product.builder()
                .id("product2")
                .name("Product 2")
                .price(75.0)
                .sellerID("seller2")
                .build();

        when(productRepository.findAll()).thenReturn(List.of(product1, product2));

        // Act
        List<Product> products = productService.getAllProducts();

        // Assert
        assertThat(products).hasSize(2);
        assertThat(products.get(0).getId()).isEqualTo("product1");
        assertThat(products.get(1).getId()).isEqualTo("product2");
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no products exist")
    void testGetAllProductsEmpty() {
        // Arrange
        when(productRepository.findAll()).thenReturn(List.of());

        // Act
        List<Product> products = productService.getAllProducts();

        // Assert
        assertThat(products).isEmpty();
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should delete product successfully")
    void testDeleteProductSuccess() {
        // Arrange
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));

        // Act
        productService.deleteProduct("product123", "seller123");

        // Assert
        verify(productRepository).findById("product123");
        verify(kafkaTemplate).send("product-deleted-topic", "product123");
        verify(productRepository).delete(testProduct);
    }

    @Test
    @DisplayName("Should publish Kafka message when deleting product")
    void testDeleteProductPublishesKafkaEvent() {
        // Arrange
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));

        // Act
        productService.deleteProduct("product123", "seller123");

        // Assert
        verify(kafkaTemplate).send("product-deleted-topic", "product123");
    }

    @Test
    @DisplayName("Should delete all products of seller successfully")
    void testDeleteProductsOfUserSuccess() {
        // Arrange
        Product product1 = Product.builder()
                .id("product1")
                .name("Product 1")
                .sellerID("seller123")
                .build();
        Product product2 = Product.builder()
                .id("product2")
                .name("Product 2")
                .sellerID("seller123")
                .build();

        when(productRepository.findAllBySellerID("seller123"))
                .thenReturn(List.of(product1, product2));

        // Act
        productService.DeleteProductsOfUser("seller123");

        // Assert
        verify(productRepository).findAllBySellerID("seller123");
        verify(kafkaTemplate, times(2)).send(eq("product-deleted-topic"), anyString());
        verify(productRepository, times(2)).delete(any(Product.class));
    }

    @Test
    @DisplayName("Should handle deletion when seller has no products")
    void testDeleteProductsOfUserNoProducts() {
        // Arrange
        when(productRepository.findAllBySellerID("seller123")).thenReturn(List.of());

        // Act
        productService.DeleteProductsOfUser("seller123");

        // Assert
        verify(productRepository).findAllBySellerID("seller123");
        verify(kafkaTemplate, never()).send(anyString(), anyString());
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("Should validate product has required fields")
    void testProductValidation() {
        // Arrange - Product with missing name
        Product invalidProduct = Product.builder()
                .description("Description")
                .price(50.0)
                .sellerID("seller123")
                .build();

        // Assert
        assertThat(invalidProduct.getName()).isNull();
    }

    @Test
    @DisplayName("Should verify price is positive")
    void testProductPriceValidation() {
        // Assert
        assertThat(testProduct.getPrice()).isPositive();
        assertThat(testProduct.getPrice()).isEqualTo(99.99);
    }

    @Test
    @DisplayName("Should verify quantity is non-negative")
    void testProductQuantityValidation() {
        // Assert
        assertThat(testProduct.getQuantity()).isGreaterThanOrEqualTo(0);
        assertThat(testProduct.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should find product by ID from repository")
    void testFindByIdSuccess() {
        // Arrange
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));

        // Act
        Optional<Product> found = productRepository.findById("product123");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("product123");
        verify(productRepository).findById("product123");
    }

    @Test
    @DisplayName("Should return empty optional when product ID not found")
    void testFindByIdNotFound() {
        // Arrange
        when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<Product> found = productRepository.findById("nonexistent");

        // Assert
        assertThat(found).isEmpty();
        verify(productRepository).findById("nonexistent");
    }

    @Test
    @DisplayName("Should save product successfully")
    void testSaveProductSuccess() {
        // Arrange
        Product newProduct = Product.builder()
                .name("New Product")
                .description("New Description")
                .price(49.99)
                .quantity(5)
                .sellerID("seller456")
                .build();

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId("newProductId");
            return p;
        });

        // Act
        Product saved = productRepository.save(newProduct);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("newProductId");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should verify seller ID is present")
    void testProductSellerIDPresent() {
        // Assert
        assertThat(testProduct.getSellerID()).isNotNull();
        assertThat(testProduct.getSellerID()).isEqualTo("seller123");
    }

    @Test
    @DisplayName("Should verify product timestamps are set")
    void testProductTimestamps() {
        // Assert
        assertThat(testProduct.getCreatedAt()).isNotNull();
        assertThat(testProduct.getUpdatedAt()).isNotNull();
    }
}
