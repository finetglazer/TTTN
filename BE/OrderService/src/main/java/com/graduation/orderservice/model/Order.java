package com.graduation.orderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an order in the system
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_user_id", columnList = "userId"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_saga_id", columnList = "sagaId"),
        @Index(name = "idx_order_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "User ID cannot be blank")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotBlank(message = "User email cannot be blank")
    @Email(message = "Invalid email format")
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @NotBlank(message = "User name cannot be blank")
    @Column(name = "user_name", nullable = false)
    private String userName;

    @NotBlank(message = "Order description cannot be blank")
    @Size(max = 1000, message = "Order description cannot exceed 1000 characters")
    @Column(name = "order_description", nullable = false, length = 1000)
    private String orderDescription;

    @NotNull(message = "Total amount cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Total amount must have at most 10 integer digits and 2 decimal places")
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @NotNull(message = "Order status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "saga_id")
    private String sagaId;

    // Relationship with OrderHistory
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderHistory> orderHistories = new ArrayList<>();

    /**
     * Business method to create a new order
     */
    public static Order createOrder(String userId, String userEmail, String userName,
                                    String orderDescription, BigDecimal totalAmount) {
        return Order.builder()
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .orderDescription(orderDescription)
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .build();
    }

    /**
     * Business method to update order status with history tracking
     */
    public void updateStatus(OrderStatus newStatus, String reason, String changedBy) {
        if (this.status == newStatus) {
            return; // No change needed
        }

        // Validate status transition
        if (this.status.isFinalState()) {
            throw new IllegalStateException("Cannot change status from final state: " + this.status);
        }

        // Create history record
        OrderHistory history = OrderHistory.builder()
                .order(this)
                .orderId(this.id)
                .previousStatus(this.status)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(changedBy)
                .build();

        this.orderHistories.add(history);
        this.status = newStatus;
    }

    /**
     * Business method to cancel the order
     */
    public void cancel(String reason, String cancelledBy) {
        if (!this.status.canBeCancelled()) {
            throw new IllegalStateException("Order cannot be cancelled in current status: " + this.status);
        }

        updateStatus(OrderStatus.CANCELLED, reason, cancelledBy);
    }

    /**
     * Business method to confirm the order
     */
    public void confirm(String confirmedBy) {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("Order can only be confirmed from CREATED status, current: " + this.status);
        }

        updateStatus(OrderStatus.CONFIRMED, "Order confirmed for processing", confirmedBy);
    }

    /**
     * Business method to mark order as delivered
     */
    public void markAsDelivered(String deliveredBy) {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order can only be delivered from CONFIRMED status, current: " + this.status);
        }

        updateStatus(OrderStatus.DELIVERED, "Order successfully delivered", deliveredBy);
    }

    /**
     * Get order details for external communication
     */
    public OrderDetails getOrderDetails() {
        return OrderDetails.builder()
                .orderId(this.id)
                .userId(this.userId)
                .userEmail(this.userEmail)
                .userName(this.userName)
                .orderDescription(this.orderDescription)
                .totalAmount(this.totalAmount)
                .status(this.status)
                .createdAt(this.createdAt)
                .sagaId(this.sagaId)
                .build();
    }

    /**
     * Check if order can be modified
     */
    public boolean canBeModified() {
        return this.status == OrderStatus.CREATED;
    }

    /**
     * Pre-persist callback
     */
    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = OrderStatus.CREATED;
        }
    }

    @Override
    public String toString() {
        return String.format("Order{id=%d, userId='%s', status=%s, totalAmount=%s, sagaId='%s'}",
                id, userId, status, totalAmount, sagaId);
    }

    /**
     * DTO class for external communication
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetails {
        private Long orderId;
        private String userId;
        private String userEmail;
        private String userName;
        private String orderDescription;
        private BigDecimal totalAmount;
        private OrderStatus status;
        private LocalDateTime createdAt;
        private String sagaId;
    }
}