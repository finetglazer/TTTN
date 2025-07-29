package com.graduation.sagaorchestratorservice.service;

import com.graduation.sagaorchestratorservice.model.FencingLockResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * PHASE 3: Redis-based distributed lock service with FENCING TOKEN support
 * Enhanced to provide split-brain protection through monotonically increasing tokens
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService {

    private final RedisTemplate<String, String> redisTemplate;
    private final String serviceInstanceId = UUID.randomUUID().toString().substring(0, 8);

    // PHASE 3: Lua script for atomic lock acquisition WITH fencing token generation
    private static final String ACQUIRE_LOCK_WITH_FENCING_SCRIPT =
            "local lockKey = KEYS[1]\n" +
                    "local tokenKey = KEYS[2]\n" +
                    "local lockValue = ARGV[1]\n" +
                    "local ttlSeconds = ARGV[2]\n" +
                    "\n" +
                    "-- Try to acquire the lock\n" +
                    "local acquired = redis.call('SET', lockKey, lockValue, 'NX', 'EX', ttlSeconds)\n" +
                    "if acquired then\n" +
                    "    -- Generate fencing token (atomic increment)\n" +
                    "    local fencingToken = redis.call('INCR', tokenKey)\n" +
                    "    -- Set expiration for token key (longer than lock to prevent reuse)\n" +
                    "    redis.call('EXPIRE', tokenKey, ttlSeconds * 2)\n" +
                    "    return {1, fencingToken}\n" +
                    "else\n" +
                    "    return {0, nil}\n" +
                    "end";

    // Existing Lua scripts
    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('DEL', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";

    private static final String EXTEND_LOCK_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
                    "else " +
                    "    return 0 " +
                    "end";

    // PHASE 3: Lua script for validating fencing token and performing operation
    private static final String VALIDATE_TOKEN_AND_EXECUTE_SCRIPT =
            "local resourceKey = KEYS[1]\n" +
                    "local incomingToken = tonumber(ARGV[1])\n" +
                    "local operation = ARGV[2]\n" +
                    "\n" +
                    "-- Get current token for this resource\n" +
                    "local currentToken = redis.call('GET', resourceKey)\n" +
                    "if currentToken == false then\n" +
                    "    currentToken = 0\n" +
                    "else\n" +
                    "    currentToken = tonumber(currentToken)\n" +
                    "end\n" +
                    "\n" +
                    "-- Only allow operation if incoming token is newer or equal\n" +
                    "if incomingToken >= currentToken then\n" +
                    "    redis.call('SET', resourceKey, incomingToken)\n" +
                    "    return {1, incomingToken, currentToken}\n" +
                    "else\n" +
                    "    return {0, incomingToken, currentToken}\n" +
                    "end";

    /**
     * PHASE 3: Acquire a distributed lock WITH fencing token
     * This prevents split-brain scenarios by providing a unique, increasing token
     */
    public FencingLockResult acquireLockWithFencing(String lockKey, long ttl, TimeUnit timeUnit) {
        try {
            String lockValue = serviceInstanceId + ":" + Instant.now().toEpochMilli();
            String tokenKey = buildFencingTokenKey(lockKey);
            long ttlSeconds = timeUnit.toSeconds(ttl);

            log.debug("Attempting to acquire lock with fencing: lockKey={}, tokenKey={}", lockKey, tokenKey);

            DefaultRedisScript<java.util.List> script = new DefaultRedisScript<>(ACQUIRE_LOCK_WITH_FENCING_SCRIPT, java.util.List.class);
            java.util.List<Object> result = redisTemplate.execute(script,
                    Arrays.asList(lockKey, tokenKey),
                    lockValue,
                    String.valueOf(ttlSeconds));

            if (result != null && result.size() == 2) {
                int acquired = ((Number) result.get(0)).intValue();
                if (acquired == 1) {
                    String fencingToken = result.get(1).toString();

                    log.info("Lock acquired with fencing token: lockKey={}, token={}, ttl={}s",
                            lockKey, fencingToken, ttlSeconds);

                    return FencingLockResult.success(lockKey, fencingToken, lockValue);
                }
            }

            log.warn("Failed to acquire lock with fencing: lockKey={}", lockKey);
            return FencingLockResult.failure(lockKey);

        } catch (Exception e) {
            log.error("Error acquiring lock with fencing: lockKey={}", lockKey, e);
            return FencingLockResult.failure(lockKey);
        }
    }

    /**
     * PHASE 3: Try to acquire lock with fencing (non-blocking)
     */
    public FencingLockResult tryLockWithFencing(String lockKey, long ttl, TimeUnit timeUnit) {
        log.debug("Attempting to acquire lock with fencing atomically: key={}", lockKey);
        return acquireLockWithFencing(lockKey, ttl, timeUnit);
    }

    /**
     * PHASE 3: Validate fencing token for a resource operation
     * Returns true if the token is valid (equal or newer than current)
     */
    public boolean validateFencingToken(String resourceKey, String fencingToken) {
        if (fencingToken == null) {
            log.warn("Fencing token is null for resource: {}", resourceKey);
            return false;
        }

        try {
            String tokenValidationKey = buildResourceTokenKey(resourceKey);

            DefaultRedisScript<java.util.List> script = new DefaultRedisScript<>(VALIDATE_TOKEN_AND_EXECUTE_SCRIPT, java.util.List.class);
            java.util.List<Object> result = redisTemplate.execute(script,
                    Collections.singletonList(tokenValidationKey),
                    fencingToken,
                    "validate");

            if (result != null && result.size() == 3) {
                int valid = ((Number) result.get(0)).intValue();
                String incomingToken = result.get(1).toString();
                String currentToken = result.get(2).toString();

                if (valid == 1) {
                    log.debug("Fencing token validated successfully: resource={}, token={}, previous={}",
                            resourceKey, incomingToken, currentToken);
                    return true;
                } else {
                    log.warn("Fencing token rejected - stale operation detected: resource={}, incoming={}, current={}",
                            resourceKey, incomingToken, currentToken);
                    return false;
                }
            }

            log.error("Unexpected result from token validation script: {}", result);
            return false;

        } catch (Exception e) {
            log.error("Error validating fencing token: resource={}, token={}", resourceKey, fencingToken, e);
            return false;
        }
    }

    // ===================== PRESERVE EXISTING METHODS =====================

    /**
     * Acquire a distributed lock (backward compatibility)
     */
    public boolean acquireLock(String lockKey, long ttl, TimeUnit timeUnit) {
        FencingLockResult result = acquireLockWithFencing(lockKey, ttl, timeUnit);
        return result.isAcquired();
    }

    /**
     * Try to acquire a distributed lock (backward compatibility)
     */
    public boolean tryLock(String lockKey, long ttl, TimeUnit timeUnit) {
        return acquireLock(lockKey, ttl, timeUnit);
    }

    /**
     * Release a distributed lock
     */
    public boolean releaseLock(String lockKey) {
        try {
            String expectedValue = getCurrentLockValue(lockKey);
            if (expectedValue == null) {
                log.warn("Attempted to release lock not held by this instance: key={}", lockKey);
                return false;
            }

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), expectedValue);

            boolean released = result != null && result == 1L;
            if (released) {
                log.info("Lock released successfully: key={}", lockKey);
            } else {
                log.warn("Failed to release lock (not owned by this instance): key={}", lockKey);
            }

            return released;

        } catch (Exception e) {
            log.error("Error releasing lock: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * Check if a lock is currently held
     */
    public boolean isLocked(String lockKey) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            log.error("Error checking lock status: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * Get the current lock holder information
     */
    public String getLockHolder(String lockKey) {
        try {
            return redisTemplate.opsForValue().get(lockKey);
        } catch (Exception e) {
            log.error("Error getting lock holder: key={}", lockKey, e);
            return null;
        }
    }

    /**
     * Extend an existing lock TTL
     */
    public boolean extendLock(String lockKey, long ttl, TimeUnit timeUnit) {
        try {
            String expectedValue = getCurrentLockValue(lockKey);
            if (expectedValue == null) {
                return false;
            }

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(EXTEND_LOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script,
                    Collections.singletonList(lockKey),
                    expectedValue,
                    String.valueOf(timeUnit.toSeconds(ttl)));

            boolean extended = result != null && result == 1L;
            if (extended) {
                log.info("Lock extended successfully: key={}, new_ttl={}s", lockKey, timeUnit.toSeconds(ttl));
            }

            return extended;

        } catch (Exception e) {
            log.error("Error extending lock: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * Get all locks held by this service instance
     */
    public Set<String> getLocksHeldByThisInstance() {
        try {
            Set<String> allLockKeys = redisTemplate.keys("saga:lock:*");
            return allLockKeys.stream()
                    .filter(key -> {
                        String value = redisTemplate.opsForValue().get(key);
                        return value != null && value.startsWith(serviceInstanceId + ":");
                    })
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            log.error("Error getting locks held by this instance", e);
            return Collections.emptySet();
        }
    }

    /**
     * Release all locks held by this service instance
     */
    public void releaseAllLocksForInstance() {
        try {
            Set<String> heldLocks = getLocksHeldByThisInstance();
            for (String lockKey : heldLocks) {
                releaseLock(lockKey);
            }
            log.info("Released {} locks during shutdown", heldLocks.size());
        } catch (Exception e) {
            log.error("Error releasing locks during shutdown", e);
        }
    }

    // ===================== Lock Key Builders =====================

    public static String buildPaymentLockKey(String orderId) {
        return "saga:lock:order:" + orderId + ":payment";
    }

    public static String buildOrderLockKey(String orderId) {
        return "saga:lock:order:" + orderId + ":order";
    }

    public static String buildSagaLockKey(String sagaId) {
        return "saga:lock:saga:" + sagaId;
    }

    // ===================== PHASE 3: Fencing Token Key Builders =====================

    /**
     * Build fencing token key for lock-based tokens
     */
    private String buildFencingTokenKey(String lockKey) {
        return lockKey + ":token";
    }

    /**
     * Build resource token key for operation validation
     */
    private String buildResourceTokenKey(String resourceKey) {
        return "saga:token:resource:" + resourceKey;
    }

    /**
     * Build payment resource token key
     */
    public static String buildPaymentResourceTokenKey(String orderId) {
        return "saga:token:resource:payment:" + orderId;
    }

    /**
     * Build order resource token key
     */
    public static String buildOrderResourceTokenKey(String orderId) {
        return "saga:token:resource:order:" + orderId;
    }

    // ===================== Helper Methods =====================

    /**
     * Get current lock value for this instance
     */
    private String getCurrentLockValue(String lockKey) {
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        if (currentValue != null && currentValue.startsWith(serviceInstanceId + ":")) {
            return currentValue;
        }
        return null;
    }
}