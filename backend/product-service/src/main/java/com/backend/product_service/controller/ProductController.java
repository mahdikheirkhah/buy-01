package com.backend.product_service.controller;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.service.ProductService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    public ProductController(ProductService productService, KafkaTemplate<String, String> kafkaTemplate) {
        this.productService = productService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> createProduct(
            @RequestBody @NotNull(message ="this request needs body") CreateProductDTO productDto,
            @RequestHeader("X-User-ID") String sellerId) {
        productService.createProduct(sellerId, productDto);
        return ResponseEntity.ok("Product created successfully");
    }
    @PostMapping("/create/images/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> addImagesToProduct(
            @RequestHeader("X-User-ID") String sellerId,
            @PathVariable("productId") String productId,
            @RequestParam("files") List<MultipartFile> files
    ){
        productService.createImage(files, productId, sellerId);
        return ResponseEntity.ok("Image(s) created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> updateProduct(
            @PathVariable String productId,
            @RequestBody @NotNull(message ="this request needs body") UpdateProductDTO productDto,
            @RequestHeader("X-User-ID") String sellerId ) {
        productService.updateProduct(productId, sellerId, productDto);
        return ResponseEntity.ok("Product updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<String> deleteProduct(
            @PathVariable String productId,
            @RequestHeader("X-User-ID") String sellerId){
        productService.deleteProduct(productId, sellerId);
        kafkaTemplate.send("product-deleted-topic", productId);
        return ResponseEntity.ok("Product deleted successfully");
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<ProductDTO> products = productService.getAllProductsWithDetail();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<ProductDTO>> getAllProductsBySellerId(
            @RequestHeader("X-User-ID") String sellerId) {
        List<ProductDTO> products = productService.getAllProductsWithSellerID(sellerId);
        return ResponseEntity.ok(products);
    }
    @GetMapping("/another/{email}")
    public ResponseEntity<List<ProductDTO>> getAllProductsByEmail(@PathVariable String email) {
        List<ProductDTO> products = productService.getAllProductsWithEmail(email);
        return ResponseEntity.ok(products);
    }
}