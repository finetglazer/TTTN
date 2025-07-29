package com.graduation.orderservice.service;

import com.graduation.orderservice.constant.Constant;
import com.graduation.orderservice.model.FencingLockResult;
import com.graduation.orderservice.model.Order;
import com.graduation.orderservice.model.OrderStatus;
import com.graduation.orderservice.model.ProcessedMessage;
import com.graduation.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for Order business logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandHandlerService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IdempotencyService idempotencyService;
    private final RedisLockService redisLockService;

    /**
     * Create a new order and trigger saga
     */
    @Transactional
    public Order createOrder(String userId, String userEmail, String userName,
                             String orderDescription, BigDecimal totalAmount, String shippingAddress) {

        log.info(Constant.LOG_CREATING_ORDER, userId, totalAmount);

        try {
            /*
            We had checked data from the request successfully! => No need here
             */
            // Create the order
            Order order = Order.createOrder(userId, userEmail, userName, orderDescription, totalAmount, shippingAddress);

            // Save to database
            Order savedOrder = orderRepository.save(order);
            log.info(Constant.LOG_ORDER_CREATED_SUCCESS, savedOrder.getId());

            // Publish event to trigger saga
            publishOrderCreatedEvent(savedOrder);

            return savedOrder;

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_CREATING_ORDER, userId, e);
            throw new RuntimeException(String.format(Constant.ERROR_FAILED_TO_CREATE_ORDER_RUNTIME, e.getMessage()), e);
        }
    }

    /**
     * Publish OrderCreated event to trigger saga
     */
    public void publishOrderCreatedEvent(Order order) {
        try {
            // Create event payload
            Map<String, Object> event = new HashMap<>();
            event.put(Constant.FIELD_MESSAGE_ID, generateMessageId());
            event.put(Constant.FIELD_TYPE, Constant.EVENT_ORDER_CREATED);
            event.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());

            // Order data for saga
            event.put(Constant.FIELD_ORDER_ID, order.getId());
            event.put(Constant.FIELD_USER_ID, order.getUserId());
            event.put(Constant.FIELD_USER_EMAIL, order.getUserEmail());
            event.put(Constant.FIELD_USER_NAME, order.getUserName());
            event.put(Constant.FIELD_ORDER_DESCRIPTION, order.getOrderDescription());
            event.put(Constant.FIELD_TOTAL_AMOUNT, order.getTotalAmount());
            event.put(Constant.FIELD_ORDER_STATUS, order.getStatus().name());
            event.put(Constant.FIELD_CREATED_AT, order.getCreatedAt().toString());

            // Publish to saga events topic
            kafkaTemplate.send(Constant.TOPIC_ORDER_EVENTS, order.getId().toString(), event);

            log.info(Constant.LOG_PUBLISHED_ORDER_CREATED, order.getId());

        } catch (Exception e) {
            log.error(Constant.LOG_FAILED_PUBLISH_ORDER_CREATED, order.getId(), e);
            // Don't fail the order creation if event publishing fails
            // In production, you might want to retry or use a more robust mechanism
        }
    }

    /**
     * PHASE 3 ENHANCEMENT: Update order status with fencing token validation
     * Eliminates split-brain scenarios by validating fencing tokens before any operations
     * Preserves all existing business logic while adding bulletproof distributed safety
     */
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, String reason,
                                             String sagaId, String fencingToken) {
        log.info("Updating order status with fencing token: orderId={}, status={}, sagaId={}, token={}",
                orderId, newStatus, sagaId, fencingToken);

        // PHASE 3: Acquire distributed lock with fencing token for order updates
        String lockKey = RedisLockService.buildOrderLockKey(orderId.toString());
        FencingLockResult lockResult = redisLockService.acquireLockWithFencing(lockKey, 30, TimeUnit.SECONDS);

        if (lockResult.isAcquired() && lockResult.isValid()) {
            try {
                // PRESERVE EXISTING - Find order
                Optional<Order> optionalOrder = orderRepository.findByIdWithHistories(orderId);
                if (optionalOrder.isEmpty()) {
                    throw new RuntimeException(String.format(Constant.ERROR_ORDER_NOT_FOUND_ID, orderId));
                }

                Order order = optionalOrder.get();

                // Update saga ID if provided (preserve existing logic)
                if (sagaId != null && order.getSagaId() == null) {
                    order.setSagaId(sagaId);
                }

                // PHASE 3: Validate fencing token with Redis
                String resourceTokenKey = RedisLockService.buildOrderResourceTokenKey(orderId.toString());
                if (!redisLockService.validateFencingToken(resourceTokenKey, fencingToken)) {
                    log.error("Order status update rejected due to stale fencing token: orderId={}, token={}",
                            orderId, fencingToken);
                    throw new RuntimeException("Order update rejected due to stale operation");
                }

                // PHASE 3: Update status with fencing token validation
                boolean updateSuccessful = order.updateStatusWithFencing(
                        newStatus, reason, Constant.ACTOR_SAGA_ORCHESTRATOR, Long.valueOf(fencingToken));

                if (!updateSuccessful) {
                    log.error("Order status update failed due to fencing token validation: orderId={}, token={}",
                            orderId, fencingToken);
                    throw new RuntimeException("Order update failed due to stale fencing token");
                }

                // PRESERVE EXISTING - Save order
                orderRepository.save(order);
                log.info("Order status updated successfully with fencing token: orderId={}, status={}, token={}",
                        orderId, newStatus, fencingToken);

            } finally {
                redisLockService.releaseLock(lockKey);
            }
        } else {
            log.error("Failed to acquire order lock for status update: orderId={}", orderId);
            throw new RuntimeException("Failed to acquire lock for order update");
        }
    }


    /**
     * PHASE 3: Cancel order with fencing token validation
     * Enhanced version of existing cancelOrder method
     */
    @Transactional
    public void cancelOrderWithFencing(Long orderId, String reason, String sagaId, String fencingToken) {
        log.info("Cancelling order with fencing token: orderId={}, sagaId={}, token={}", orderId, sagaId, fencingToken);

        // PHASE 3: Acquire distributed lock with fencing token for order cancellation
        String lockKey = RedisLockService.buildOrderLockKey(orderId.toString());
        FencingLockResult lockResult = redisLockService.acquireLockWithFencing(lockKey, 30, TimeUnit.SECONDS);

        if (lockResult.isAcquired() && lockResult.isValid()) {
            try {
                // PRESERVE EXISTING - Find order
                Optional<Order> optionalOrder = orderRepository.findByIdWithHistories(orderId);
                if (optionalOrder.isEmpty()) {
                    throw new RuntimeException(String.format(Constant.ERROR_ORDER_NOT_FOUND_ID, orderId));
                }

                Order order = optionalOrder.get();

                // PHASE 3: Validate fencing token with Redis
                String resourceTokenKey = RedisLockService.buildOrderResourceTokenKey(orderId.toString());
                if (!redisLockService.validateFencingToken(resourceTokenKey, fencingToken)) {
                    log.error("Order cancellation rejected due to stale fencing token: orderId={}, token={}",
                            orderId, fencingToken);
                    throw new RuntimeException("Order cancellation rejected due to stale operation");
                }

                // PHASE 3: Cancel order with fencing token validation
                boolean cancellationSuccessful = order.cancelWithFencing(
                        reason, Constant.ACTOR_SAGA_COMPENSATION, Long.valueOf(fencingToken));

                if (!cancellationSuccessful) {
                    log.error("Order cancellation failed due to fencing token validation: orderId={}, token={}",
                            orderId, fencingToken);
                    throw new RuntimeException("Order cancellation failed due to stale fencing token");
                }

                // PRESERVE EXISTING - Save order
                orderRepository.save(order);
                log.info("Order cancelled successfully with fencing token: orderId={}, token={}", orderId, fencingToken);

            } finally {
                redisLockService.releaseLock(lockKey);
            }
        } else {
            log.error("Failed to acquire order lock for cancellation: orderId={}", orderId);
            throw new RuntimeException("Failed to acquire lock for order cancellation");
        }
    }


     /**
     * PHASE 3: Handle order update confirmed command with fencing token
     */
    public void handleUpdateOrderConfirmed(Map<String, Object> command) {
        Map<String, Object> payload = (Map<String, Object>) command.get(Constant.FIELD_PAYLOAD);
        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);
        String sagaId = command.get(Constant.FIELD_SAGA_ID).toString();
        String fencingToken = (String) command.get("fencingToken"); // PHASE 3: Extract fencing token

        // PRESERVE EXISTING - Idempotency check
        if (idempotencyService.isProcessed(messageId, sagaId)) {
            log.info(Constant.LOG_MESSAGE_ALREADY_PROCESSED, messageId, sagaId);
            return;
        }

        String reason = (String) payload.getOrDefault(Constant.FIELD_REASON, Constant.REASON_ORDER_CONFIRMED);
        Long orderId = Long.valueOf(payload.get(Constant.FIELD_ORDER_ID).toString());

        try {
            // PRESERVE EXISTING - Validate payload
            if (validatePayload(sagaId, messageId, orderId, reason)) return;

            // PHASE 3: Update order status with fencing token
            updateOrderStatus(orderId, OrderStatus.CONFIRMED, reason, sagaId, fencingToken);

            // PRESERVE EXISTING - Record success and publish event
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_STATUS_UPDATED_CONFIRMED, true,
                    Constant.STATUS_DESC_CONFIRMED, null);

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_UPDATING_CONFIRMED, e.getMessage(), e);
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_STATUS_UPDATE_FAILED, false,
                    null, e.getMessage());
        }
    }

    /**
     * Handle order update delivered command
     */
    public void handleUpdateOrderDelivered(Map<String, Object> command) {
        String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);
        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);
        String fencingToken = (String) command.get("fencingToken"); // PHASE 3: Extract fencing token

        // Idempotency check
        if (idempotencyService.isProcessed(messageId, sagaId)) {
            log.info(Constant.LOG_MESSAGE_ALREADY_PROCESSED, messageId, sagaId);
            return; // Skip processing if already handled
        }

        // Extract payload first (consistent with other handlers)
        Map<String, Object> payload = (Map<String, Object>) command.get(Constant.FIELD_PAYLOAD);

        Long orderId = Long.valueOf(payload.get(Constant.FIELD_ORDER_ID).toString());
        String reason = (String) payload.getOrDefault(Constant.FIELD_REASON, Constant.REASON_ORDER_DELIVERED_SUCCESS);
        // Validate orderId and reason
        if (validatePayload(sagaId, messageId, orderId, reason)) return;

        log.info(Constant.LOG_UPDATING_ORDER_DELIVERED, orderId, sagaId);

        try {

            // Update order status to DELIVERED
            updateOrderStatus(orderId, OrderStatus.DELIVERED, reason, sagaId, fencingToken);

            // Add 10-second delay before sending event to saga
            log.info(Constant.LOG_PROCESSING_DELIVERY_WAIT, sagaId);
            try {
                Thread.sleep(10000); // 10 seconds delay
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn(Constant.LOG_THREAD_INTERRUPTED, sagaId);
            }
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);

            // Publish success event
            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_STATUS_UPDATED_DELIVERED, true,
                    Constant.STATUS_DESC_DELIVERED, null);

            // Log saga completion
            log.info(Constant.LOG_SAGA_COMPLETED, sagaId, orderId);

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_UPDATING_DELIVERED, e.getMessage(), e);
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_STATUS_UPDATE_FAILED, false,
                    null, e.getMessage());

            // Log saga failure
            log.error(Constant.LOG_SAGA_FAILED, sagaId, orderId, e.getMessage());
        }
    }

    private boolean validatePayload(String sagaId, String messageId, Long orderId, String reason) {
        if (orderId <= 0 || reason == null || reason.isEmpty()) {
            log.error(Constant.LOG_INVALID_ORDER_ID, orderId, sagaId);
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            publishOrderEvent(sagaId, null, Constant.EVENT_ORDER_STATUS_UPDATE_FAILED, false,
                    null, Constant.ERROR_INVALID_ORDER_ID);
            return true;
        }
        return false;
    }

    /**
     * PHASE 3: Handle order cancellation with fencing token validation
     */
    public void handleCancelOrder(Map<String, Object> command) {
        String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);
        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);
        String fencingToken = (String) command.get("fencingToken"); // PHASE 3: Extract fencing token
        Map<String, Object> payload = (Map<String, Object>) command.get(Constant.FIELD_PAYLOAD);

        Long orderId = Long.valueOf(payload.get(Constant.FIELD_ORDER_ID).toString());
        String reason = (String) payload.getOrDefault(Constant.FIELD_REASON, Constant.REASON_ORDER_CANCELLED_SAGA);

        try {
            // PRESERVE EXISTING - Validate payload
            if (validatePayload(sagaId, messageId, orderId, reason)) return;

            // PHASE 3: Cancel order with fencing token validation
            cancelOrderWithFencing(orderId, reason, sagaId, fencingToken);

            // PRESERVE EXISTING - Record success and publish event
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_CANCELLED, true,
                    Constant.STATUS_DESC_CANCELLED, null);

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_CANCELLING_ORDER, e.getMessage(), e);

            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_CANCELLATION_FAILED, false,
                    null, e.getMessage());
        }
    }

    /**
     * Initiate order cancellation by publishing cancel request event to Saga
     */
    @Transactional
    public void initiateCancellation(Order order, String reason) {
        try {
            log.info("Initiating cancellation for order: {}", order.getId());

            // Publish "CancelRequestReceived" event to Saga
            publishCancelRequestEvent(order, reason);

            log.info("Cancel request event published for order: {}", order.getId());

        } catch (Exception e) {
            log.error("Failed to initiate cancellation for order: {}", order.getId(), e);
            throw new RuntimeException("Failed to initiate order cancellation: " + e.getMessage(), e);
        }
    }

    /**
     * PHASE 3: Atomically update order status with fencing token validation
     * Enhanced version that combines database-level locking with fencing tokens
     * Uses the existing updateOrderStatusAtomically method for database operations
     */
    @Transactional
    public boolean updateOrderStatusAtomicallyWithFencing(Long orderId, OrderStatus expectedCurrentStatus,
                                                          OrderStatus newStatus, String reason, String changedBy,
                                                          String fencingToken) {
        log.info("Attempting atomic status update with fencing token: orderId={}, from={}, to={}, token={}",
                orderId, expectedCurrentStatus, newStatus, fencingToken);

        // PHASE 3: Acquire distributed lock with fencing token
        String lockKey = RedisLockService.buildOrderLockKey(orderId.toString());
        FencingLockResult lockResult = redisLockService.acquireLockWithFencing(lockKey, 30, TimeUnit.SECONDS);

        if (lockResult.isAcquired() && lockResult.isValid()) {
            try {
                // PHASE 3: Validate fencing token with Redis first
                String resourceTokenKey = RedisLockService.buildOrderResourceTokenKey(orderId.toString());
                if (!redisLockService.validateFencingToken(resourceTokenKey, fencingToken)) {
                    log.error("Atomic order update rejected due to stale fencing token: orderId={}, token={}",
                            orderId, fencingToken);
                    return false;
                }

                // PHASE 3: Use the existing atomic update method with fencing token
                boolean success = updateOrderStatusAtomicallyWithFencingValidation(
                        orderId, expectedCurrentStatus, newStatus, reason, changedBy, Long.valueOf(fencingToken));

                if (success) {
                    log.info("Atomic order status update successful with fencing token: orderId={}, token={}",
                            orderId, fencingToken);
                } else {
                    log.warn("Atomic order status update failed - status mismatch or stale token: orderId={}, expected={}, actual=?",
                            orderId, expectedCurrentStatus);
                }

                return success;

            } finally {
                redisLockService.releaseLock(lockKey);
            }
        } else {
            log.error("Failed to acquire order lock for atomic update: orderId={}", orderId);
            return false;
        }
    }

    /**
     * PHASE 3: Enhanced version of updateOrderStatusAtomically with fencing token validation
     * Preserves all existing database-level atomic logic while adding fencing token protection
     */
    @Transactional
    public boolean updateOrderStatusAtomicallyWithFencingValidation(Long orderId, OrderStatus expectedCurrentStatus,
                                                                    OrderStatus newStatus, String reason, String changedBy,
                                                                    Long fencingToken) {
        log.info("Attempting atomic status update with fencing validation: orderId={}, from={}, to={}, token={}",
                orderId, expectedCurrentStatus, newStatus, fencingToken);

        try {
            // PRESERVE EXISTING - Use pessimistic write lock to ensure atomicity
            Optional<Order> optionalOrder = orderRepository.findByIdForAtomicUpdate(orderId);

            if (optionalOrder.isEmpty()) {
                log.warn("Order not found for atomic update: orderId={}", orderId);
                return false;
            }

            Order order = optionalOrder.get();

            // PRESERVE EXISTING - Compare-and-swap: only update if current status matches expected
            if (order.getStatus() != expectedCurrentStatus) {
                log.warn("Atomic update failed - status mismatch: orderId={}, expected={}, actual={}",
                        orderId, expectedCurrentStatus, order.getStatus());
                return false;
            }

            // PHASE 3: Additional fencing token validation at entity level
            if (fencingToken != null && !order.isValidFencingToken(fencingToken)) {
                log.warn("Atomic update failed - stale fencing token: orderId={}, incoming={}, current={}",
                        orderId, fencingToken, order.getFencingToken());
                return false;
            }

            // PHASE 3: Perform the atomic update with fencing token
            if (fencingToken != null) {
                // Use fencing-aware update method
                boolean updateSuccess = order.updateStatusWithFencing(newStatus, reason, changedBy, fencingToken);
                if (!updateSuccess) {
                    log.warn("Order status update rejected by entity fencing validation: orderId={}, token={}",
                            orderId, fencingToken);
                    return false;
                }
            } else {
                // PRESERVE EXISTING - Fall back to regular update if no fencing token
                order.updateStatus(newStatus, reason, changedBy);
            }

            // PRESERVE EXISTING - Save the order
            orderRepository.save(order);

            log.info("Atomic status update with fencing validation successful: orderId={}, from={}, to={}, token={}",
                    orderId, expectedCurrentStatus, newStatus, fencingToken);
            return true;

        } catch (Exception e) {
            log.error("Error during atomic status update with fencing: orderId={}, from={}, to={}, token={}",
                    orderId, expectedCurrentStatus, newStatus, fencingToken, e);
            return false;
        }
    }

    /**
     * PRESERVE EXISTING: Your original updateOrderStatusAtomically method
     * Enhanced to properly use the fencing token parameter
     */
    @Transactional
    public boolean updateOrderStatusAtomically(Long orderId, OrderStatus expectedCurrentStatus,
                                               OrderStatus newStatus, String reason, String changedBy, Long fencingToken) {
        log.info("Attempting atomic status update: orderId={}, from={}, to={}, fencingToken={}",
                orderId, expectedCurrentStatus, newStatus, fencingToken);

        try {
            // Use pessimistic write lock to ensure atomicity
            Optional<Order> optionalOrder = orderRepository.findByIdForAtomicUpdate(orderId);

            if (optionalOrder.isEmpty()) {
                log.warn("Order not found for atomic update: orderId={}", orderId);
                return false;
            }

            Order order = optionalOrder.get();

            // Compare-and-swap: only update if current status matches expected
            if (order.getStatus() != expectedCurrentStatus) {
                log.warn("Atomic update failed - status mismatch: orderId={}, expected={}, actual={}",
                        orderId, expectedCurrentStatus, order.getStatus());
                return false;
            }

            // PHASE 3: Add fencing token validation if provided
            if (fencingToken != null) {
                // Validate fencing token before proceeding
                if (!order.isValidFencingToken(fencingToken)) {
                    log.warn("Atomic update failed - invalid fencing token: orderId={}, incoming={}, current={}",
                            orderId, fencingToken, order.getFencingToken());
                    return false;
                }

                // Use fencing-aware update
                boolean updateSuccess = order.updateStatusWithFencing(newStatus, reason, changedBy, fencingToken);
                if (!updateSuccess) {
                    log.warn("Order status update rejected by fencing validation: orderId={}, token={}",
                            orderId, fencingToken);
                    return false;
                }
            } else {
                // Regular update without fencing token
                order.updateStatus(newStatus, reason, changedBy);
            }

            orderRepository.save(order);

            log.info("Atomic status update successful: orderId={}, from={}, to={}, fencingToken={}",
                    orderId, expectedCurrentStatus, newStatus, fencingToken);
            return true;

        } catch (Exception e) {
            log.error("Error during atomic status update: orderId={}, from={}, to={}, fencingToken={}",
                    orderId, expectedCurrentStatus, newStatus, fencingToken, e);
            return false;
        }
    }

    /**
     * Publish cancel request event to trigger saga cancellation flow
     */
    private void publishCancelRequestEvent(Order order, String reason) {
        try {
            // Create cancel request event payload
            Map<String, Object> event = new HashMap<>();
            event.put(Constant.FIELD_MESSAGE_ID, generateMessageId());
            event.put(Constant.FIELD_TYPE, Constant.EVENT_CANCEL_REQUEST_RECEIVED);
            event.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());

            // Order data for saga
            event.put(Constant.FIELD_ORDER_ID, order.getId());
            event.put(Constant.FIELD_USER_ID, order.getUserId());
            event.put(Constant.FIELD_SAGA_ID, order.getSagaId());
            event.put(Constant.FIELD_ORDER_STATUS, order.getStatus().name());
            event.put(Constant.FIELD_REASON, reason);
            event.put(Constant.FIELD_USER_EMAIL, order.getUserEmail());
            event.put(Constant.FIELD_USER_NAME, order.getUserName());
            event.put(Constant.FIELD_TOTAL_AMOUNT, order.getTotalAmount());

            // Publish to saga events topic
            kafkaTemplate.send(Constant.TOPIC_ORDER_EVENTS, order.getId().toString(), event);

            log.info("Published cancel request event for order: {} with saga: {}",
                    order.getId(), order.getSagaId());

        } catch (Exception e) {
            log.error("Failed to publish cancel request event for order: {}", order.getId(), e);
            throw new RuntimeException("Failed to publish cancel request event", e);
        }
    }

    /**
     * Publish order event back to saga orchestrator
     */
    private void publishOrderEvent(String sagaId, Long orderId, String eventType,
                                   boolean success, String successMessage, String errorMessage) {
        try {
            // TODO: Inject KafkaTemplate when available
            // For now, just log the event that would be published

            log.info(Constant.LOG_PUBLISHING_ORDER_EVENT,
                    sagaId, orderId, eventType, success);

            if (success) {
                log.info(Constant.LOG_ORDER_EVENT_SUCCESS, successMessage);
            } else {
                log.error(Constant.LOG_ORDER_EVENT_FAILURE, errorMessage);
            }

            Map<String, Object> event = new HashMap<>();
            event.put(Constant.FIELD_MESSAGE_ID, Constant.PREFIX_ORDER_EVENT + System.currentTimeMillis());
            event.put(Constant.FIELD_SAGA_ID, sagaId);
            event.put(Constant.FIELD_TYPE, eventType);
            event.put(Constant.FIELD_SUCCESS, success);
            event.put(Constant.FIELD_ORDER_ID, orderId);
            event.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());

            if (success) {
                event.put(Constant.RESPONSE_MESSAGE, successMessage);
            } else {
                event.put(Constant.FIELD_ERROR_MESSAGE, errorMessage);
            }

            kafkaTemplate.send(Constant.TOPIC_ORDER_EVENTS, sagaId, event);

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_PUBLISHING_EVENT, e.getMessage(), e);
        }
    }

    /**
     * Generate unique message ID
     */
    private String generateMessageId() {
        return Constant.PREFIX_ORDER_MESSAGE + System.currentTimeMillis() + "_" +
                java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}