# Test Templates for Backend Services

Use these templates to quickly create tests for ProductService, MediaService, and Controllers.

---

## Template 1: Service Unit Test

Use this template for `ProductServiceUnitTest.java`, `MediaServiceUnitTest.java`, etc.

```java
package com.backend.product_service.service;

import com.backend.common.dto.Role;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceUnitTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id("product123")
                .name("Test Product")
                .description("Test Description")
                .price(99.99)
                .sellerId("seller123")
                .build();
    }

    @Test
    @DisplayName("Should create product successfully")
    void testCreateProductSuccessfully() {
        // Arrange
        Product newProduct = Product.builder()
                .name("New Product")
                .description("New Description")
                .price(49.99)
                .sellerId("seller456")
                .build();

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId("newId");
            return p;
        });

        // Act
        Product created = productService.createProduct(newProduct);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo("newId");
        assertThat(created.getName()).isEqualTo("New Product");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should find product by ID")
    void testFindByIdSuccess() {
        // Arrange
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));

        // Act
        Optional<Product> found = productRepository.findById("product123");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("Should update product successfully")
    void testUpdateProductSuccess() {
        // Arrange
        Product updated = Product.builder()
                .id("product123")
                .name("Updated Product")
                .description("Updated Description")
                .price(59.99)
                .build();

        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updated);

        // Act
        Product result = productService.updateProduct("product123", updated);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Product");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should delete product successfully")
    void testDeleteProductSuccess() {
        // Arrange
        when(productRepository.findById("product123")).thenReturn(Optional.of(testProduct));

        // Act
        productService.deleteProduct("product123");

        // Assert
        verify(productRepository).deleteById("product123");
    }
}
```

---

## Template 2: Service Integration Test

Use this template for `ProductServiceIntegrationTest.java`, `MediaServiceIntegrationTest.java`, etc.

```java
package com.backend.product_service.service;

import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@DisplayName("ProductService Integration Tests")
class ProductServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:7.0")
    ).withReuse(true);

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    ).withReuse(true);

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        String mongoUri = "mongodb://" + mongoDBContainer.getHost() + ":" 
            + mongoDBContainer.getFirstMappedPort() + "/testdb";
        registry.add("spring.data.mongodb.uri", () -> mongoUri);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create and persist product to MongoDB")
    void testCreateProductIntegration() {
        // Arrange
        Product newProduct = Product.builder()
                .name("Integration Product")
                .description("Test Description")
                .price(99.99)
                .sellerId("seller123")
                .build();

        // Act
        Product created = productService.createProduct(newProduct);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();

        // Verify persistence
        Product saved = productRepository.findById(created.getId()).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("Integration Product");
    }

    @Test
    @DisplayName("Should find products by seller ID")
    void testFindBySellerIdIntegration() {
        // Arrange
        Product p1 = Product.builder()
                .name("Product 1")
                .sellerId("seller123")
                .price(50.0)
                .build();
        Product p2 = Product.builder()
                .name("Product 2")
                .sellerId("seller123")
                .price(75.0)
                .build();

        productRepository.save(p1);
        productRepository.save(p2);

        // Act
        var products = productRepository.findBySellerIdAndStatus("seller123", "ACTIVE");

        // Assert
        assertThat(products).hasSize(2);
    }

    @Test
    @DisplayName("Should update product and persist changes")
    void testUpdateProductIntegration() {
        // Arrange
        Product product = Product.builder()
                .name("Original Name")
                .price(50.0)
                .sellerId("seller123")
                .build();
        Product saved = productRepository.save(product);

        // Act
        saved.setName("Updated Name");
        saved.setPrice(75.0);
        productRepository.save(saved);

        // Assert
        Product retrieved = productRepository.findById(saved.getId()).orElse(null);
        assertThat(retrieved.getName()).isEqualTo("Updated Name");
        assertThat(retrieved.getPrice()).isEqualTo(75.0);
    }
}
```

---

## Template 3: Controller Unit Test

Use this template for `UserControllerTest.java`, `ProductControllerTest.java`, etc.

```java
package com.backend.product_service.controller;

import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ProductController Unit Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id("product123")
                .name("Test Product")
                .description("Test Description")
                .price(99.99)
                .sellerId("seller123")
                .build();
    }

    @Test
    @DisplayName("Should get product by ID and return 200")
    void testGetProductByIdSuccess() throws Exception {
        // Arrange
        when(productService.getProductById("product123"))
                .thenReturn(Optional.of(testProduct));

        // Act & Assert
        mockMvc.perform(get("/api/products/product123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("product123"))
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    @DisplayName("Should return 404 when product not found")
    void testGetProductNotFound() throws Exception {
        // Arrange
        when(productService.getProductById("nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/products/nonexistent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should create product and return 201")
    void testCreateProductSuccess() throws Exception {
        // Arrange
        ProductDTO dto = ProductDTO.builder()
                .name("New Product")
                .description("New Description")
                .price(49.99)
                .build();

        when(productService.createProduct(any()))
                .thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("product123"));
    }

    @Test
    @DisplayName("Should update product and return 200")
    void testUpdateProductSuccess() throws Exception {
        // Arrange
        ProductDTO dto = ProductDTO.builder()
                .name("Updated Product")
                .price(59.99)
                .build();

        testProduct.setName("Updated Product");
        when(productService.updateProduct("product123", testProduct))
                .thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(put("/api/products/product123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"));
    }

    @Test
    @DisplayName("Should delete product and return 204")
    void testDeleteProductSuccess() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/products/product123"))
                .andExpect(status().isNoContent());
    }
}
```

---

## How to Use These Templates

### Step 1: Copy Template to New Service
```bash
# Example: Creating ProductService tests
cp backend/user-service/src/test/java/.../UserServiceUnitTest.java \
   backend/product-service/src/test/java/.../ProductServiceUnitTest.java
```

### Step 2: Replace Class Names
```java
// Change from:
class UserServiceUnitTest {
    private UserService userService;

// To:
class ProductServiceUnitTest {
    private ProductService productService;
```

### Step 3: Replace Domain Objects
```java
// Change from:
User testUser = User.builder()...

// To:
Product testProduct = Product.builder()...
```

### Step 4: Replace Method Calls
```java
// Change from:
userService.registerUser()

// To:
productService.createProduct()
```

### Step 5: Add Custom Tests
Add tests specific to the service's business logic.

---

## Test Coverage Checklist

For each service, ensure you test:

### Unit Tests
- [ ] Create/Register entity
- [ ] Find by ID
- [ ] Find by email/unique identifier
- [ ] Update entity
- [ ] Delete entity
- [ ] Handle duplicate entries
- [ ] Error cases
- [ ] Default values
- [ ] Data validation

### Integration Tests
- [ ] Persistence to database
- [ ] Query multiple records
- [ ] Transaction rollback
- [ ] Concurrent operations
- [ ] Kafka message publishing
- [ ] Data integrity

### Controller Tests
- [ ] GET requests (success, not found)
- [ ] POST requests (success, validation errors)
- [ ] PUT requests (success, not found)
- [ ] DELETE requests (success, not found)
- [ ] Status codes (200, 201, 204, 400, 404, 500)
- [ ] Error responses
- [ ] Authentication/Authorization

---

## Quick Copy-Paste Commands

### Create ProductService Tests
```bash
# Navigate to product-service
cd backend/product-service/src/test/java/com/backend/product_service/service

# Create files from templates above
# Update class names and domain objects
# Run tests:
mvn test -Dtest=ProductServiceUnitTest
mvn test -Dtest=ProductServiceIntegrationTest
```

### Create MediaService Tests
```bash
# Navigate to media-service
cd backend/media-service/src/test/java/com/backend/media_service/service

# Create files from templates
# Focus on file upload/handling tests
# Run tests:
mvn test -Dtest=MediaServiceUnitTest
mvn test -Dtest=MediaServiceIntegrationTest
```

### Create ControllerTests
```bash
# For each controller, create tests
# Use MockMvc for HTTP testing
# Mock service layer
# Test all HTTP methods and status codes
```

---

## Common Customizations

### For Kafka-Heavy Services
Add Kafka consumer tests:
```java
@Test
void testKafkaMessagePublishing() {
    // Arrange
    when(kafkaTemplate.send(anyString(), any()))
            .thenReturn(new SettableListenableFuture<>());
    
    // Act
    service.publishEvent(someEvent);
    
    // Assert
    verify(kafkaTemplate).send(anyString(), any());
}
```

### For File Upload Services (MediaService)
Add file handling tests:
```java
@Test
void testFileUpload() throws IOException {
    // Arrange
    MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "content".getBytes());
    
    // Act
    String fileUrl = mediaService.uploadFile(file);
    
    // Assert
    assertThat(fileUrl).isNotNull();
    assertThat(fileUrl).contains("uploads/");
}
```

### For Query Methods
Test custom repository queries:
```java
@Test
void testFindByCustomCriteria() {
    // Arrange
    // Create test data with specific criteria
    
    // Act
    List<Product> results = productRepository
            .findByPriceGreaterThanAndCategoryEquals(50.0, "Electronics");
    
    // Assert
    assertThat(results).hasSize(expectedCount);
    assertThat(results.get(0).getPrice()).isGreaterThan(50.0);
}
```

---

## References

- **Unit Test Template** → Use for service logic testing
- **Integration Test Template** → Use with Testcontainers
- **Controller Test Template** → Use for HTTP endpoint testing

Apply these patterns to all backend services for consistent, comprehensive test coverage!

