package com.graduation.orderservice.model;

import com.graduation.orderservice.constant.Constant;
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
@Table(name = Constant.TABLE_ORDER_HISTORY, indexes = {
        @Index(name = Constant.INDEX_ORDER_HISTORY_ORDER_ID, columnList = Constant.COLUMN_ORDER_ID),
        @Index(name = Constant.INDEX_ORDER_HISTORY_CHANGED_AT, columnList = Constant.COLUMN_CHANGED_AT),
        @Index(name = Constant.INDEX_ORDER_HISTORY_CHANGED_BY, columnList = Constant.COLUMN_CHANGED_BY)
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

    @NotNull(message = Constant.VALIDATION_ORDER_ID_NULL)
    @Column(name = Constant.COLUMN_ORDER_ID, nullable = false)
    private Long orderId;

    @NotNull(message = Constant.VALIDATION_PREVIOUS_STATUS_NULL)
    @Enumerated(EnumType.STRING)
    @Column(name = Constant.COLUMN_PREVIOUS_STATUS, nullable = false)
    private OrderStatus previousStatus;

    @NotNull(message = Constant.VALIDATION_NEW_STATUS_NULL)
    @Enumerated(EnumType.STRING)
    @Column(name = Constant.COLUMN_NEW_STATUS, nullable = false)
    private OrderStatus newStatus;

    @Column(name = Constant.COLUMN_REASON, length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = Constant.COLUMN_CHANGED_AT, nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @NotBlank(message = Constant.VALIDATION_CHANGED_BY_BLANK)
    @Column(name = Constant.COLUMN_CHANGED_BY, nullable = false)
    private String changedBy;

    // Many-to-one relationship with Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = Constant.COLUMN_ORDER_ID, insertable = false, updatable = false)
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
        String baseDescription = String.format(Constant.CHANGE_DESC_FORMAT,
                previousStatus.name(), newStatus.name());

        if (reason != null && !reason.trim().isEmpty()) {
            baseDescription += String.format(Constant.CHANGE_DESC_WITH_REASON, reason);
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
        return String.format(Constant.FORMAT_ORDER_HISTORY_TOSTRING,
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