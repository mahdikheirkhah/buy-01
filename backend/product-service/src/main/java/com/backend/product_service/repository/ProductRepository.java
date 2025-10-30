package com.backend.product_service.repository;

import com.backend.product_service.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository; // <-- Change this back

import java.util.List;
// PagingAndSortingRepository is no longer needed here

public interface ProductRepository extends MongoRepository<Product, String> { // <-- Use MongoRepository

    // This new method will find all products by a seller, with pagination
    Page<Product> findBySellerID(String sellerId, Pageable pageable);

    List<Product> findAllBySellerID(String sellerId);
    // You now get all these methods:
    // 1. Basic CRUD: save(), findById(), delete() (from CrudRepository)
    // 2. Paging/Sorting: findAll(Pageable), findAll(Sort) (from PagingAndSortingRepository)
    // 3. Mongo-specific: insert(), findAll(Example) (from MongoRepository)
}