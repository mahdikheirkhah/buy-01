package com.backend.product_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.backend.common.dto.InfoUserDTO;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.service.ProductService;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private ProductController productController;

    private Pageable pageable;
    private List<ProductCardDTO> productList;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        productList = new ArrayList<>();

        // Create sample product data
        ProductCardDTO product1 = new ProductCardDTO();
        product1.setName("Product 1");
        product1.setPrice(29.99);

        ProductCardDTO product2 = new ProductCardDTO();
        product2.setName("Product 2");
        product2.setPrice(39.99);

        productList.add(product1);
        productList.add(product2);
    }

    @Test
    void testGetAllProducts_Success() {
        // Arrange
        Page<ProductCardDTO> productPage = new PageImpl<>(productList, pageable, productList.size());
        when(productService.getAllProducts(any(Pageable.class), anyString())).thenReturn(productPage);

        // Act
        ResponseEntity<Page<ProductCardDTO>> result = productController.getAllProducts(pageable, "seller123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().getContent().size());
        assertEquals("Product 1", result.getBody().getContent().get(0).getName());
        verify(productService, times(1)).getAllProducts(any(Pageable.class), anyString());
    }

    @Test
    void testGetAllProducts_EmptyList() {
        // Arrange
        Page<ProductCardDTO> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        when(productService.getAllProducts(any(Pageable.class), anyString())).thenReturn(emptyPage);

        // Act
        ResponseEntity<Page<ProductCardDTO>> result = productController.getAllProducts(pageable, "seller123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().getContent().size());
        verify(productService, times(1)).getAllProducts(any(Pageable.class), anyString());
    }

    @Test
    void testGetAllProducts_WithNullSellerId() {
        // Arrange
        Page<ProductCardDTO> productPage = new PageImpl<>(productList, pageable, productList.size());
        when(productService.getAllProducts(any(Pageable.class), isNull())).thenReturn(productPage);

        // Act
        ResponseEntity<Page<ProductCardDTO>> result = productController.getAllProducts(pageable, null);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().getContent().size());
        verify(productService, times(1)).getAllProducts(any(Pageable.class), isNull());
    }

    @Test
    void testGetAllProducts_ServiceThrowsException() {
        // Arrange
        when(productService.getAllProducts(any(Pageable.class), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            productController.getAllProducts(pageable, "seller123");
        });
        verify(productService, times(1)).getAllProducts(any(Pageable.class), anyString());
    }

    @Test
    void testGetAllProducts_Pagination() {
        // Arrange
        Pageable customPageable = PageRequest.of(1, 5);
        List<ProductCardDTO> page2Products = new ArrayList<>();
        page2Products.add(productList.get(0));

        Page<ProductCardDTO> productPage = new PageImpl<>(page2Products, customPageable, 10);
        when(productService.getAllProducts(any(Pageable.class), anyString())).thenReturn(productPage);

        // Act
        ResponseEntity<Page<ProductCardDTO>> result = productController.getAllProducts(customPageable, "seller123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getContent().size());
        assertEquals(10, result.getBody().getTotalElements());
        assertEquals(1, result.getBody().getNumber()); // Page number
        verify(productService, times(1)).getAllProducts(any(Pageable.class), anyString());
    }

    @Test
    void testGetMyProducts_Success() {
        // Arrange
        Page<ProductCardDTO> productPage = new PageImpl<>(productList, pageable, productList.size());
        when(productService.getMyProducts(any(Pageable.class), eq("seller123"))).thenReturn(productPage);

        // Act
        ResponseEntity<Page<ProductCardDTO>> result = productController.getMyProducts(pageable, "seller123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().getContent().size());
        verify(productService, times(1)).getMyProducts(any(Pageable.class), eq("seller123"));
    }

    @Test
    void testCreateProduct_Success() {
        // Arrange
        CreateProductDTO createDTO = new CreateProductDTO();
        createDTO.setName("New Product");
        createDTO.setDescription("Product Description");
        createDTO.setPrice(49.99);
        createDTO.setQuantity(5);

        Product createdProduct = Product.builder()
                .id("newProduct123")
                .name("New Product")
                .description("Product Description")
                .price(49.99)
                .quantity(5)
                .sellerID("seller123")
                .build();

        when(productService.createProduct(eq("seller123"), any(CreateProductDTO.class))).thenReturn(createdProduct);

        // Act
        ResponseEntity<Product> result = productController.createProduct(createDTO, "seller123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("newProduct123", result.getBody().getId());
        assertEquals("New Product", result.getBody().getName());
        verify(productService, times(1)).createProduct(eq("seller123"), any(CreateProductDTO.class));
    }

    @Test
    void testAddImagesToProduct_Success() {
        // Arrange
        MultipartFile mockFile = mock(MultipartFile.class);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        doNothing().when(productService).createImage(any(MultipartFile.class), eq("product123"), eq("seller123"),
                eq("ROLE_SELLER"));

        // Act
        ResponseEntity<Map<String, String>> result = productController.addImagesToProduct(
                "seller123", "ROLE_SELLER", "product123", mockFile, mockRequest);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Image created successfully", result.getBody().get("message"));
        verify(productService, times(1)).createImage(any(MultipartFile.class), eq("product123"), eq("seller123"),
                eq("ROLE_SELLER"));
    }

    @Test
    void testUpdateProduct_Success() {
        // Arrange
        UpdateProductDTO updateDTO = UpdateProductDTO.builder()
                .name("Updated Product")
                .description("Updated Description")
                .price(79.99)
                .quantity(15)
                .build();

        when(productService.updateProduct(eq("product123"), eq("seller123"), any(UpdateProductDTO.class)))
                .thenReturn(updateDTO);

        // Act
        ResponseEntity<UpdateProductDTO> result = productController.updateProduct("product123", updateDTO, "seller123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Updated Product", result.getBody().getName());
        assertEquals(79.99, result.getBody().getPrice());
        verify(productService, times(1)).updateProduct(eq("product123"), eq("seller123"), any(UpdateProductDTO.class));
    }

    @Test
    void testDeleteProduct_Success() {
        // Arrange
        doNothing().when(productService).deleteProduct(eq("product123"), eq("seller123"));

        // Act
        ResponseEntity<String> result = productController.deleteProduct("product123", "seller123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Product deleted successfully", result.getBody());
        verify(productService, times(1)).deleteProduct(eq("product123"), eq("seller123"));
    }

    @Test
    void testDeleteMedia_Success() {
        // Arrange
        doNothing().when(productService).deleteProductMedia(eq("product123"), eq("seller123"), eq("media456"));

        // Act
        ResponseEntity<Map<String, String>> result = productController.deleteMedia("media456", "product123",
                "seller123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Media deleted successfully", result.getBody().get("message"));
        verify(productService, times(1)).deleteProductMedia(eq("product123"), eq("seller123"), eq("media456"));
    }

    @Test
    void testGetProductWithId_Success() {
        // Arrange
        Product product = Product.builder()
                .id("product123")
                .name("Test Product")
                .price(99.99)
                .sellerID("seller123")
                .build();
        InfoUserDTO mockSeller = InfoUserDTO.builder()
                .id("seller123")
                .firstName("John")
                .lastName("Seller")
                .email("seller@example.com")
                .build();
        ProductDTO productDTO = new ProductDTO(product, mockSeller, null);
        productDTO.setProductId("product123");

        when(productService.getProductWithDetail(eq("product123"), eq("user123"))).thenReturn(productDTO);

        // Act
        ResponseEntity<ProductDTO> result = productController.getProductWithId("product123", "user123");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("product123", result.getBody().getProductId());
        assertEquals("Test Product", result.getBody().getName());
        verify(productService, times(1)).getProductWithDetail(eq("product123"), eq("user123"));
    }

    @Test
    void testGetAllProductsByEmail_Success() {
        // Arrange
        Product prod1 = Product.builder().id("product1").name("Product 1").sellerID("seller123").build();
        Product prod2 = Product.builder().id("product2").name("Product 2").sellerID("seller123").build();
        InfoUserDTO mockSeller = InfoUserDTO.builder()
                .id("seller123")
                .firstName("John")
                .lastName("Seller")
                .email("seller@example.com")
                .build();

        ProductDTO product1 = new ProductDTO(prod1, mockSeller, null);
        product1.setProductId("product1");

        ProductDTO product2 = new ProductDTO(prod2, mockSeller, null);
        product2.setProductId("product2");

        List<ProductDTO> products = List.of(product1, product2);
        when(productService.getAllProductsWithEmail(eq("seller@example.com"))).thenReturn(products);

        // Act
        ResponseEntity<List<ProductDTO>> result = productController.getAllProductsByEmail("seller@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
        assertEquals("product1", result.getBody().get(0).getProductId());
        verify(productService, times(1)).getAllProductsWithEmail(eq("seller@example.com"));
    }
}
