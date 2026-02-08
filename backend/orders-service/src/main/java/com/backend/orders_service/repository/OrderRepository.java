package com.backend.orders_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderStatus;

public interface OrderRepository extends MongoRepository<Order, String> {
    Page<Order> findByUserId(String userId, Pageable pageable);

    Page<Order> findByUserIdAndIsRemovedFalse(String userId, Pageable pageable);

    List<Order> findTop20ByUserIdOrderByOrderDateDesc(String userId);

    List<Order> findByUserIdOrderByOrderDateDesc(String userId);

    List<Order> findByUserIdAndStatusOrderByOrderDateDesc(String userId, OrderStatus status);

    Optional<Order> findFirstByUserIdAndStatusOrderByOrderDateDesc(String userId, OrderStatus status);

    // Get all orders for statistics calculation
    List<Order> findAll();

    // Get all non-pending orders for seller view
    List<Order> findByStatusNot(OrderStatus status);
}