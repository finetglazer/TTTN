package com.graduation.orderservice.repository;

import com.graduation.orderservice.model.OrderHistory;
import com.graduation.orderservice.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for OrderHistory entity
 */
@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {

    /**
     * Find history records by order ID
     */
    List<OrderHistory> findByOrderIdOrderByChangedAtDesc(Long orderId);

    /**
     * Find history records by order ID in ascending order
     */
    List<OrderHistory> findByOrderIdOrderByChangedAtAsc(Long orderId);

    /**
     * Find history records by who made the change
     */
    List<OrderHistory> findByChangedBy(String changedBy);

    /**
     * Find history records by new status
     */
    List<OrderHistory> findByNewStatus(OrderStatus newStatus);

    /**
     * Find history records by previous status
     */
    List<OrderHistory> findByPreviousStatus(OrderStatus previousStatus);

    /**
     * Find status changes within a date range
     */
    @Query("SELECT oh FROM OrderHistory oh WHERE oh.changedAt BETWEEN :startDate AND :endDate ORDER BY oh.changedAt DESC")
    List<OrderHistory> findHistoryInDateRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Find cancellation records
     */
    @Query("SELECT oh FROM OrderHistory oh WHERE oh.newStatus = 'CANCELLED' ORDER BY oh.changedAt DESC")
    List<OrderHistory> findCancellationRecords();

    /**
     * Find completion records (delivered orders)
     */
    @Query("SELECT oh FROM OrderHistory oh WHERE oh.newStatus = 'DELIVERED' ORDER BY oh.changedAt DESC")
    List<OrderHistory> findCompletionRecords();

    /**
     * Get latest history record for an order
     */
    @Query("SELECT oh FROM OrderHistory oh WHERE oh.orderId = :orderId ORDER BY oh.changedAt DESC LIMIT 1")
    OrderHistory findLatestByOrderId(@Param("orderId") Long orderId);

    /**
     * Count status changes by order ID
     */
    long countByOrderId(Long orderId);

    /**
     * Count status changes by changed by user
     */
    long countByChangedBy(String changedBy);

    /**
     * Find history records with specific status transition
     */
    @Query("SELECT oh FROM OrderHistory oh WHERE oh.previousStatus = :previousStatus AND oh.newStatus = :newStatus")
    List<OrderHistory> findByStatusTransition(@Param("previousStatus") OrderStatus previousStatus,
                                              @Param("newStatus") OrderStatus newStatus);

    /**
     * Get history statistics by status changes
     */
    @Query("SELECT oh.previousStatus, oh.newStatus, COUNT(oh) FROM OrderHistory oh GROUP BY oh.previousStatus, oh.newStatus")
    List<Object[]> getStatusChangeStatistics();

    /**
     * Find history records for multiple orders
     */
    List<OrderHistory> findByOrderIdInOrderByChangedAtDesc(List<Long> orderIds);

    /**
     * Find recent history records (last N hours)
     */
    @Query("SELECT oh FROM OrderHistory oh WHERE oh.changedAt >= :cutoffTime ORDER BY oh.changedAt DESC")
    List<OrderHistory> findRecentHistory(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Check if order has specific status change
     */
    boolean existsByOrderIdAndPreviousStatusAndNewStatus(Long orderId, OrderStatus previousStatus, OrderStatus newStatus);

    /**
     * Find first status change for an order
     */
    @Query("SELECT oh FROM OrderHistory oh WHERE oh.orderId = :orderId ORDER BY oh.changedAt ASC LIMIT 1")
    OrderHistory findFirstByOrderId(@Param("orderId") Long orderId);

    /**
     * Get average time between status changes
     */
    @Query(value = """
        SELECT AVG(
            EXTRACT(EPOCH FROM (oh2.changed_at - oh1.changed_at))
        ) / 3600 as avg_hours
        FROM order_history oh1
        JOIN order_history oh2 ON oh1.order_id = oh2.order_id
        WHERE oh1.new_status = :fromStatus 
        AND oh2.previous_status = :fromStatus 
        AND oh2.new_status = :toStatus
        AND oh2.changed_at > oh1.changed_at
        """, nativeQuery = true)
    Double getAverageTimeBetweenStatuses(@Param("fromStatus") String fromStatus,
                                         @Param("toStatus") String toStatus);

    /**
     * Delete old history records (for cleanup)
     */
    void deleteByChangedAtBefore(LocalDateTime cutoffDate);
}