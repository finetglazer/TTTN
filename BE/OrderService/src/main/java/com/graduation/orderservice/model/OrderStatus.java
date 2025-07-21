package com.graduation.orderservice.model;

import com.graduation.orderservice.constant.Constant;
import lombok.Getter;

/**
 * Enumeration representing the various states of an order
 */
@Getter
public enum OrderStatus {
    /**
     * Order has been created but not yet confirmed
     */
    CREATED(Constant.STATUS_DESC_CREATED),

    /**
     * Order has been confirmed and is being processed
     */
    CONFIRMED(Constant.STATUS_DESC_CONFIRMED),

    /**
     * Order has been delivered to the customer
     */
    DELIVERED(Constant.STATUS_DESC_DELIVERED),

    /**
     * Order has been cancelled
     */
    CANCELLED(Constant.STATUS_DESC_CANCELLED);

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    /**
     * Check if the status allows for cancellation
     */
    public boolean canBeCancelled() {
        return this == CREATED || this == CONFIRMED;
    }

    /**
     * Check if the status is a final state
     */
    public boolean isFinalState() {
        return this == DELIVERED || this == CANCELLED;
    }

    /**
     * Get the next possible statuses from current status
     */
    public OrderStatus[] getPossibleNextStatuses() {
        return switch (this) {
            case CREATED -> new OrderStatus[]{CONFIRMED, CANCELLED};
            case CONFIRMED -> new OrderStatus[]{DELIVERED, CANCELLED};
            case DELIVERED, CANCELLED -> new OrderStatus[]{}; // Final states
        };
    }
}