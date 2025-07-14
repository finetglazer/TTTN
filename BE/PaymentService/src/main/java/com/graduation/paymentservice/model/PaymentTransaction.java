package com.graduation.paymentservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a payment transaction in the system
 */
@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_order_id", columnList = "orderId"),
        @Index(name = "idx_payment_user_id", columnList = "userId"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_saga_id", columnList = "sagaId"),
        @Index(name = "idx_payment_processed_at", columnList = "processedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Order ID cannot be blank")
    @Column(name = "order_id", nullable = false)
    private String orderId;

    @NotBlank(message = "User ID cannot be blank")
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer digits and 2 decimal places")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Payment status cannot be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "auth_token", length = 500)
    private String authToken;

    @Column(name = "mock_decision_reason", length = 1000)
    private String mockDecisionReason;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    @Column(name = "saga_id")
    private String sagaId;

    // Additional fields for payment processing
    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "transaction_reference", unique = true)
    private String transactionReference;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    /**
     * Factory method to create a new payment transaction
     */
    public static PaymentTransaction createPaymentTransaction(String orderId, String userId,
                                                              BigDecimal amount, String paymentMethod) {
        return PaymentTransaction.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING)
                .transactionReference(generateTransactionReference())
                .build();
    }

    /**
     * Factory method to create payment transaction for saga
     */
    public static PaymentTransaction createForSaga(String orderId, String userId, BigDecimal amount,
                                                   String sagaId, String paymentMethod) {
        return PaymentTransaction.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .sagaId(sagaId)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING)
                .transactionReference(generateTransactionReference())
                .build();
    }

    /**
     * Business method to process payment
     */
    public void processPayment() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment can only be processed from PENDING status, current: " + this.status);
        }

        // Simulate payment processing (mock implementation)
        PaymentResult result = mockPaymentProcessing();

        this.status = result.status;
        this.authToken = result.authToken;
        this.mockDecisionReason = result.reason;
        this.externalTransactionId = result.externalTransactionId;

        if (result.status.isFailure()) {
            this.failureReason = result.reason;
        }
    }

    /**
     * Business method to confirm payment
     */
    public void confirmPayment(String authToken, String externalTransactionId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment can only be confirmed from PENDING status, current: " + this.status);
        }

        this.status = PaymentStatus.CONFIRMED;
        this.authToken = authToken;
        this.externalTransactionId = externalTransactionId;
        this.mockDecisionReason = "Payment confirmed successfully";
    }

    /**
     * Business method to decline payment
     */
    public void declinePayment(String reason) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment can only be declined from PENDING status, current: " + this.status);
        }

        this.status = PaymentStatus.DECLINED;
        this.failureReason = reason;
        this.mockDecisionReason = reason;
    }

    /**
     * Business method to mark payment as failed
     */
    public void markAsFailed(String reason) {
        if (this.status.isFinalStatus() && this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Cannot mark as failed from final status: " + this.status);
        }

        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.mockDecisionReason = reason;
    }

    /**
     * Business method to retry payment
     */
    public void retryPayment() {
        if (!this.status.allowsRetry()) {
            throw new IllegalStateException("Payment retry not allowed for status: " + this.status);
        }

        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
        this.failureReason = null;
        this.mockDecisionReason = "Payment retry attempt #" + this.retryCount;
    }

    /**
     * Get payment status for external communication
     */
    public PaymentDetails getPaymentDetails() {
        return PaymentDetails.builder()
                .transactionId(this.id)
                .orderId(this.orderId)
                .userId(this.userId)
                .amount(this.amount)
                .status(this.status)
                .paymentMethod(this.paymentMethod)
                .transactionReference(this.transactionReference)
                .processedAt(this.processedAt)
                .sagaId(this.sagaId)
                .retryCount(this.retryCount)
                .build();
    }

    /**
     * Check if payment can be retried
     */
    public boolean canBeRetried() {
        return this.status.allowsRetry() && this.retryCount < 3; // Max 3 retries
    }

    /**
     * Mock payment processing logic
     */
    private PaymentResult mockPaymentProcessing() {
        // Simulate different payment outcomes based on amount
        String externalId = "EXT_" + UUID.randomUUID().toString().substring(0, 8);

        if (amount.compareTo(BigDecimal.valueOf(1000)) > 0) {
            // High amounts might be declined
            if (Math.random() < 0.3) { // 30% chance of decline
                return new PaymentResult(PaymentStatus.DECLINED, null,
                        "High amount transaction declined", externalId);
            }
        }

        if (Math.random() < 0.1) { // 10% chance of technical failure
            return new PaymentResult(PaymentStatus.FAILED, null,
                    "Technical error during processing", externalId);
        }

        // Success case
        String authToken = "AUTH_" + UUID.randomUUID().toString();
        return new PaymentResult(PaymentStatus.CONFIRMED, authToken,
                "Payment processed successfully", externalId);
    }

    /**
     * Generate unique transaction reference
     */
    private static String generateTransactionReference() {
        return "PAY_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public String toString() {
        return String.format("PaymentTransaction{id=%d, orderId='%s', userId='%s', amount=%s, status=%s, sagaId='%s'}",
                id, orderId, userId, amount, status, sagaId);
    }

    /**
     * Pre-persist callback
     */
    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        if (this.transactionReference == null) {
            this.transactionReference = generateTransactionReference();
        }
    }

    /**
     * Inner class for payment processing result
     */
    private static class PaymentResult {
        final PaymentStatus status;
        final String authToken;
        final String reason;
        final String externalTransactionId;

        PaymentResult(PaymentStatus status, String authToken, String reason, String externalTransactionId) {
            this.status = status;
            this.authToken = authToken;
            this.reason = reason;
            this.externalTransactionId = externalTransactionId;
        }
    }

    /**
     * DTO class for external communication
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetails {
        private Long transactionId;
        private String orderId;
        private String userId;
        private BigDecimal amount;
        private PaymentStatus status;
        private String paymentMethod;
        private String transactionReference;
        private LocalDateTime processedAt;
        private String sagaId;
        private Integer retryCount;
    }
}