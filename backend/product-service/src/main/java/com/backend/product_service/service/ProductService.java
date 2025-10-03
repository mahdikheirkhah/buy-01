package com.backend.product_service.service;

import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.exception.CustomException;
import com.backend.common.dto.InfoUserDTO;
import com.backend.product_service.dto.CreateProductDTO;
import com.backend.product_service.dto.ProductDTO;
import com.backend.product_service.dto.UpdateProductDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductMapper;
import com.backend.product_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public ProductService(ProductRepository productRepository,  ProductMapper productMapper, WebClient.Builder webClientBuilder) {
        this.productMapper = productMapper;
        this.productRepository = productRepository;
        this.webClientBuilder = webClientBuilder;
    }
    public Product getProduct(String id) {
        return productRepository.findById(id)
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

    public void deleteProduct(String productId, String sellerId) {
        Product existingProduct = checkProduct(productId, sellerId);
        productRepository.delete(existingProduct);
    }

    public List<ProductDTO> getAllProductsWithSellerInfo() {

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

    public List<ProductDTO> getAllMyProducts(String sellerId) {
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

    private List<InfoUserDTO> getSellersInfo(List<String> sellerIds) {
        return webClientBuilder.build().get()
                .uri("http://USER-SERVICE/api/users/batch?ids=" + String.join(",", sellerIds))
                .retrieve()
                .bodyToFlux(InfoUserDTO.class) // Use Flux for a list
                .collectList()
                .block();
    }
    private List<MediaUploadResponseDTO> getMedia(String productId) {
        return webClientBuilder.build().get()
                .uri("http://MEDIA-SERVICE/api/media/batch?productID={productId}", productId)
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
