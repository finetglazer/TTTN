package com.graduation.orderservice.repository;

import com.graduation.orderservice.model.Order;
import com.graduation.orderservice.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order entity
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderHistories WHERE o.id = :id")
    Optional<Order> findByIdWithHistories(@Param("id") Long id);

    /**
     * Find orders by user ID
     */
    List<Order> findByUserId(String userId);

    /**
     * Find orders by user ID and status
     */
    List<Order> findByUserIdAndStatus(String userId, OrderStatus status);

    /**
     * Find order by saga ID
     */
    Optional<Order> findBySagaId(String sagaId);

    /**
     * Find orders by status
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Find orders by user email
     */
    List<Order> findByUserEmail(String userEmail);

    /**
     * Find orders created within a date range
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Find orders by status and created date range
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByStatusAndCreatedAtBetween(@Param("status") OrderStatus status,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Find orders with total amount greater than specified value
     */
    List<Order> findByTotalAmountGreaterThan(BigDecimal amount);

    /**
     * Find orders with total amount between range
     */
    List<Order> findByTotalAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);

    /**
     * Count orders by status
     */
    long countByStatus(OrderStatus status);

    /**
     * Count orders by user ID
     */
    long countByUserId(String userId);

    /**
     * Find pending orders (orders that are not in final state)
     */
    @Query("SELECT o FROM Order o WHERE o.status NOT IN ('DELIVERED', 'CANCELLED')")
    List<Order> findPendingOrders();

    /**
     * Find orders that need attention (created more than specified hours ago but not processed)
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'CREATED' AND o.createdAt < :cutoffTime")
    List<Order> findOrdersNeedingAttention(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find orders by user and date range
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findUserOrdersInDateRange(@Param("userId") String userId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Get total amount by user ID
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.userId = :userId AND o.status = 'CONFIRMED'")
    BigDecimal getTotalAmountByUser(@Param("userId") String userId);

    /**
     * Get order statistics by status
     */
    @Query("SELECT o.status, COUNT(o), SUM(o.totalAmount) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderStatisticsByStatus();

    /**
     * Find orders by multiple statuses
     */
    List<Order> findByStatusIn(List<OrderStatus> statuses);

    /**
     * Check if user has any orders
     */
    boolean existsByUserId(String userId);

    /**
     * Find latest orders for a user
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findLatestOrdersByUser(@Param("userId") String userId, org.springframework.data.domain.Pageable pageable);

    /**
     * Delete orders older than specified date (for cleanup)
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}