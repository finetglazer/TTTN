package com.graduation.paymentservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.graduation.paymentservice.constant.Constant;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a payment transaction in the system
 */
@Slf4j
@Entity
@Table(name = Constant.TABLE_PAYMENT_TRANSACTIONS, indexes = {
        @Index(name = Constant.INDEX_PAYMENT_ORDER_ID, columnList = Constant.FIELD_ORDER_ID),
        @Index(name = Constant.INDEX_PAYMENT_USER_ID, columnList = Constant.FIELD_USER_ID),
        @Index(name = Constant.INDEX_PAYMENT_STATUS, columnList = Constant.FIELD_STATUS),
        @Index(name = Constant.INDEX_PAYMENT_SAGA_ID, columnList = Constant.FIELD_SAGA_ID),
        @Index(name = Constant.INDEX_PAYMENT_PROCESSED_AT, columnList = "processedAt")
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

    @NotBlank(message = Constant.VALIDATION_ORDER_ID_BLANK)
    @Column(name = Constant.COLUMN_ORDER_ID, nullable = false)
    private String orderId;

    @NotBlank(message = Constant.VALIDATION_USER_ID_BLANK)
    @Column(name = Constant.COLUMN_USER_ID, nullable = false)
    private String userId;

    @NotNull(message = Constant.VALIDATION_AMOUNT_NULL)
    @DecimalMin(value = "0.0", inclusive = false, message = Constant.VALIDATION_AMOUNT_MIN)
    @Digits(integer = 10, fraction = 2, message = Constant.VALIDATION_AMOUNT_DIGITS)
    @Column(name = Constant.COLUMN_AMOUNT, nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull(message = Constant.VALIDATION_PAYMENT_STATUS_NULL)
    @Enumerated(EnumType.STRING)
    @Column(name = Constant.COLUMN_STATUS, nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = Constant.COLUMN_AUTH_TOKEN, length = 500)
    private String authToken;

    @Column(name = Constant.COLUMN_MOCK_DECISION_REASON, length = 1000)
    private String mockDecisionReason;

    @CreationTimestamp
    @Column(name = Constant.COLUMN_PROCESSED_AT, nullable = false, updatable = false)
    private LocalDateTime processedAt;

    @Column(name = Constant.COLUMN_SAGA_ID)
    private String sagaId;

    @Column(name = Constant.COLUMN_PAYMENT_METHOD)
    private String paymentMethod;

    @Column(name = Constant.COLUMN_TRANSACTION_REFERENCE, unique = true)
    private String transactionReference;

    @Column(name = Constant.COLUMN_EXTERNAL_TRANSACTION_ID)
    private String externalTransactionId;

    @Column(name = Constant.COLUMN_FAILURE_REASON, length = 500)
    private String failureReason;

    @Column(name = Constant.COLUMN_RETRY_COUNT)
    @Builder.Default
    private Integer retryCount = Constant.DEFAULT_RETRY_COUNT;

    @Column(name = Constant.COLUMN_LAST_RETRY_AT)
    private LocalDateTime lastRetryAt;

    @Column(name = "fencing_token", nullable = false)
    private Long fencingToken = 0L;

    @Column(name = "last_token_update")
    private LocalDateTime lastTokenUpdate;


    public boolean updateStatusWithFencing(PaymentStatus newStatus, String reason, Long fencingToken) {
        if (fencingToken == null) {
            log.warn("Attempted to update payment status without fencing token: orderId={}", this.orderId);
            return false;
        }

        // Only allow updates with equal or newer fencing tokens
        if (fencingToken >= this.fencingToken) {
            // Update status using existing logic
            this.status = newStatus;
            this.mockDecisionReason = reason;
            this.processedAt = LocalDateTime.now();

            // Update fencing token
            this.fencingToken = fencingToken;
            this.lastTokenUpdate = LocalDateTime.now();

            log.info("Payment status updated with fencing token: orderId={}, status={}, token={}",
                    this.orderId, newStatus, fencingToken);
            return true;
        } else {
            log.warn("Payment status update rejected - stale fencing token: orderId={}, incoming={}, current={}",
                    this.orderId, fencingToken, this.fencingToken);
            return false;
        }
    }

    /**
     * PHASE 3: Process payment with fencing token
     * Preserves existing business logic while adding fencing token protection
     */
    public boolean processPaymentWithFencing(Long fencingToken) {
        if (fencingToken == null) {
            log.warn("Attempted to process payment without fencing token: orderId={}", this.orderId);
            return false;
        }

        // Only allow processing with equal or newer fencing tokens
        if (fencingToken >= this.fencingToken) {
            // Use existing processPayment logic
            processPayment();

            // Update fencing token
            this.fencingToken = fencingToken;
            this.lastTokenUpdate = LocalDateTime.now();

            log.info("Payment processed with fencing token: orderId={}, status={}, token={}",
                    this.orderId, this.status, fencingToken);
            return true;
        } else {
            log.warn("Payment processing rejected - stale fencing token: orderId={}, incoming={}, current={}",
                    this.orderId, fencingToken, this.fencingToken);
            return false;
        }
    }

    /**
     * PHASE 3: Check if a fencing token is valid for this payment
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
            throw new IllegalStateException(String.format(Constant.ERROR_PAYMENT_ONLY_PROCESS_PENDING, this.status));
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

        //Add 15s delay to simulate processing time
        try {
            Thread.sleep(Constant.MOCK_PROCESSING_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(Constant.FAILED_TO_PROCESS_PAYMENT, e);
        }
    }

    /**
     * Business method to confirm payment
     */
    public void confirmPayment(String authToken, String externalTransactionId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException(String.format(Constant.ERROR_PAYMENT_ONLY_CONFIRM_PENDING, this.status));
        }

        this.status = PaymentStatus.CONFIRMED;
        this.authToken = authToken;
        this.externalTransactionId = externalTransactionId;
        this.mockDecisionReason = Constant.REASON_PAYMENT_CONFIRMED;
    }

    /**
     * Mock payment processing logic
     */
    private PaymentResult mockPaymentProcessing() {
        // Simulate different payment outcomes based on amount
        String externalId = Constant.PREFIX_EXTERNAL_TRANSACTION + UUID.randomUUID().toString().substring(0, Constant.UUID_SUBSTRING_LENGTH);

        if (amount.compareTo(BigDecimal.valueOf(Constant.HIGH_AMOUNT_THRESHOLD)) > 0) {
            // High amounts might be declined
            if (Math.random() < Constant.DECLINE_PROBABILITY) {
                return new PaymentResult(PaymentStatus.FAILED, null,
                        Constant.REASON_HIGH_AMOUNT_DECLINED, externalId);
            }
        }

        if (Math.random() < Constant.FAILURE_PROBABILITY) {
            return new PaymentResult(PaymentStatus.FAILED, null,
                    Constant.REASON_TECHNICAL_ERROR, externalId);
        }

        // Success case
        String authToken = Constant.PREFIX_AUTH_TOKEN + UUID.randomUUID().toString();
        return new PaymentResult(PaymentStatus.CONFIRMED, authToken,
                Constant.REASON_PAYMENT_PROCESSED, externalId);
    }

    /**
     * Generate unique transaction reference
     */
    private static String generateTransactionReference() {
        return Constant.PREFIX_PAYMENT_MESSAGE + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, Constant.UUID_SUBSTRING_LENGTH);
    }

    @Override
    public String toString() {
        return String.format(Constant.FORMAT_PAYMENT_TRANSACTION_TOSTRING,
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
            this.retryCount = Constant.DEFAULT_RETRY_COUNT;
        }
        if (this.transactionReference == null) {
            this.transactionReference = generateTransactionReference();
        }
    }

    // Inner classes remain the same...
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