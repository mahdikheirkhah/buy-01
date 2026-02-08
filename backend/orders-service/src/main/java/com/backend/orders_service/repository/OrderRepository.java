package com.backend.orders_service.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderStatus;

public interface OrderRepository extends MongoRepository<Order, String>, OrderRepositoryCustom {
    Page<Order> findByUserId(String userId, Pageable pageable);

    Page<Order> findByUserIdAndIsRemovedFalse(String userId, Pageable pageable);

    List<Order> findTop20ByUserIdOrderByOrderDateDesc(String userId);

    List<Order> findByUserIdOrderByOrderDateDesc(String userId);

    List<Order> findByUserIdAndStatusOrderByOrderDateDesc(String userId, OrderStatus status);

    Optional<Order> findFirstByUserIdAndStatusOrderByOrderDateDesc(String userId, OrderStatus status);

    // Get all orders for statistics calculation
    List<Order> findAll();

    // Get all non-pending orders for seller view - filtered by status and date
    // range at DB level
    List<Order> findByStatusNot(OrderStatus status);

    /**
     * Get all non-pending orders with optional date range filtering
     * Used by seller order service to filter at database level
     */
    @Query("{ 'status': { $ne: ?0 }, " +
            "?1 : { $lte: ?2 }, " + // updatedAt >= minDate (or skip if null)
            "?3 : { $gte: ?4 } " + // updatedAt <= maxDate (or skip if null)
            "}")
    List<Order> findNonPendingOrdersWithDateRange(
            OrderStatus excludeStatus,
            String minDateField,
            Instant minDate,
            String maxDateField,
            Instant maxDate);

    /**
     * Search and filter orders for a client
     * Filters by keyword (order ID or product name), price range, date range, and
     * statuses
     */
    @Query("{ 'userId': ?0, 'isRemoved': false, " +
            "?1 : { $exists: true }, " + // keyword search on id or items.productName
            "?2 : { $exists: true }, " + // price range
            "?3 : { $exists: true } " + // date range
            "}")
    Page<Order> searchAndFilterOrders(
            String userId,
            String keyword,
            Double minPrice,
            Double maxPrice,
            Instant minUpdateDate,
            Instant maxUpdateDate,
            List<OrderStatus> statuses,
            Pageable pageable);
}
