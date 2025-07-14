package com.graduation.orderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing the history of order status changes
 */
@Entity
@Table(name = "order_history", indexes = {
        @Index(name = "idx_order_history_order_id", columnList = "orderId"),
        @Index(name = "idx_order_history_changed_at", columnList = "changedAt"),
        @Index(name = "idx_order_history_changed_by", columnList = "changedBy")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Order ID cannot be null")
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @NotNull(message = "Previous status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    private OrderStatus previousStatus;

    @NotNull(message = "New status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private OrderStatus newStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @NotBlank(message = "Changed by cannot be blank")
    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    // Many-to-one relationship with Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    /**
     * Factory method to create an order history record
     */
    public static OrderHistory createStatusChangeRecord(Long orderId, OrderStatus previousStatus,
                                                        OrderStatus newStatus, String reason, String changedBy) {
        return OrderHistory.builder()
                .orderId(orderId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(changedBy)
                .build();
    }

    /**
     * Factory method to create an order history record from Order entity
     */
    public static OrderHistory createFromOrder(Order order, OrderStatus newStatus, String reason, String changedBy) {
        return OrderHistory.builder()
                .order(order)
                .orderId(order.getId())
                .previousStatus(order.getStatus())
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(changedBy)
                .build();
    }

    /**
     * Get a description of the status change
     */
    public String getChangeDescription() {
        String baseDescription = String.format("Status changed from %s to %s",
                previousStatus.name(), newStatus.name());

        if (reason != null && !reason.trim().isEmpty()) {
            baseDescription += " - " + reason;
        }

        return baseDescription;
    }

    /**
     * Check if this represents a status upgrade (positive change)
     */
    public boolean isStatusUpgrade() {
        return switch (previousStatus) {
            case CREATED -> newStatus == OrderStatus.CONFIRMED;
            case CONFIRMED -> newStatus == OrderStatus.DELIVERED;
            default -> false;
        };
    }

    /**
     * Check if this represents a cancellation
     */
    public boolean isCancellation() {
        return newStatus == OrderStatus.CANCELLED;
    }

    /**
     * Get time since the change occurred
     */
    public long getHoursSinceChange() {
        return java.time.Duration.between(changedAt, LocalDateTime.now()).toHours();
    }

    @Override
    public String toString() {
        return String.format("OrderHistory{id=%d, orderId=%d, %s->%s, changedBy='%s', changedAt=%s}",
                id, orderId, previousStatus, newStatus, changedBy, changedAt);
    }

    /**
     * DTO for external communication
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderHistoryDetails {
        private Long id;
        private Long orderId;
        private OrderStatus previousStatus;
        private OrderStatus newStatus;
        private String reason;
        private LocalDateTime changedAt;
        private String changedBy;
        private String changeDescription;
        private boolean isStatusUpgrade;
        private boolean isCancellation;

        public static OrderHistoryDetails fromEntity(OrderHistory history) {
            return OrderHistoryDetails.builder()
                    .id(history.getId())
                    .orderId(history.getOrderId())
                    .previousStatus(history.getPreviousStatus())
                    .newStatus(history.getNewStatus())
                    .reason(history.getReason())
                    .changedAt(history.getChangedAt())
                    .changedBy(history.getChangedBy())
                    .changeDescription(history.getChangeDescription())
                    .isStatusUpgrade(history.isStatusUpgrade())
                    .isCancellation(history.isCancellation())
                    .build();
        }
    }
}