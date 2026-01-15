package com.backend.product_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.dto.ProductSimpleDTO;
import com.backend.product_service.dto.StockAdjustmentRequest;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.service.ProductService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

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

    @GetMapping("/all")
    public ResponseEntity<Page<ProductCardDTO>> getAllProducts(
            // Spring magically creates a Pageable object from URL params
            // e.g., /all?page=0&size=10&sort=createdAt,desc
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-User-ID", required = false) String sellerId) {

        Page<ProductCardDTO> page = productService.getAllProducts(pageable, sellerId);
        return ResponseEntity.ok(page);
    }

    // --- Endpoint 2: For the "My Products" page ---
    @GetMapping("/my-products")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    public ResponseEntity<Page<ProductCardDTO>> getMyProducts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-User-ID") String sellerId) { // required = true (default)

        Page<ProductCardDTO> page = productService.getMyProducts(pageable, sellerId);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')") // Make sure role name matches JWT
    public ResponseEntity<Product> createProduct(
            @RequestBody @NotNull(message = "this request needs body") CreateProductDTO productDto,
            @RequestHeader("X-User-ID") String sellerId) {
        System.out.println("Creating product " + productDto + "seller " + sellerId);
        // This service method now only saves the product and returns it
        Product newProduct = productService.createProduct(sellerId, productDto);

        // Return the full product (including its new ID) so the frontend can use it in
        // step 2
        return ResponseEntity.status(HttpStatus.CREATED).body(newProduct);
    }

    @PostMapping("/adjust-stock")
    public ResponseEntity<Void> adjustStock(@RequestBody List<StockAdjustmentRequest> adjustments) {
        productService.adjustProductStock(adjustments);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restock")
    public ResponseEntity<Void> restock(@RequestBody List<StockAdjustmentRequest> adjustments) {
        productService.restockProducts(adjustments);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create/images")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> addImagesToProduct(
            @RequestHeader("X-User-ID") String sellerId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam("productId") String productId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) { // Changed to "file" to match frontend
        // This service method now only handles saving the image
        productService.createImage(file, productId, sellerId, role);
        return ResponseEntity.ok(Map.of("message", "Image created successfully"));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    public ResponseEntity<UpdateProductDTO> updateProduct(
            @PathVariable String productId,
            @RequestBody @NotNull(message = "this request needs body") UpdateProductDTO productDto,
            @RequestHeader("X-User-ID") String sellerId) {
        UpdateProductDTO savedProduct = productService.updateProduct(productId, sellerId, productDto);
        return ResponseEntity.ok(savedProduct);
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> deleteProduct(
            @PathVariable("productId") String productId,
            @RequestHeader("X-User-ID") String sellerId) {
        productService.deleteProduct(productId, sellerId);
        return ResponseEntity.ok("Product deleted successfully");
    }

    @DeleteMapping("deleteMedia/{productId}/{mediaId}")
    @PreAuthorize("hasRole('ROLE_SELLER') || hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteMedia(
            @PathVariable("mediaId") String mediaId,
            @PathVariable("productId") String productId,
            @RequestHeader("X-User-ID") String sellerId) {
        productService.deleteProductMedia(productId, sellerId, mediaId);
        return ResponseEntity.ok(Map.of("message", "Media deleted successfully"));
    }

    @KafkaListener(topics = "user-deleted-topic", groupId = "product-service-group")
    public void handleUserDeleted(String userId) {
        System.out.println("Received user deletion event for ID: " + userId);
        productService.DeleteProductsOfUser(userId);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO> getProductWithId(@PathVariable String productId,
            @RequestHeader("X-User-ID") String userId) {
        ProductDTO product = productService.getProductWithDetail(productId, userId);
        return ResponseEntity.ok(product);
    }

    // Public endpoint for internal services (like orders-service) to fetch product
    // details without user context
    @GetMapping("/public/{productId}")
    public ResponseEntity<ProductDTO> getProductPublic(@PathVariable String productId) {
        ProductDTO product = productService.getProductWithDetail(productId, null);
        return ResponseEntity.ok(product);
    }

    // Lightweight public endpoint - returns only product DTO without heavy details
    // Used by orders-service to fetch basic product info (price, name, sellerID)
    @GetMapping("/simple/{productId}")
    public ResponseEntity<ProductSimpleDTO> getProductSimple(@PathVariable String productId) {
        ProductSimpleDTO product = productService.getProductDTOOnly(productId);
        return ResponseEntity.ok(product);
    }

    // @GetMapping("/me")
    // @PreAuthorize("hasRole('ROLE_SELLER')")
    // public ResponseEntity<List<ProductDTO>> getAllProductsBySellerId(
    // @RequestHeader("X-User-ID") String sellerId) {
    // List<ProductDTO> products =
    // productService.getAllProductsWithSellerID(sellerId);
    // return ResponseEntity.ok(products);
    // }
    @GetMapping("/another/{email}")
    public ResponseEntity<List<ProductDTO>> getAllProductsByEmail(@PathVariable String email) {
        List<ProductDTO> products = productService.getAllProductsWithEmail(email);
        return ResponseEntity.ok(products);
    }
}