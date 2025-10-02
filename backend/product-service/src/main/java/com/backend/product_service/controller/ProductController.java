package com.backend.product_service.controller;package package com.backend.product_service.controller;
import com.backend.product_service.dto.CreateProductDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @PostMapping
    @PreAuthorize("hasRole('SELLER')") // Only users with ROLE_SELLER can access this
    public ResponseEntity<String> createProduct(
            @RequestBody CreateProductDTO productDto,
            @RequestHeader("X-User-ID") String sellerId) {


        return ResponseEntity.ok("Product created successfully");
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        // ... logic to get all products ...
    }
}