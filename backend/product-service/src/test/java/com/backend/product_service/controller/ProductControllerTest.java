package com.backend.product_service.controller;

import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.service.ProductService;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
}

