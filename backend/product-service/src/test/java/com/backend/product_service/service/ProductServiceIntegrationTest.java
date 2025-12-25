package com.backend.product_service.service;

import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
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
 * Integration tests for ProductService using Testcontainers.
 * Uses @SpringBootTest to load the full application context with real MongoDB and Kafka.
 */
@Testcontainers
@SpringBootTest
@DisplayName("ProductService Integration Tests with Testcontainers")
class ProductServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:7.0")
    );

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @Autowired
    private ProductRepository productRepository;

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri",
            () -> "mongodb://" + mongoDBContainer.getHost() + ":" +
                  mongoDBContainer.getFirstMappedPort() + "/testdb");
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create and persist product to MongoDB")
    void testCreateProductIntegration() {
        Product newProduct = Product.builder()
                .name("Integration Test Product")
                .description("Test Description for Integration")
                .price(99.99)
                .quantity(5)
                .sellerID("seller123")
                .build();

        Product saved = productRepository.save(newProduct);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Integration Test Product");

        Product retrieved = productRepository.findById(saved.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getPrice()).isEqualTo(99.99);
        assertThat(retrieved.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should find products by seller ID with pagination")
    void testFindBySellerIDWithPaginationIntegration() {
        Product p1 = Product.builder().name("Product 1").price(50.0).quantity(10).sellerID("seller123").build();
        Product p2 = Product.builder().name("Product 2").price(75.0).quantity(20).sellerID("seller123").build();
        Product p3 = Product.builder().name("Product 3").price(100.0).quantity(15).sellerID("seller123").build();

        productRepository.save(p1);
        productRepository.save(p2);
        productRepository.save(p3);

        Pageable pageable = PageRequest.of(0, 2);
        Page<Product> products = productRepository.findBySellerID("seller123", pageable);

        assertThat(products.getContent()).hasSize(2);
        assertThat(products.getTotalElements()).isEqualTo(3);
        assertThat(products.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Should find all products by seller ID")
    void testFindAllBySellerIDIntegration() {
        Product p1 = Product.builder().name("Product 1").price(50.0).sellerID("seller123").build();
        Product p2 = Product.builder().name("Product 2").price(75.0).sellerID("seller123").build();
        Product p3 = Product.builder().name("Product 3").price(100.0).sellerID("seller456").build();

        productRepository.save(p1);
        productRepository.save(p2);
        productRepository.save(p3);

        List<Product> sellerProducts = productRepository.findAllBySellerID("seller123");

        assertThat(sellerProducts).hasSize(2);
        assertThat(sellerProducts.stream().allMatch(p -> p.getSellerID().equals("seller123"))).isTrue();
    }

    @Test
    @DisplayName("Should update product and persist changes")
    void testUpdateProductIntegration() {
        Product product = Product.builder()
                .name("Original Name")
                .description("Original Description")
                .price(50.0)
                .quantity(10)
                .sellerID("seller123")
                .build();
        Product saved = productRepository.save(product);

        saved.setName("Updated Name");
        saved.setPrice(75.0);
        saved.setQuantity(20);
        Product updated = productRepository.save(saved);

        Product retrieved = productRepository.findById(updated.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("Updated Name");
        assertThat(retrieved.getPrice()).isEqualTo(75.0);
        assertThat(retrieved.getQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should delete product from MongoDB")
    void testDeleteProductIntegration() {
        Product product = Product.builder()
                .name("To Delete")
                .description("Will be deleted")
                .price(50.0)
                .sellerID("seller123")
                .build();
        Product saved = productRepository.save(product);

        productRepository.delete(saved);

        assertThat(productRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should maintain data consistency with multiple sellers")
    void testMultipleSellersDataConsistencyIntegration() {
        Product seller1Product1 = Product.builder().name("S1 P1").price(50.0).sellerID("seller1").build();
        Product seller1Product2 = Product.builder().name("S1 P2").price(60.0).sellerID("seller1").build();
        Product seller2Product1 = Product.builder().name("S2 P1").price(70.0).sellerID("seller2").build();
        Product seller2Product2 = Product.builder().name("S2 P2").price(80.0).sellerID("seller2").build();

        productRepository.saveAll(List.of(seller1Product1, seller1Product2, seller2Product1, seller2Product2));

        List<Product> seller1Products = productRepository.findAllBySellerID("seller1");
        List<Product> seller2Products = productRepository.findAllBySellerID("seller2");

        assertThat(seller1Products).hasSize(2);
        assertThat(seller2Products).hasSize(2);
        assertThat(seller1Products.stream().allMatch(p -> p.getSellerID().equals("seller1"))).isTrue();
        assertThat(seller2Products.stream().allMatch(p -> p.getSellerID().equals("seller2"))).isTrue();
    }

    @Test
    @DisplayName("Should handle product with edge case values")
    void testProductEdgeCasesIntegration() {
        Product minProduct = Product.builder()
                .name("Min")
                .description("Minimum description")
                .price(0.01)
                .quantity(0)
                .sellerID("seller")
                .build();

        Product saved = productRepository.save(minProduct);

        assertThat(saved).isNotNull();
        assertThat(saved.getPrice()).isEqualTo(0.01);
        assertThat(saved.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should retrieve all products from collection")
    void testGetAllProductsIntegration() {
        Product p1 = Product.builder().name("Product 1").price(50.0).sellerID("seller1").build();
        Product p2 = Product.builder().name("Product 2").price(75.0).sellerID("seller2").build();
        Product p3 = Product.builder().name("Product 3").price(100.0).sellerID("seller3").build();

        productRepository.saveAll(List.of(p1, p2, p3));

        List<Product> allProducts = productRepository.findAll();

        assertThat(allProducts).hasSize(3);
        assertThat(allProducts.stream().map(Product::getName))
                .contains("Product 1", "Product 2", "Product 3");
    }

    @Test
    @DisplayName("Should handle concurrent product creation")
    void testConcurrentProductCreationIntegration() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            Product p = Product.builder()
                    .name("Thread1 Product")
                    .description("Created by thread 1")
                    .price(50.0)
                    .sellerID("seller1")
                    .build();
            productRepository.save(p);
        });

        Thread thread2 = new Thread(() -> {
            Product p = Product.builder()
                    .name("Thread2 Product")
                    .description("Created by thread 2")
                    .price(75.0)
                    .sellerID("seller2")
                    .build();
            productRepository.save(p);
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(2);
    }

    @Test
    @DisplayName("Should preserve product timestamps")
    void testProductTimestampsIntegration() {
        Product product = Product.builder()
                .name("Timestamp Test")
                .description("Testing timestamps")
                .price(50.0)
                .sellerID("seller123")
                .build();

        Product saved = productRepository.save(product);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Product retrieved = productRepository.findById(saved.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getCreatedAt()).isNotNull();
        assertThat(retrieved.getUpdatedAt()).isNotNull();
    }
}

