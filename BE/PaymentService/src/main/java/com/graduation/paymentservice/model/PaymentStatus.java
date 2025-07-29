package com.graduation.paymentservice.model;

import lombok.Getter;

/**
 * Enumeration representing the various states of a payment transaction
 */
@Getter
public enum PaymentStatus {
    /**
     * Payment is pending processing
     */
    PENDING("Payment is pending processing"),

    /**
     * Payment has been confirmed and processed successfully
     */
    CONFIRMED("Payment has been confirmed"),

    /**
     * Payment has been reversed, typically due to a refund or chargeback
     */
    REVERSED("Payment has been reversed"),

    /**
     * Payment processing failed due to technical issues
     */
    FAILED("Payment processing failed");



    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    /**
     * Check if the payment status is successful
     */
    public boolean isSuccessful() {
        return this == CONFIRMED;
    }

    /**
     * Check if the payment status is a failure
     */
    public boolean isFailure() {
    return this == REVERSED || this == FAILED;
    }

    /**
     * Check if the payment is still in progress
     */
    public boolean isInProgress() {
        return this == PENDING;
    }

    /**
     * Check if the payment status is final (no further processing)
     */
    public boolean isFinalStatus() {
        return this == CONFIRMED || this == FAILED || this == REVERSED;
    }

    /**
     * Get the next possible statuses from current status
     */
    public PaymentStatus[] getPossibleNextStatuses() {
        return switch (this) {
            case PENDING -> new PaymentStatus[]{CONFIRMED, FAILED}; // Pending can lead to any final state
            case FAILED -> new PaymentStatus[]{REVERSED}; // Failed can be retried
            case CONFIRMED, REVERSED -> new PaymentStatus[]{}; // Final states
        };
    }

    /**
     * Check if this status allows retry
     */
    public boolean allowsRetry() {
        return this == FAILED; // Only technical failures can be retried
    }

    /**
     * Get status code for external APIs
     */
    public int getStatusCode() {
        return switch (this) {
            case PENDING -> 102; // Processing
            case CONFIRMED -> 200; // OK
            case REVERSED -> 402; // Payment
            case FAILED -> 500; // Internal Server Error
        };
    }
}