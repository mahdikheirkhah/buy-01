package com.backend.product_service.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.backend.product_service.model.Product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom repository implementation for complex Product queries
 * Uses MongoTemplate to dynamically build queries based on provided filters
 */
@RequiredArgsConstructor
@Slf4j
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    /**
     * Search and filter products with dynamic criteria
     * Only applies filters for non-null parameters
     *
     * @param keyword     - Search in name and description (case-insensitive)
     * @param minPrice    - Minimum price filter
     * @param maxPrice    - Maximum price filter
     * @param minQuantity - Minimum quantity filter
     * @param maxQuantity - Maximum quantity filter
     * @param startDate   - Start of date range (products created after this date)
     * @param endDate     - End of date range (products created before this date)
     * @param pageable    - Pagination info
     * @return Page of products matching the criteria
     */
    @Override
    public Page<Product> searchAndFilterProducts(
            String keyword,
            Double minPrice,
            Double maxPrice,
            Integer minQuantity,
            Integer maxQuantity,
            Instant startDate,
            Instant endDate,
            Pageable pageable) {

        log.info("Searching with filters - keyword: {}, price: {}-{}, qty: {}-{}, dates: {} to {}",
                keyword, minPrice, maxPrice, minQuantity, maxQuantity, startDate, endDate);

        // Build criteria dynamically based on provided filters
        List<Criteria> criteria = new ArrayList<>();

        // Keyword filter - search in name or description (case-insensitive)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String sanitized = keyword.trim();
            Pattern pattern = Pattern.compile(Pattern.quote(sanitized), Pattern.CASE_INSENSITIVE);

            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("name").regex(pattern),
                    Criteria.where("description").regex(pattern));
            criteria.add(keywordCriteria);
        }

        // Price filters
        if (minPrice != null) {
            criteria.add(Criteria.where("price").gte(minPrice));
        }
        if (maxPrice != null) {
            criteria.add(Criteria.where("price").lte(maxPrice));
        }

        // Quantity filters
        if (minQuantity != null) {
            criteria.add(Criteria.where("quantity").gte(minQuantity));
        }
        if (maxQuantity != null) {
            criteria.add(Criteria.where("quantity").lte(maxQuantity));
        }

        // Date range filters on createdAt field
        if (startDate != null && endDate != null) {
            // Both dates provided - filter between startDate and endDate
            criteria.add(Criteria.where("createdAt").gte(startDate).lte(endDate));
        } else if (startDate != null) {
            // Only start date - filter createdAt >= startDate
            criteria.add(Criteria.where("createdAt").gte(startDate));
        } else if (endDate != null) {
            // Only end date - filter createdAt <= endDate
            criteria.add(Criteria.where("createdAt").lte(endDate));
        }

        // Combine all criteria with AND
        Query query = new Query();
        if (!criteria.isEmpty()) {
            Criteria combinedCriteria = new Criteria().andOperator(criteria.toArray(new Criteria[0]));
            query.addCriteria(combinedCriteria);
        }

        // Apply sorting and pagination
        query.with(pageable);

        // Execute count query for pagination
        long total = mongoTemplate.count(query, Product.class);

        // Execute search query
        List<Product> results = mongoTemplate.find(query, Product.class);

        log.info("Found {} products matching criteria", total);

        return new PageImpl<>(results, pageable, total);
    }
}
