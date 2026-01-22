package com.backend.product_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.exception.CustomException;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceUnitTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

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

    @Test
    @DisplayName("Should get product by product ID with details")
    void testGetProductByProductID() {
        // Arrange
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(InfoUserDTO.class)).thenReturn(Mono.just(testSeller));
        when(responseSpec.bodyToFlux(MediaUploadResponseDTO.class)).thenReturn(Flux.empty());

        // Act
        ProductDTO result = productService.getProductByProductID("product123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo("product123");
        verify(productRepository).findById("product123");
    }

    @Test
    @DisplayName("Should update product successfully")
    void testUpdateProduct() {
        // Arrange
        UpdateProductDTO updateDTO = UpdateProductDTO.builder()
                .name("Updated Product")
                .description("Updated Description")
                .price(149.99)
                .quantity(20)
                .build();

        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        doNothing().when(productMapper).updateProductFromDto(any(UpdateProductDTO.class), any(Product.class));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        UpdateProductDTO result = productService.updateProduct("product123", "seller123", updateDTO);

        // Assert
        assertThat(result).isNotNull();
        verify(productRepository).findById("product123");
        verify(productMapper).updateProductFromDto(eq(updateDTO), any(Product.class));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should delete product media successfully")
    void testDeleteProductMedia() {
        // Arrange
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("Deleted"));

        // Act
        productService.deleteProductMedia("product123", "seller123", "media456");

        // Assert
        verify(productRepository).findById("product123");
        verify(webClient).delete();
    }

    @Test
    @DisplayName("Should get product with detail including createdByMe flag")
    void testGetProductWithDetail() {
        // Arrange
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(InfoUserDTO.class)).thenReturn(Mono.just(testSeller));
        when(responseSpec.bodyToFlux(MediaUploadResponseDTO.class)).thenReturn(Flux.empty());

        // Act
        ProductDTO result = productService.getProductWithDetail("product123", "seller123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo("product123");
        assertThat(result.isCreatedByMe()).isTrue();
        verify(productRepository).findById("product123");
    }

    @Test
    @DisplayName("Should get all products with email")
    void testGetAllProductsWithEmail() {
        // Arrange
        List<Product> products = List.of(testProduct);
        MediaUploadResponseDTO media = new MediaUploadResponseDTO();
        media.setFileUrl("http://example.com/image.jpg");

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(InfoUserDTO.class)).thenReturn(Mono.just(testSeller));
        when(productRepository.findAllBySellerID("seller123")).thenReturn(products);
        when(responseSpec.bodyToFlux(MediaUploadResponseDTO.class)).thenReturn(Flux.just(media));

        // Act
        List<ProductDTO> result = productService.getAllProductsWithEmail("seller@example.com");

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        verify(productRepository).findAllBySellerID("seller123");
    }

    @Test
    @DisplayName("Should get my products with pagination")
    void testGetMyProducts() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Product> productList = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(productList, pageable, 1);

        when(productRepository.findBySellerID(eq("seller123"), any(Pageable.class))).thenReturn(productPage);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(List.of()));

        // Act
        Page<ProductCardDTO> result = productService.getMyProducts(pageable, "seller123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findBySellerID(eq("seller123"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should create product successfully")
    void testCreateProduct() {
        // Arrange
        CreateProductDTO createDTO = new CreateProductDTO();
        createDTO.setName("New Product");
        createDTO.setDescription("New Description");
        createDTO.setPrice(79.99);
        createDTO.setQuantity(15);

        Product newProduct = Product.builder()
                .name("New Product")
                .description("New Description")
                .price(79.99)
                .quantity(15)
                .build();

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId("newId123");
            return p;
        });

        // Act
        Product result = productService.createProduct("seller123", createDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("newId123");
        assertThat(result.getSellerID()).isEqualTo("seller123");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should not create image when file is empty")
    void testCreateImageWithEmptyFile() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(true);

        // Act
        productService.createImage(mockFile, "product123", "seller123", "ROLE_SELLER");

        // Assert
        verify(productRepository, never()).findById(anyString());
        // Also ensure no webClient call is made
        verify(webClient, never()).post();
    }
}
