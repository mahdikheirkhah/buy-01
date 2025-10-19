package com.backend.product_service.service;

import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.exception.CustomException;
import com.backend.common.dto.InfoUserDTO;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public ProductService(ProductRepository productRepository,  ProductMapper productMapper, WebClient.Builder webClientBuilder, KafkaTemplate<String, String> kafkaTemplate) {
        this.productMapper = productMapper;
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

        List<String> sellerIds = new ArrayList<>();
        sellerIds.add(product.getSellerID());
        List<InfoUserDTO> seller = getSellersInfo(sellerIds);
        return new ProductDTO(product, seller.get(0), getMedia(productID));
    }

    public Product getProduct(String productID) {
        return productRepository.findById(productID)
                .orElseThrow(() -> new CustomException("Product not found", HttpStatus.NOT_FOUND));
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public void createProduct(String sellerId,CreateProductDTO productDto) {
        Product product = productDto.toProduct();
        if (checkId(sellerId)) {
            throw new CustomException("Seller ID is null", HttpStatus.UNAUTHORIZED);
        }
        product.setSellerID(sellerId);
        productRepository.save(product);
    }

    public void updateProduct(String productId, String sellerId ,UpdateProductDTO productDto) {
        Product existingProduct = checkProduct(productId, sellerId);
        productMapper.updateProductFromDto(productDto, existingProduct);
        productRepository.save(existingProduct);
    }
    public void DeleteProductsOfUser(String sellerId) {
        List<Product> products = productRepository.findAllBySellerID(sellerId);
        if (products.isEmpty()) {
            return;
        }
        for (Product product : products) {
            kafkaTemplate.send("product-deleted-topic", product.getID());
            productRepository.delete(product);
        }
    }
    public void deleteProduct(String productId, String sellerId) {
        Product existingProduct = checkProduct(productId, sellerId);
        kafkaTemplate.send("product-deleted-topic", productId);
        productRepository.delete(existingProduct);
    }

    public List<ProductDTO> getAllProductsWithDetail() {

        List<Product> products = productRepository.findAll();

        if (products.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductDTO> result;
        List<String> sellerIds = products.stream()
                .map(Product::getSellerID)
                .distinct()
                .collect(Collectors.toList());
        List<InfoUserDTO> sellers = getSellersInfo(sellerIds);
        result = appendSellersToProduct(products, sellers);
        for (ProductDTO productDTO : result) {
            productDTO.setMedia(getMedia(productDTO.getProductId()));
        }

        return result;
    }
    public List<ProductDTO> getAllProductsWithEmail(String email) {
         InfoUserDTO seller = getSellerInfoWithEmail(email);
         List<Product> products = productRepository.findAllBySellerID(seller.getId());
         if (products.isEmpty()) {
             return Collections.emptyList();
         }
         List<ProductDTO> result;
         result = appendSellersToProduct(products,Collections.singletonList(seller));
         for (ProductDTO productDTO : result) {
             productDTO.setMedia(getMedia(productDTO.getProductId()));

         }
         return result;
    }
    public List<ProductDTO> getAllProductsWithSellerID(String sellerId) {
        List<Product> products = productRepository.findAllBySellerID(sellerId);
        if (products.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductDTO> result;
        List<String> sellerIds = new ArrayList<>();
        sellerIds.add(sellerId);
        List<InfoUserDTO> seller = getSellersInfo(sellerIds);
        result = appendSellersToProduct(products, seller);
        for (ProductDTO productDTO : result) {
            productDTO.setMedia(getMedia(productDTO.getProductId()));
        }
        return result;
    }

    private List<ProductDTO> appendSellersToProduct(List<Product> products,List<InfoUserDTO>  sellers) {
        assert sellers != null;
        Map<String, InfoUserDTO> sellerMap = sellers.stream()
                .collect(Collectors.toMap(InfoUserDTO::getId, user -> user));

        return products.stream().map(product -> {
            InfoUserDTO seller = sellerMap.get(product.getSellerID());
            return new ProductDTO(product, seller, null);
        }).collect(Collectors.toList());
    }
    public void createImage(List<MultipartFile> files, String productId, String sellerId)  {
        if (files == null || files.isEmpty()) {
                return;
        }
        Product product = checkProduct(productId, sellerId);
        for (MultipartFile file : files) {
            saveProductImage(file,productId);
        }
        return;
    }
    public String saveProductImage(MultipartFile image, String productId) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", image.getResource());
        MediaUploadResponseDTO mediaResponse = webClientBuilder.build().post()
                .uri("https://MEDIA-SERVICE/api/media/upload/product/"+productId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(MediaUploadResponseDTO.class)
                .block(); // .block() makes the call synchronous. A reactive chain is more advanced.

        if (mediaResponse == null || mediaResponse.getFileUrl().isBlank()) {
            throw new CustomException("Failed to upload avatar image.", HttpStatus.BAD_REQUEST);
        }
        return mediaResponse.getFileUrl();
    }
    private List<InfoUserDTO> getSellersInfo(List<String> sellerIds) {
        return webClientBuilder.build().get()
                .uri("https://USER-SERVICE/api/users/batch?ids=" + String.join(",", sellerIds))
                .retrieve()
                .bodyToFlux(InfoUserDTO.class) // Use Flux for a list
                .collectList()
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
