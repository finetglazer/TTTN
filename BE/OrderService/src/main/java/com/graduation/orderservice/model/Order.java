package com.graduation.orderservice.model;

import com.graduation.orderservice.constant.Constant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an order in the system
 */
@Slf4j
@Entity
@Table(name = Constant.TABLE_ORDERS, indexes = {
        @Index(name = Constant.INDEX_ORDER_USER_ID, columnList = Constant.COLUMN_USER_ID),
        @Index(name = Constant.INDEX_ORDER_STATUS, columnList = Constant.COLUMN_STATUS),
        @Index(name = Constant.INDEX_ORDER_SAGA_ID, columnList = Constant.COLUMN_SAGA_ID),
        @Index(name = Constant.INDEX_ORDER_CREATED_AT, columnList = Constant.COLUMN_CREATED_AT)
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

    @NotBlank(message = Constant.VALIDATION_USER_ID_BLANK)
    @Column(name = Constant.COLUMN_USER_ID, nullable = false)
    private String userId;

    @NotBlank(message = Constant.VALIDATION_USER_EMAIL_BLANK)
    @Email(message = Constant.VALIDATION_INVALID_EMAIL)
    @Column(name = Constant.COLUMN_USER_EMAIL, nullable = false)
    private String userEmail;

    @NotBlank(message = Constant.VALIDATION_USER_NAME_BLANK)
    @Column(name = Constant.COLUMN_USER_NAME, nullable = false)
    private String userName;

    @NotBlank(message = Constant.VALIDATION_ORDER_DESCRIPTION_BLANK)
    @Size(max = 1000, message = Constant.VALIDATION_ORDER_DESCRIPTION_SIZE)
    @Column(name = Constant.COLUMN_ORDER_DESCRIPTION, nullable = false, length = 1000)
    private String orderDescription;

    @NotNull(message = Constant.VALIDATION_TOTAL_AMOUNT_NULL)
    @DecimalMin(value = "0.0", inclusive = false, message = Constant.VALIDATION_TOTAL_AMOUNT_MIN)
    @Digits(integer = 10, fraction = 2, message = Constant.VALIDATION_TOTAL_AMOUNT_DIGITS)
    @Column(name = Constant.COLUMN_TOTAL_AMOUNT, nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @NotNull(message = Constant.VALIDATION_ORDER_STATUS_NULL)
    @Enumerated(EnumType.STRING)
    @Column(name = Constant.COLUMN_STATUS, nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.CREATED;

    @CreationTimestamp
    @Column(name = Constant.COLUMN_CREATED_AT, nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = Constant.COLUMN_UPDATED_AT)
    private LocalDateTime updatedAt;

    @Column(name = Constant.COLUMN_SAGA_ID)
    private String sagaId;

    @Column(name = Constant.COLUMN_SHIPPING_ADDRESS)
    private String shippingAddress;

    // Relationship with OrderHistory
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderHistory> orderHistories = new ArrayList<>();

    @Column(name = "fencing_token", nullable = false)
    private Long fencingToken = 0L;

    @Column(name = "last_token_update")
    private LocalDateTime lastTokenUpdate;
    /**
     * Business method to create a new order
     */

    /**
     * PHASE 3: Update order status with fencing token validation
     * Only allows updates from operations with equal or newer fencing tokens
     */
    public boolean updateStatusWithFencing(OrderStatus newStatus, String reason, String changedBy, Long fencingToken) {
        if (fencingToken == null) {
            log.warn("Attempted to update order status without fencing token: orderId={}", this.id);
            return false;
        }

        // Only allow updates with equal or newer fencing tokens
        if (fencingToken >= this.fencingToken) {
            // Use existing updateStatus logic but with fencing protection
            OrderStatus previousStatus = this.status;

            // Update status using existing business logic
            this.status = newStatus;
            this.updatedAt = LocalDateTime.now();

            // Add to history using existing logic
            OrderHistory historyEntry = OrderHistory.builder()
                    .order(this)
                    .previousStatus(previousStatus)
                    .newStatus(newStatus)
                    .reason(reason)
                    .changedBy(changedBy)
                    .changedAt(LocalDateTime.now())
                    .build();

            if (this.orderHistories == null) {
                this.orderHistories = new ArrayList<>();
            }
            this.orderHistories.add(historyEntry);

            // Update fencing token
            this.fencingToken = fencingToken;
            this.lastTokenUpdate = LocalDateTime.now();

            log.info("Order status updated with fencing token: orderId={}, status={}, token={}",
                    this.id, newStatus, fencingToken);
            return true;
        } else {
            log.warn("Order status update rejected - stale fencing token: orderId={}, incoming={}, current={}",
                    this.id, fencingToken, this.fencingToken);
            return false;
        }
    }

    /**
     * PHASE 3: Cancel order with fencing token validation
     * Preserves existing cancellation logic while adding fencing token protection
     */
    public boolean cancelWithFencing(String reason, String changedBy, Long fencingToken) {
        if (fencingToken == null) {
            log.warn("Attempted to cancel order without fencing token: orderId={}", this.id);
            return false;
        }

        // Only allow cancellation with equal or newer fencing tokens
        if (fencingToken >= this.fencingToken) {
            // Can cancel from CREATED or CONFIRMED status
            boolean canCancel = this.status == OrderStatus.CREATED || this.status == OrderStatus.CONFIRMED;

            if (canCancel) {
                // CORRECTED: First transition to CANCELLATION_PENDING
                updateStatusWithFencing(OrderStatus.CANCELLATION_PENDING, reason, changedBy, fencingToken);
                log.info("Order cancellation initiated with fencing token: orderId={}, token={}", this.id, fencingToken);
                return true;
            } else {
                log.warn("Order cannot be cancelled in current status: orderId={}, status={}", this.id, this.status);
                return false;
            }
        } else {
            log.warn("Order cancellation rejected - stale fencing token: orderId={}, incoming={}, current={}",
                    this.id, fencingToken, this.fencingToken);
            return false;
        }
    }

    /**
     * PHASE 3: Confirm order with fencing token validation
     */
    public boolean confirmWithFencing(String reason, String changedBy, Long fencingToken) {
        if (fencingToken == null) {
            log.warn("Attempted to confirm order without fencing token: orderId={}", this.id);
            return false;
        }

        // Only allow confirmation with equal or newer fencing tokens
        if (fencingToken >= this.fencingToken && this.status == OrderStatus.CREATED) {
            updateStatusWithFencing(OrderStatus.CONFIRMED, reason, changedBy, fencingToken);
            log.info("Order confirmed with fencing token: orderId={}, token={}", this.id, fencingToken);
            return true;
        } else if (fencingToken < this.fencingToken) {
            log.warn("Order confirmation rejected - stale fencing token: orderId={}, incoming={}, current={}",
                    this.id, fencingToken, this.fencingToken);
            return false;
        } else {
            log.warn("Order cannot be confirmed in current status: orderId={}, status={}", this.id, this.status);
            return false;
        }
    }

    /**
     * PHASE 3: Deliver order with fencing token validation
     */
    public boolean deliverWithFencing(String reason, String changedBy, Long fencingToken) {
        if (fencingToken == null) {
            log.warn("Attempted to deliver order without fencing token: orderId={}", this.id);
            return false;
        }

        // Only allow delivery with equal or newer fencing tokens
        if (fencingToken >= this.fencingToken && this.status == OrderStatus.CONFIRMED) {
            updateStatusWithFencing(OrderStatus.DELIVERED, reason, changedBy, fencingToken);
            log.info("Order delivered with fencing token: orderId={}, token={}", this.id, fencingToken);
            return true;
        } else if (fencingToken < this.fencingToken) {
            log.warn("Order delivery rejected - stale fencing token: orderId={}, incoming={}, current={}",
                    this.id, fencingToken, this.fencingToken);
            return false;
        } else {
            log.warn("Order cannot be delivered in current status: orderId={}, status={}", this.id, this.status);
            return false;
        }
    }

    /**
     * PHASE 3: Check if a fencing token is valid for this order
     */
    public boolean isValidFencingToken(Long incomingToken) {
        return incomingToken != null && incomingToken >= this.fencingToken;
    }

    /**
     * PHASE 3: Get fencing token info for logging and debugging
     */
    public String getFencingTokenInfo() {
        return String.format("token=%d, lastUpdate=%s", fencingToken, lastTokenUpdate);
    }




    public static Order createOrder(String userId, String userEmail, String userName,
                                    String orderDescription, BigDecimal totalAmount, String shippingAddress) {
        return Order.builder()
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .orderDescription(orderDescription)
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .shippingAddress(shippingAddress)
                .build();
    }

    /**
     * Business method to update order status with history tracking
     */
    public void updateStatus(OrderStatus newStatus, String reason, String changedBy) {
        if (this.status == newStatus) {
            return; // No change needed
        }

        // Check current = CREATED, newStatus must be CONFIRMED or CANCELLED
        if (this.status.equals(OrderStatus.CREATED) && !newStatus.equals(OrderStatus.CONFIRMED) && !newStatus.equals(OrderStatus.CANCELLED)) {
            throw new IllegalArgumentException(String.format(Constant.ERROR_INVALID_STATUS_TRANSITION, this.status, newStatus));
        }

        // Check current = CONFIRMED, newStatus must be DELIVERED or CANCELLED
        if (this.status.equals(OrderStatus.CONFIRMED) && !newStatus.equals(OrderStatus.DELIVERED) && !newStatus.equals(OrderStatus.CANCELLED)) {
            throw new IllegalArgumentException(String.format(Constant.ERROR_INVALID_STATUS_TRANSITION, this.status, newStatus));
        }

        // Validate status transition
        if (this.status.isFinalState()) {
            throw new IllegalStateException(String.format(Constant.ERROR_CANNOT_CHANGE_FINAL_STATUS, this.status));
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
        if (this.status.canBeCancelled()) {
            throw new IllegalStateException(String.format(Constant.ERROR_CANNOT_CANCEL_STATUS, this.status));
        }

        updateStatus(OrderStatus.CANCELLED, reason, cancelledBy);
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
        return String.format(Constant.FORMAT_ORDER_TOSTRING,
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