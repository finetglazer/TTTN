package com.graduation.orderservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PHASE 3: Result of acquiring a distributed lock with fencing token
 * Contains both the lock acquisition status and the fencing token for preventing split-brain scenarios
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FencingLockResult {

    /**
     * Whether the lock was successfully acquired
     */
    private boolean acquired;

    /**
     * The fencing token - a unique, monotonically increasing number
     * This token must be passed to all resource operations to prevent stale operations
     */
    private String fencingToken;

    /**
     * The lock key that was acquired
     */
    private String lockKey;

    /**
     * Additional metadata about the lock acquisition
     */
    private String lockValue;

    /**
     * Timestamp when the lock was acquired
     */
    private long acquiredAt;

    /**
     * Factory method for successful lock acquisition
     */
    public static FencingLockResult success(String lockKey, String fencingToken, String lockValue) {
        return new FencingLockResult(
                true,
                fencingToken,
                lockKey,
                lockValue,
                System.currentTimeMillis()
        );
    }

    /**
     * Factory method for failed lock acquisition
     */
    public static FencingLockResult failure(String lockKey) {
        return new FencingLockResult(
                false,
                null,
                lockKey,
                null,
                System.currentTimeMillis()
        );
    }

    /**
     * Check if the lock acquisition was successful and we have a valid fencing token
     */
    public boolean isValid() {
        return acquired && fencingToken != null && !fencingToken.trim().isEmpty();
    }

    /**
     * Get fencing token as Long for database operations
     */
    public Long getFencingTokenAsLong() {
        if (fencingToken == null) {
            return null;
        }
        try {
            return Long.parseLong(fencingToken);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}