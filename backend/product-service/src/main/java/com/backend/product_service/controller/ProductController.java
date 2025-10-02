package com.backend.product_service.controller;package package com.backend.product_service.controller;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.service.ProductService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    @PostMapping
    @PreAuthorize("hasRole('SELLER')") // Only users with ROLE_SELLER can access this
    public ResponseEntity<String> createProduct(
            @RequestBody @NotNull(message ="this request needs body") CreateProductDTO productDto,
            @RequestHeader("X-User-ID") String sellerId) {

        productService.createProduct(sellerId, productDto);
        return ResponseEntity.ok("Product created successfully");
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
        return ResponseEntity.ok("Product deleted successfully");
    }
    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<Product>> getAllProductsBySeller() {
        List<Product> products = productService.getAllProducts();
        for (Product product : products) {

        }
        return ResponseEntity.ok(products);
    }
    GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<Product>> getAllProductsByClient() {}

    GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<Product>> getAllProductsBySellerId(
            @RequestHeader("X-User-ID") String sellerId) {
    }
}