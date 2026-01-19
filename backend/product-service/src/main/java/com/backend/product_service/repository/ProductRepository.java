package com.backend.product_service.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.backend.product_service.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {

    // Find products by seller with pagination
    Page<Product> findBySellerID(String sellerId, Pageable pageable);

    List<Product> findAllBySellerID(String sellerId);

    /**
     * Search and filter products with dynamic criteria
     * MongoDB query with optional filters for keyword, price, quantity, and date
     * range
     */
    @Query("{ " +
            "$and: [ " +
            "  { $or: [ " +
            "    { $expr: { $eq: [?0, null] } }, " +
            "    { name: { $regex: ?0, $options: 'i' } }, " +
            "    { description: { $regex: ?0, $options: 'i' } } " +
            "  ]}, " +
            "  { $or: [ { $expr: { $eq: [?1, null] } }, { price: { $gte: ?1 } } ]}, " +
            "  { $or: [ { $expr: { $eq: [?2, null] } }, { price: { $lte: ?2 } } ]}, " +
            "  { $or: [ { $expr: { $eq: [?3, null] } }, { quantity: { $gte: ?3 } } ]}, " +
            "  { $or: [ { $expr: { $eq: [?4, null] } }, { quantity: { $lte: ?4 } } ]}, " +
            "  { $or: [ { $expr: { $eq: [?5, null] } }, { createdAt: { $gte: ?5 } } ]}, " +
            "  { $or: [ { $expr: { $eq: [?6, null] } }, { createdAt: { $lte: ?6 } } ]} " +
            "] " +
            "}")
    Page<Product> searchAndFilterProducts(
            String keyword,
            Double minPrice,
            Double maxPrice,
            Integer minQuantity,
            Integer maxQuantity,
            Instant startDate,
            Instant endDate,
            Pageable pageable);
}