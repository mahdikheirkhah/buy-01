package com.backend.product_service.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.exception.CustomException;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.dto.ProductSimpleDTO;
import com.backend.product_service.dto.StockAdjustmentRequest;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public ProductService(ProductRepository productRepository,
            WebClient.Builder webClientBuilder, KafkaTemplate<String, String> kafkaTemplate) {
        this.productRepository = productRepository;
        this.webClientBuilder = webClientBuilder;
        this.kafkaTemplate = kafkaTemplate;
    }

    public ProductDTO getProductByProductID(String productID) {
        Product product = productRepository.findById(productID)
                .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));
        if (product == null) {
            return null;
        }
        InfoUserDTO seller = getSellersInfo(product.getSellerID());
        return new ProductDTO(product, seller, getMedia(productID));
    }

    public Product getProduct(String productID) {
        System.out.println("Get product by productID: " + productID);
        return productRepository.findById(productID)
                .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Get product DTO with minimal details (just product info from database)
     * Used for internal service-to-service calls that only need basic product data
     * No external calls - returns data directly from database
     */
    public ProductSimpleDTO getProductDTOOnly(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));
        if (product == null) {
            return null;
        }
        // Return simple DTO with just product data including sellerID
        return new ProductSimpleDTO(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public UpdateProductDTO updateProduct(String productId, String sellerId, UpdateProductDTO productDto) {
        Product existingProduct = checkProduct(productId, sellerId);
        // Manual mapping to avoid MapStruct bean dependency
        if (productDto.getName() != null) {
            existingProduct.setName(productDto.getName());
        }
        if (productDto.getDescription() != null) {
            existingProduct.setDescription(productDto.getDescription());
        }
        if (productDto.getPrice() != null) {
            existingProduct.setPrice(productDto.getPrice());
        }
        if (productDto.getQuantity() != null) {
            existingProduct.setQuantity(productDto.getQuantity());
        }
        Product savedProduct = productRepository.save(existingProduct);
        return UpdateProductDTO.builder()
                .name(savedProduct.getName())
                .description(savedProduct.getDescription())
                .price(savedProduct.getPrice())
                .quantity(savedProduct.getQuantity())
                .build();
    }

    public void DeleteProductsOfUser(String sellerId) {
        List<Product> products = productRepository.findAllBySellerID(sellerId);
        if (products.isEmpty()) {
            return;
        }
        for (Product product : products) {
            kafkaTemplate.send("product-deleted-topic", product.getId());
            productRepository.delete(product);
        }
    }

    public void deleteProduct(String productId, String sellerId) {
        Product existingProduct = checkProduct(productId, sellerId);
        kafkaTemplate.send("product-deleted-topic", productId);
        productRepository.delete(existingProduct);
    }

    public void deleteProductMedia(String productId, String sellerId, String mediaId) {
        Product existingProduct = checkProduct(productId, sellerId);
        deleteMedia(mediaId);
    }

    public ProductDTO getProductWithDetail(String productId, String userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));
        if (product == null) {
            return null;
        }

        InfoUserDTO seller = getSellersInfo(product.getSellerID());

        List<MediaUploadResponseDTO> media = getMedia(product.getId());
        ProductDTO productDTO = new ProductDTO(product, seller, media);
        productDTO.setCreatedByMe(product.getSellerID().equals(userId));
        return productDTO;
    }

    public List<ProductDTO> getAllProductsWithEmail(String email) {
        InfoUserDTO seller = getSellerInfoWithEmail(email);
        if (seller == null) {
            throw new CustomException("Seller not found", HttpStatus.NOT_FOUND);
        }
        List<Product> products = productRepository.findAllBySellerID(seller.getId());
        if (products.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductDTO> result;
        result = appendSellersToProduct(products, Collections.singletonList(seller));
        for (ProductDTO productDTO : result) {
            productDTO.setMedia(getMedia(productDTO.getProductId()));

        }
        return result;
    }

    // --- New Method 1: Get ALL products (for Home page) ---
    public Page<ProductCardDTO> getAllProducts(Pageable pageable, String sellerId) {
        Page<Product> productPage = productRepository.findAll(pageable);
        return convertToCardDTOPage(productPage, sellerId);
    }

    // --- New Method 2: Get products for a specific seller (for My Products page)
    // ---
    public Page<ProductCardDTO> getMyProducts(Pageable pageable, String sellerId) {
        Page<Product> productPage = productRepository.findBySellerID(sellerId, pageable);
        return convertToCardDTOPage(productPage, sellerId);
    }

    private Page<ProductCardDTO> convertToCardDTOPage(Page<Product> productPage, String sellerId) {
        return productPage.map(product -> {

            boolean isCreator = (product.getSellerID().equals(sellerId));

            List<String> limitedImages = getLimitedImageUrls(product.getId(), 3);

            return new ProductCardDTO(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getQuantity(),
                    isCreator,
                    limitedImages);
        });
    }

    /**
     * New private helper to fetch limited image URLs from Media Service
     */
    private List<String> getLimitedImageUrls(String productId, int limit) {
        try {
            // This is the correct type for deserializing a generic list
            ParameterizedTypeReference<List<String>> listType = new ParameterizedTypeReference<>() {
            };

            return webClientBuilder.build().get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("MEDIA-SERVICE")
                            .path("/api/media/product/{productId}/urls")
                            .queryParam("limit", limit)
                            .build(productId))
                    .retrieve()
                    // ✅ THE FIX: Deserialize directly into a List<String>
                    .bodyToMono(listType)
                    .block(); // .block() is acceptable here
        } catch (Exception e) {
            System.err.println("Failed to fetch media URLs for product " + productId + ": " + e.getMessage());
            return List.of();
        }
    }

    private List<ProductDTO> appendSellersToProduct(List<Product> products, List<InfoUserDTO> sellers) {
        assert sellers != null;
        Map<String, InfoUserDTO> sellerMap = sellers.stream()
                .collect(Collectors.toMap(InfoUserDTO::getId, user -> user));

        return products.stream().map(product -> {
            InfoUserDTO seller = sellerMap.get(product.getSellerID());
            return new ProductDTO(product, seller, null);
        }).collect(Collectors.toList());
    }

    public Product createProduct(String sellerId, CreateProductDTO productDto) {
        Product product = productDto.toProduct();
        if (checkId(sellerId)) {
            throw new CustomException("Seller ID is null", HttpStatus.UNAUTHORIZED);
        }
        product.setSellerID(sellerId);

        // Save and return the product so we can get its ID
        return productRepository.save(product);
    }

    public void adjustProductStock(List<StockAdjustmentRequest> adjustments) {
        if (adjustments == null || adjustments.isEmpty()) {
            return;
        }

        for (StockAdjustmentRequest adjustment : adjustments) {
            Product product = productRepository.findById(adjustment.getProductId())
                    .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));

            if (product.getQuantity() < adjustment.getQuantity()) {
                throw new CustomException(
                        String.format("Product '%s' is out of stock. Available: %d, Requested: %d",
                                product.getName(), product.getQuantity(), adjustment.getQuantity()),
                        HttpStatus.BAD_REQUEST);
            }

            product.setQuantity(product.getQuantity() - adjustment.getQuantity());
            productRepository.save(product);
        }
    }

    public void restockProducts(List<StockAdjustmentRequest> adjustments) {
        if (adjustments == null || adjustments.isEmpty()) {
            return;
        }

        for (StockAdjustmentRequest adjustment : adjustments) {
            Product product = productRepository.findById(adjustment.getProductId())
                    .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));

            product.setQuantity(product.getQuantity() + adjustment.getQuantity());
            productRepository.save(product);
        }
    }

    public void createImage(MultipartFile file, String productId, String sellerId, String role) {
        if (file == null || file.isEmpty()) {
            return;
        }
        // Check that the seller owns this product
        Product product = checkProduct(productId, sellerId);
        // Save the single image

        saveProductImage(file, productId, role);
    }

    public String saveProductImage(MultipartFile image, String productId, String role) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        try {
            // ✅ Use ByteArrayResource, which is more reliable
            ByteArrayResource fileResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            };
            body.add("file", fileResource);
            body.add("productId", productId); // Send productId as a part

        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new CustomException("Failed to read image file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        MediaUploadResponseDTO mediaResponse = webClientBuilder.build().post()
                // ✅ Make sure this URL is correct for your media-service
                .uri("https://MEDIA-SERVICE/api/media/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("X-User-Role", role)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(MediaUploadResponseDTO.class)
                .block();

        if (mediaResponse == null || mediaResponse.getFileUrl().isBlank()) {
            throw new CustomException("Failed to upload product image.", HttpStatus.BAD_REQUEST);
        }
        return mediaResponse.getFileUrl();
    }

    private InfoUserDTO getSellersInfo(String sellerId) {
        return webClientBuilder.build().get()
                .uri("https://USER-SERVICE/api/users/seller?id={sellerId}", sellerId) // Use URI variable
                .retrieve()
                .bodyToMono(InfoUserDTO.class) // ✅ FIX: Expect a single object
                .block();
    }

    private InfoUserDTO getSellerInfoWithEmail(String email) {
        return webClientBuilder.build().get()
                .uri("https://USER-SERVICE/api/users/email?email=" + email)
                .retrieve()
                .bodyToMono(InfoUserDTO.class)
                .block();
    }

    private List<MediaUploadResponseDTO> getMedia(String productId) {
        return webClientBuilder.build().get()
                .uri("https://MEDIA-SERVICE/api/media/batch?productID={productId}", productId)
                .retrieve()
                .bodyToFlux(MediaUploadResponseDTO.class)
                .collectList()
                .block();
    }

    private String deleteMedia(String mediaId) {
        System.out.println("sending the delete request");
        return webClientBuilder.build().delete()
                .uri("https://MEDIA-SERVICE/api/media/{mediaId}", mediaId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private boolean checkId(String id) {
        return id == null || id.isBlank();
    }

    private Product checkProduct(String productId, String sellerId) {
        if (checkId(productId)) {
            throw new CustomException("Seller ID is null", HttpStatus.UNAUTHORIZED);
        }
        if (checkId(sellerId)) {
            throw new CustomException("product id is null", HttpStatus.BAD_REQUEST);
        }
        Product existingProduct = getProduct(productId);
        if (!existingProduct.getSellerID().equals(sellerId)) {
            throw new CustomException("Access Denied", HttpStatus.FORBIDDEN);
        }
        return existingProduct;
    }

}
