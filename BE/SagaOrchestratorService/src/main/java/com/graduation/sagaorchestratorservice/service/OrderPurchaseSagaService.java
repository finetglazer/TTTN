package com.graduation.sagaorchestratorservice.service;

import com.graduation.sagaorchestratorservice.constants.Constant;
import com.graduation.sagaorchestratorservice.exception.SagaExecutionException;
import com.graduation.sagaorchestratorservice.exception.SagaNotFoundException;
import com.graduation.sagaorchestratorservice.model.FencingLockResult;
import com.graduation.sagaorchestratorservice.model.OrderPurchaseSagaState;
import com.graduation.sagaorchestratorservice.model.SagaEvent;
import com.graduation.sagaorchestratorservice.model.enums.*;
import com.graduation.sagaorchestratorservice.repository.OrderPurchaseSagaStateRepository;
import com.graduation.sagaorchestratorservice.utils.MessageIdGenerator;
import com.graduation.sagaorchestratorservice.utils.SagaIdGenerator;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for managing Order Purchase Saga workflow
 * Handles the complete lifecycle from order creation to completion/compensation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPurchaseSagaService {

    private final OrderPurchaseSagaStateRepository sagaRepository;
    private final KafkaMessagePublisher messagePublisher;
    private final IdempotencyService idempotencyService;
    private final SagaMonitoringService monitoringService;
    private final RedisLockService redisLockService;

    @Value("${saga.retry.max-attempts:3}")
    private int maxRetries;

    @Value("${saga.timeout.default-minutes:10}")
    private long defaultTimeoutMinutes;

    // Kafka topic configuration
    @Value("${kafka.topics.order-commands}")
    private String orderCommandsTopic;

    @Value("${kafka.topics.payment-commands}")
    private String paymentCommandsTopic;

    @Value("${saga.compensation.max-retries:3}")
    private int defaultMaxCompensationRetries;

    @Value("${saga.retry.delay-seconds:5}")
    private int baseRetryDelaySeconds;

    @Value("${saga.lock.monitoring.enabled:true}")
    private boolean lockMonitoringEnabled;

    // Synchronization for preventing race conditions
    private final ConcurrentHashMap<String, ReentrantLock> sagaLocks = new ConcurrentHashMap<>();

    /**
     * Start a new order purchase saga
     * Called when an order is created and needs payment processing
     */
    @Transactional
    public OrderPurchaseSagaState startSaga(String userId, Long orderId, String userEmail,
                                            String userName, String orderDescription, BigDecimal totalAmount) {

        log.info(Constant.LOG_STARTING_ORDER_PURCHASE_SAGA, orderId, userId);

        // Check if saga already exists for this order
        Optional<OrderPurchaseSagaState> existingSaga = sagaRepository.findByOrderId(orderId);
        if (existingSaga.isPresent()) {
            log.warn(Constant.LOG_SAGA_ALREADY_EXISTS, orderId);
            return existingSaga.get();
        }

        // Generate unique saga ID
        String sagaId = SagaIdGenerator.generateForType("ORDER_PURCHASE");

        // Create and save the saga
        OrderPurchaseSagaState saga = OrderPurchaseSagaState.initiate(
                sagaId, userId, orderId, userEmail, userName, orderDescription, totalAmount);

        sagaRepository.save(saga);

        // Record in monitoring
        monitoringService.recordSagaStarted(sagaId, "ORDER_PURCHASE");

        // Process the first step (PROCESS_PAYMENT)
        processNextStepWithFencing(saga);

        log.info(Constant.LOG_ORDER_PURCHASE_SAGA_STARTED, sagaId);
        return saga;
    }

//    /**
//     * Process the next step in the saga
//     */
//    @Transactional
//    public void processNextStep(OrderPurchaseSagaState saga) {
//        log.debug(Constant.LOG_PROCESSING_STEP, saga.getCurrentStep(), saga.getSagaId());
//
//        // Handle completion
//        if (saga.getCurrentStep() == OrderPurchaseSagaStep.COMPLETE_SAGA) {
//            completeSaga(saga);
//            return;
//        }
//
//        try {
//            // Create command message for current step
//            Map<String, Object> command = createCommandForCurrentStep(saga);
//            if (command == null) {
//                log.warn("No command created for step: {} in saga: {}",
//                        saga.getCurrentStep(), saga.getSagaId());
//                return;
//            }
//
//            // Save saga state before sending command
//            sagaRepository.save(saga);
//
//            // Determine target topic and publish command
//            String targetTopic = getTopicForCommand(saga.getCurrentStep().getCommandType());
//
//            messagePublisher.publishSagaStepCommand(
//                    saga.getSagaId(),
//                    saga.getCurrentStep().getStepNumber(),
//                    saga.getCurrentStep().getCommandType().name(),
//                    command,
//                    targetTopic
//            );
//
//            log.info(Constant.LOG_PUBLISHED_COMMAND,
//                    saga.getCurrentStep().getCommandType(), saga.getSagaId(), targetTopic);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_PROCESSING_STEP, saga.getCurrentStep(), saga.getSagaId(), e);
//            handleStepFailure(saga, "Failed to process step: " + e.getMessage());
//        }
//    }


    /**
     * PHASE 2 ENHANCEMENT: Handle incoming event messages from services with DISTRIBUTED LOCKING
     * Preserves all existing idempotency, validation, and compensation logic
     * Adds distributed saga locking with retry mechanism
     */
    @Transactional
    public void handleEventMessage(Map<String, Object> eventData) {
        String sagaId = (String) eventData.get(Constant.FIELD_SAGA_ID);
        String eventType = (String) eventData.get(Constant.FIELD_TYPE);
        Boolean success = (Boolean) eventData.get(Constant.FIELD_SUCCESS);

        log.debug(Constant.LOG_HANDLING_EVENT, eventType, sagaId);

        // PHASE 2: Use distributed saga lock instead of ReentrantLock
        String sagaLockKey = RedisLockService.buildSagaLockKey(sagaId);

        // Try to acquire distributed lock with retry mechanism
        if (acquireSagaLockWithRetry(sagaLockKey, sagaId)) {
            try {
                // PRESERVE ALL EXISTING LOGIC - Find the saga
                Optional<OrderPurchaseSagaState> optionalSaga = sagaRepository.findById(sagaId);
                if (optionalSaga.isEmpty()) {
                    log.warn("Received event for unknown saga: {}", sagaId);
                    return;
                }

                OrderPurchaseSagaState saga = optionalSaga.get();

                // PRESERVE EXISTING - Check idempotency
                String messageId = (String) eventData.get(Constant.FIELD_MESSAGE_ID);
                ActionType actionType = isCompensationEvent(eventType) ? ActionType.COMPENSATION : ActionType.FORWARD;

                if (idempotencyService.isProcessed(messageId, sagaId,
                        saga.getCurrentStep() != null ? saga.getCurrentStep().getStepNumber() : null,
                        eventType, actionType)) {
                    log.info(Constant.LOG_EVENT_ALREADY_PROCESSED, eventType, sagaId);
                    return;
                }

                try {
                    // PRESERVE EXISTING - Validate event matches current step
                    if (!isEventForCurrentStep(saga, eventType)) {
                        log.warn(Constant.LOG_EVENT_IGNORED_WRONG_STEP,
                                eventType, saga.getCurrentStep(), sagaId);
                        recordEventProcessing(eventData, saga, "Event ignored - doesn't match current step");
                        return;
                    }

                    // PRESERVE EXISTING - Process based on success/failure
                    if (Boolean.TRUE.equals(success)) {
                        processSuccessEvent(saga, eventData);
                    } else {
                        processFailureEvent(saga, eventData);
                    }

                    // PRESERVE EXISTING - Record successful processing
                    recordEventProcessing(eventData, saga, "Event processed successfully");

                } catch (Exception e) {
                    log.error("Error handling event {} for saga {}", eventType, sagaId, e);
                    recordEventProcessing(eventData, saga, "Error processing event: " + e.getMessage());
                }
            } finally {
                // PHASE 2: Release distributed saga lock
                boolean released = redisLockService.releaseLock(sagaLockKey);
                log.debug("Distributed saga lock released: sagaId={}, released={}", sagaId, released);
            }
        } else {
            // Failed to acquire saga lock after retries
            log.warn("Failed to acquire saga lock after retries, event processing skipped: sagaId={}, eventType={}",
                    sagaId, eventType);

            // TODO: Could implement event queuing or DLQ here if needed
            // For now, we'll let the event be reprocessed by Kafka retry mechanism
        }
    }

    /**
     * PHASE 2: Acquire distributed saga lock with retry mechanism using existing retry configuration
     * Preserves your excellent exponential backoff logic
     */
    private boolean acquireSagaLockWithRetry(String sagaLockKey, String sagaId) {
        int maxAttempts = maxRetries; // Use existing saga retry configuration

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            // Try to acquire the lock (2 minutes TTL for saga processing)
            if (redisLockService.tryLock(sagaLockKey, 2, TimeUnit.MINUTES)) {
                log.debug("Distributed saga lock acquired: sagaId={}, attempt={}", sagaId, attempt);
                return true;
            }

            if (attempt < maxAttempts) {
                // Use your existing retry delay calculation logic
                long delayMs = calculateSagaLockRetryDelay(attempt);

                log.debug("Saga lock acquisition failed, retrying in {}ms: sagaId={}, attempt={}/{}",
                        delayMs, sagaId, attempt, maxAttempts);

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Saga lock retry interrupted: sagaId={}", sagaId);
                    return false;
                }
            }
        }

        log.warn("Failed to acquire saga lock after {} attempts: sagaId={}", maxAttempts, sagaId);
        return false;
    }

    /**
     * PHASE 2: Calculate retry delay for saga lock acquisition using existing retry logic
     * Reuses your proven exponential backoff algorithm but with shorter delays for locks
     */
    private long calculateSagaLockRetryDelay(int attempt) {
        // Use shorter base delay for lock acquisition (500ms vs 5s for saga steps)
        long baseLockDelayMs = 500L;

        // Reuse your exponential backoff logic: base * 2^(attempt-1)
        long exponentialDelay = baseLockDelayMs * (1L << (attempt - 1));

        // Reuse your jitter logic (±20% random variation)
        double jitter = 0.8 + (Math.random() * 0.4); // 0.8 to 1.2
        long delayWithJitter = Math.round(exponentialDelay * jitter);

        // Cap at reasonable maximum for lock retries (2 seconds vs 60s for saga steps)
        long maxLockDelayMs = 2000L;
        return Math.min(delayWithJitter, maxLockDelayMs);
    }

    // Helper method to determine if event is compensation
    private boolean isCompensationEvent(String eventType) {
        return eventType.contains("FAILED") || eventType.contains("COMPENSATION") || eventType.contains("ROLLBACK");
    }

    /**
     * Process a successful event
     */
    private void processSuccessEvent(OrderPurchaseSagaState saga, Map<String, Object> eventData) {
        String eventType = (String) eventData.get(Constant.FIELD_TYPE);
        log.info(Constant.LOG_PROCESSING_SUCCESS_EVENT, eventType, saga.getSagaId());

        // Update saga with event data
        updateSagaWithEventData(saga, eventData);

        // Record monitoring
        long processingTime = System.currentTimeMillis(); // Simple timing
        monitoringService.recordMessageProcessed(saga.getSagaId(), eventType, processingTime);

        if (saga.getStatus() == SagaStatus.COMPENSATING) {
            // Handle compensation step success
            handleCompensationStepSuccess(saga);
        } else {
            // Normal flow - move to next step
            saga.addEvent(SagaEvent.stepCompleted(saga.getCurrentStep().getDescription()));
            saga.moveToNextStep();
        }

        // Save and continue
        sagaRepository.save(saga);

        // Process next step if still active
        if (saga.getStatus() == SagaStatus.IN_PROGRESS || saga.getStatus() == SagaStatus.COMPENSATING) {
            processNextStepWithFencing(saga);
        }
    }

    /**
     * Process a failure event
     */
    private void processFailureEvent(OrderPurchaseSagaState saga, Map<String, Object> eventData) {
        String eventType = (String) eventData.get(Constant.FIELD_TYPE);
        String errorMessage = (String) eventData.get(Constant.FIELD_ERROR_MESSAGE);

        log.warn(Constant.LOG_PROCESSING_FAILURE_EVENT, eventType, saga.getSagaId(), errorMessage);

        // Record monitoring
        monitoringService.recordMessageFailed(saga.getSagaId(), eventType, errorMessage);

        // Check if this is a compensation step failure
        if (saga.getStatus() == SagaStatus.COMPENSATING) {
            // Handle compensation step failure
            handleCompensationStepFailure(saga, errorMessage != null ? errorMessage : Constant.ERROR_STEP_FAILED);
        } else {
            // Handle normal step failure
            handleStepFailure(saga, errorMessage != null ? errorMessage : Constant.ERROR_STEP_FAILED);
        }
    }

    /**
     * Handle step failure and start compensation if needed
     */
    private void handleStepFailure(OrderPurchaseSagaState saga, String reason) {
        log.warn(Constant.LOG_HANDLING_STEP_FAILURE, saga.getSagaId(), reason);

        saga.handleFailure(reason, saga.getCurrentStep().name());

        // Start compensation if this step requires it
        if (saga.getCurrentStep().requiresCompensation()) {
            startCompensation(saga);
        } else {
            // For non-compensatable steps, just mark as failed
            saga.setStatus(SagaStatus.COMPENSATION_COMPLETED);
            saga.setEndTime(Instant.now());
            monitoringService.recordSagaFailed(saga.getSagaId(), reason);
        }

        sagaRepository.save(saga);
    }

    /**
     * Start compensation process
     */
    private void startCompensation(OrderPurchaseSagaState saga) {
        log.info(Constant.LOG_STARTING_COMPENSATION, saga.getSagaId());

        saga.startCompensation();
        sagaRepository.save(saga);

        // Process compensation steps
        processNextStepWithFencing(saga);
    }

    /**
     * PHASE 3: Start compensation with fencing token protection
     * Ensures all compensation steps use fencing tokens to prevent stale operations
     */
    private void startCompensationWithFencing(OrderPurchaseSagaState saga, String paymentFencingToken) {
        log.info("Starting compensation with fencing token protection: sagaId={}, paymentToken={}",
                saga.getSagaId(), paymentFencingToken);

        try {
            // PRESERVE EXISTING - Determine compensation strategy based on completed steps
            saga.setStatus(SagaStatus.COMPENSATING);

            // PHASE 3: Store fencing tokens in saga for use during compensation
            saga.addEvent(SagaEvent.of("COMPENSATION_STARTED_WITH_FENCING",
                    "Compensation started with fencing token protection, paymentToken=" + paymentFencingToken));

            // PRESERVE EXISTING - Execute compensation steps in reverse order
            // PHASE 3: Pass fencing tokens to compensation methods
            executeCompensationStepsWithFencing(saga, paymentFencingToken);

        } catch (Exception e) {
            log.error("Error starting compensation with fencing tokens: sagaId={}", saga.getSagaId(), e);
            saga.setStatus(SagaStatus.FAILED);
            saga.setFailureReason("Failed to start compensation: " + e.getMessage());
        }

        sagaRepository.save(saga);
    }

    /**
     * PHASE 3: Execute compensation steps with fencing token validation
     * Preserves existing compensation logic while adding fencing token protection
     */
    private void executeCompensationStepsWithFencing(OrderPurchaseSagaState saga, String paymentFencingToken) {
        log.info("Executing compensation steps with fencing tokens: sagaId={}", saga.getSagaId());

        // PRESERVE EXISTING - Determine which steps need compensation based on saga state
        boolean needsPaymentCompensation = saga.getCurrentStep() != null &&
                saga.getCurrentStep().getStepNumber() >= 2; // Payment step is step 2

        boolean needsOrderCompensation = saga.getCurrentStep() != null &&
                saga.getCurrentStep().getStepNumber() >= 1; // Order step is step 1

        try {
            // PHASE 3: Compensate payment with fencing token (if needed)
            if (needsPaymentCompensation) {
                compensatePaymentWithFencing(saga, paymentFencingToken);
            }

            // PHASE 3: Compensate order with fencing token (if needed)
            if (needsOrderCompensation) {
                compensateOrderWithFencing(saga);
            }

            // Mark compensation as completed
            saga.setStatus(SagaStatus.FAILED); // Failed due to cancellation, but compensated
            saga.addEvent(SagaEvent.compensationCompleted());

        } catch (Exception e) {
            log.error("Error during compensation execution with fencing tokens: sagaId={}", saga.getSagaId(), e);
            saga.setStatus(SagaStatus.FAILED);
            saga.setFailureReason("Compensation failed: " + e.getMessage());
        }
    }

    /**
     * PHASE 3: Compensate payment with fencing token
     */
    private void compensatePaymentWithFencing(OrderPurchaseSagaState saga, String fencingToken) {
        log.info("Compensating payment with fencing token: sagaId={}, token={}", saga.getSagaId(), fencingToken);

        try {
            // PRESERVE EXISTING - Build compensation command payload
            Map<String, Object> payload = Map.of(
                    Constant.FIELD_ORDER_ID, saga.getOrderId().toString(),
                    Constant.FIELD_REASON, "Saga compensation with fencing token: " + fencingToken
            );

            // PHASE 3: Add fencing token to command
            Map<String, Object> command = Map.of(
                    Constant.FIELD_MESSAGE_ID, MessageIdGenerator.generateForSagaStep(saga.getSagaId(), 999),
                    Constant.FIELD_SAGA_ID, saga.getSagaId(),
                    Constant.FIELD_TYPE, "REVERSE_PAYMENT_WITH_FENCING",
                    Constant.FIELD_PAYLOAD, payload,
                    "fencingToken", fencingToken, // PHASE 3: Include fencing token
                    Constant.FIELD_TIMESTAMP, System.currentTimeMillis()
            );

            // PRESERVE EXISTING - Publish to payment service
            messagePublisher.publishCommand(command, paymentCommandsTopic, saga.getSagaId());

            log.info("Payment compensation command with fencing token published: sagaId={}, token={}",
                    saga.getSagaId(), fencingToken);

        } catch (Exception e) {
            log.error("Error compensating payment with fencing token: sagaId={}, token={}",
                    saga.getSagaId(), fencingToken, e);
            throw e;
        }
    }

    /**
     * PHASE 3: Compensate order with fencing token
     */
    private void compensateOrderWithFencing(OrderPurchaseSagaState saga) {
        log.info("Compensating order with fencing token: sagaId={}", saga.getSagaId());

        // PHASE 3: Acquire order lock with fencing token for compensation
        String orderLockKey = RedisLockService.buildOrderLockKey(saga.getOrderId().toString());
        FencingLockResult orderLockResult = redisLockService.tryLockWithFencing(orderLockKey, 30, TimeUnit.SECONDS);

        if (orderLockResult.isAcquired() && orderLockResult.isValid()) {
            try {
                // PRESERVE EXISTING - Build compensation command payload
                Map<String, Object> payload = Map.of(
                        Constant.FIELD_ORDER_ID, saga.getOrderId().toString(),
                        Constant.FIELD_REASON, "Saga compensation with fencing token: " + orderLockResult.getFencingToken()
                );

                // PHASE 3: Add fencing token to command
                Map<String, Object> command = Map.of(
                        Constant.FIELD_MESSAGE_ID, MessageIdGenerator.generateForSagaStep(saga.getSagaId(), 998),
                        Constant.FIELD_SAGA_ID, saga.getSagaId(),
                        Constant.FIELD_TYPE, "CANCEL_ORDER_WITH_FENCING",
                        Constant.FIELD_PAYLOAD, payload,
                        "fencingToken", orderLockResult.getFencingToken(), // PHASE 3: Include fencing token
                        Constant.FIELD_TIMESTAMP, System.currentTimeMillis()
                );

                // PRESERVE EXISTING - Publish to order service
                messagePublisher.publishCommand(command, orderCommandsTopic, saga.getSagaId());

                log.info("Order compensation command with fencing token published: sagaId={}, token={}",
                        saga.getSagaId(), orderLockResult.getFencingToken());

            } finally {
                redisLockService.releaseLock(orderLockKey);
            }
        } else {
            log.error("Failed to acquire order lock for compensation: sagaId={}, orderId={}",
                    saga.getSagaId(), saga.getOrderId());
            throw new RuntimeException("Failed to acquire order lock for compensation");
        }
    }

    /**
     * PHASE 3: Enhanced process next step with fencing token support
     * Preserves existing step processing logic while adding fencing token protection
     */
    private void processNextStepWithFencing(OrderPurchaseSagaState saga) {
        if (saga.getCurrentStep() == null) {
            log.warn("No current step to process for saga: {}", saga.getSagaId());
            return;
        }

        log.info("Processing step with fencing token protection: step={}, sagaId={}",
                saga.getCurrentStep(), saga.getSagaId());

        try {
            // PHASE 3: Acquire appropriate lock with fencing token based on step type
            String lockKey;
            FencingLockResult lockResult;

            if (saga.getCurrentStep().getCommandType().name().contains("PAYMENT")) {
                lockKey = RedisLockService.buildPaymentLockKey(saga.getOrderId().toString());
                lockResult = redisLockService.tryLockWithFencing(lockKey, 1, TimeUnit.MINUTES);
            } else {
                lockKey = RedisLockService.buildOrderLockKey(saga.getOrderId().toString());
                lockResult = redisLockService.tryLockWithFencing(lockKey, 30, TimeUnit.SECONDS);
            }

            if (lockResult.isAcquired() && lockResult.isValid()) {
                try {
                    // PRESERVE EXISTING - Build command using existing logic
                    Map<String, Object> command = createCommandForCurrentStep(saga);

                    // PHASE 3: Add fencing token to command
                    command.put("fencingToken", lockResult.getFencingToken());

                    // PRESERVE EXISTING - Determine target topic
                    String targetTopic = getTopicForCommand(saga.getCurrentStep().getCommandType());

                    // PRESERVE EXISTING - Publish command
                    messagePublisher.publishSagaStepCommand(
                            saga.getSagaId(),
                            saga.getCurrentStep().getStepNumber(),
                            saga.getCurrentStep().getCommandType().name(),
                            command,
                            targetTopic
                    );

                    log.info("Step command with fencing token published: step={}, sagaId={}, token={}",
                            saga.getCurrentStep().getCommandType(), saga.getSagaId(), lockResult.getFencingToken());

                } finally {
                    redisLockService.releaseLock(lockKey);
                }
            } else {
                log.error("Failed to acquire lock for step processing: step={}, sagaId={}",
                        saga.getCurrentStep(), saga.getSagaId());
                handleStepFailure(saga, "Failed to acquire lock with fencing token for step processing");
            }

        } catch (Exception e) {
            log.error("Error processing step with fencing token: step={}, sagaId={}",
                    saga.getCurrentStep(), saga.getSagaId(), e);
            handleStepFailure(saga, "Failed to process step with fencing token: " + e.getMessage());
        }
    }


    /**
     * Handle successful completion of a compensation step
     */
    private void handleCompensationStepSuccess(OrderPurchaseSagaState saga) {
        OrderPurchaseSagaStep currentStep = saga.getCurrentStep();
        log.debug(Constant.LOG_COMPENSATION_STEP_COMPLETED, currentStep, saga.getSagaId());

        // Add compensation step to completed steps
        saga.getCompletedSteps().add(currentStep.name());

        // Get next compensation step
        OrderPurchaseSagaStep nextStep = currentStep.getNextCompensationStep();

        if (nextStep == OrderPurchaseSagaStep.COMPLETE_SAGA) {
            // Compensation completed
            saga.completeCompensation();
            monitoringService.recordSagaFailed(saga.getSagaId(), "Compensated: " + saga.getFailureReason());
        } else {
            // Move to next compensation step
            saga.setCurrentStep(nextStep);
            saga.setCurrentStepStartTime(Instant.now());
            saga.setLastUpdatedTime(Instant.now());
            saga.addEvent(SagaEvent.of(Constant.SAGA_EVENT_COMPENSATION_STEP,
                    "Moving to compensation step: " + nextStep.getDescription()));
        }
    }


    /**
     * Handle failed compensation step
     */
    private void handleCompensationStepFailure(OrderPurchaseSagaState saga, String reason) {
        log.warn(Constant.LOG_COMPENSATION_STEP_FAILED, saga.getSagaId(), reason);

        // Retry compensation or mark as compensation failed
        if (saga.getCompensationRetryCount() < saga.getMaxCompensationRetries()) {
            // Retry compensation step
            saga.incrementCompensationRetryCount();
            saga.addEvent(SagaEvent.of(Constant.SAGA_EVENT_COMPENSATION_RETRY,
                    "Retrying compensation step " + saga.getCurrentStep() + " (attempt " + saga.getCompensationRetryCount() + ")"));

            log.info(Constant.LOG_RETRYING_COMPENSATION,
                    saga.getSagaId(), saga.getCompensationRetryCount(), saga.getMaxCompensationRetries());

            sagaRepository.save(saga);

            // Retry the current compensation step
            processNextStepWithFencing(saga);

        } else {
            // Mark as compensation failed - manual intervention needed
            log.error(Constant.LOG_COMPENSATION_FAILED_FINAL,
                    saga.getSagaId(), saga.getMaxCompensationRetries());

            saga.setStatus(SagaStatus.COMPENSATION_FAILED);
            saga.setEndTime(Instant.now());
            saga.addEvent(SagaEvent.of(Constant.SAGA_EVENT_COMPENSATION_FAILED,
                    String.format(Constant.DESC_COMPENSATION_FAILED, saga.getMaxCompensationRetries(), reason)));

            sagaRepository.save(saga);

            // Record the compensation failure
            monitoringService.recordSagaFailed(saga.getSagaId(),
                    String.format(Constant.ERROR_COMPENSATION_FAILED, reason));
        }
    }



    /**
     * Complete saga successfully
     */
    private void completeSaga(OrderPurchaseSagaState saga) {
        log.info("Completing saga successfully: {}", saga.getSagaId());

        saga.setStatus(SagaStatus.COMPLETED);
        saga.setEndTime(Instant.now());
        saga.addEvent(SagaEvent.sagaCompleted());

        sagaRepository.save(saga);
        monitoringService.recordSagaCompleted(saga.getSagaId());

        // Clean up locks for completed saga
        cleanupCompletedSagaLocks(saga.getSagaId());
    }

    /**
     * Create command for the current step
     */
    private Map<String, Object> createCommandForCurrentStep(OrderPurchaseSagaState saga) {
        Map<String, Object> command = new HashMap<>();
        command.put(Constant.FIELD_SAGA_ID, saga.getSagaId());
        command.put(Constant.FIELD_MESSAGE_ID, UUID.randomUUID().toString());
        command.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());

        // Add step-specific payload
        switch (saga.getCurrentStep()) {
            case PROCESS_PAYMENT:
                command.put(Constant.FIELD_ORDER_ID, saga.getOrderId().toString());
                command.put(Constant.FIELD_USER_ID, saga.getUserId());
                command.put(Constant.FIELD_TOTAL_AMOUNT, saga.getTotalAmount());
                break;

            case UPDATE_ORDER_STATUS_CONFIRMED:
                command.put(Constant.FIELD_ORDER_ID, saga.getOrderId().toString());
                command.put(Constant.FIELD_NEW_STATUS, Constant.ORDER_STATUS_CONFIRMED);
                command.put(Constant.FIELD_REASON, Constant.REASON_PAYMENT_PROCESSED_SUCCESS);
                break;

            case UPDATE_ORDER_STATUS_DELIVERED:
                command.put(Constant.FIELD_ORDER_ID, saga.getOrderId().toString());
                command.put(Constant.FIELD_NEW_STATUS, Constant.ORDER_STATUS_DELIVERED);
                command.put(Constant.FIELD_REASON, Constant.REASON_ORDER_CONFIRMED_SUCCESS);
                break;

            case CANCEL_PAYMENT:
                command.put(Constant.FIELD_ORDER_ID, saga.getOrderId().toString());
                command.put(Constant.FIELD_PAYMENT_TRANSACTION_ID, saga.getPaymentTransactionId());
                command.put(Constant.FIELD_REASON, saga.getFailureReason());
                break;

            case CANCEL_ORDER:
                command.put(Constant.FIELD_ORDER_ID, saga.getOrderId().toString());
                command.put(Constant.FIELD_REASON, saga.getFailureReason());
                command.put(Constant.FIELD_CANCELLED_BY, Constant.ACTOR_SAGA_COMPENSATION);
                break;

            default:
                log.warn("No command mapping for step: {}", saga.getCurrentStep());
                return null;
        }

        return command;
    }

    /**
     * Get Kafka topic for command type
     */
    private String getTopicForCommand(CommandType commandType) {
        return switch (commandType) {
            case PAYMENT_PROCESS, PAYMENT_REVERSE -> paymentCommandsTopic;
            case ORDER_UPDATE_CONFIRMED, ORDER_UPDATE_DELIVERED, ORDER_CANCEL -> orderCommandsTopic;
            default -> "saga.dlq"; // Dead letter queue fallback
        };
    }

    /**
     * Check if event matches current saga step
     */
    private boolean isEventForCurrentStep(OrderPurchaseSagaState saga, String eventType) {
        if (saga.getCurrentStep() == null) {
            return false;
        }

        try {
            EventType event = EventType.valueOf(eventType);
            CommandType expectedCommandType = saga.getCurrentStep().getCommandType();
            CommandType eventCommandType = event.getAssociatedCommandType();

            return eventCommandType == expectedCommandType;
        } catch (Exception e) {
            log.error("Error checking event match for saga {}: {}", saga.getSagaId(), e.getMessage());
            return false;
        }
    }

    /**
     * Update saga with data from event
     */
    private void updateSagaWithEventData(OrderPurchaseSagaState saga, Map<String, Object> eventData) {
        String eventType = (String) eventData.get("type");

        // Extract specific data based on event type
        switch (eventType) {
            case "PAYMENT_PROCESSED":
                Object paymentTransactionId = eventData.get("paymentTransactionId");
                if (paymentTransactionId != null) {
                    saga.setPaymentTransactionId(Long.valueOf(paymentTransactionId.toString()));
                }
                break;
            // Add other event data extraction as needed
        }
    }

    /**
     * Record event processing for idempotency
     */
    private void recordEventProcessing(Map<String, Object> eventData, OrderPurchaseSagaState saga, String result) {
        String messageId = (String) eventData.get("messageId");
        String eventType = (String) eventData.get("type");
        Integer stepId = saga.getCurrentStep() != null ? saga.getCurrentStep().getStepNumber() : null;

        Map<String, Object> processingResult = new HashMap<>();
        processingResult.put("result", result);
        processingResult.put("sagaStatus", saga.getStatus().name());
        processingResult.put("currentStep", saga.getCurrentStep() != null ? saga.getCurrentStep().name() : "null");

        idempotencyService.recordProcessing(messageId, saga.getSagaId(), stepId, eventType, processingResult);
    }

    /**
     * PHASE 2 ENHANCEMENT: Check for timed-out sagas with DISTRIBUTED COORDINATION
     * Preserves existing timeout detection logic
     * Adds distributed locking to ensure only one instance processes each timeout
     */
    public void checkForTimeouts() {
        log.debug("Checking for timed-out saga steps with distributed coordination");

        // PRESERVE EXISTING - Use your proven timeout detection logic
        List<SagaStatus> activeStatuses = Arrays.asList(SagaStatus.STARTED, SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING);
        Instant cutoffTime = Instant.now().minus(Duration.ofMinutes(defaultTimeoutMinutes));

        List<OrderPurchaseSagaState> timedOutSagas = sagaRepository.findSagasWithStepTimeout(activeStatuses, cutoffTime);

        log.debug("Found {} potentially timed-out sagas", timedOutSagas.size());

        // PHASE 2: Process each timeout with distributed coordination
        for (OrderPurchaseSagaState saga : timedOutSagas) {
            processTimeoutWithDistributedLocking(saga);
        }
    }

    /**
     * PHASE 2: Process individual timeout with distributed locking
     * Returns true if this instance processed the timeout, false if another instance is handling it
     */
    private boolean processTimeoutWithDistributedLocking(OrderPurchaseSagaState saga) {
        String sagaLockKey = RedisLockService.buildSagaLockKey(saga.getSagaId());

        // Try to acquire lock with shorter timeout for batch processing
        if (redisLockService.tryLock(sagaLockKey, 30, TimeUnit.SECONDS)) {
            try {
                // Re-check saga status after acquiring lock (another instance might have processed it)
                Optional<OrderPurchaseSagaState> latestSaga = sagaRepository.findById(saga.getSagaId());
                if (latestSaga.isPresent() && latestSaga.get().getStatus().isActive()) {
                    // PRESERVE EXISTING - Use your proven handleSagaTimeout logic
                    handleSagaTimeout(latestSaga.get());
                    return true;
                } else {
                    log.debug("Saga {} was processed by another instance or is no longer active", saga.getSagaId());
                    return false;
                }
            } finally {
                redisLockService.releaseLock(sagaLockKey);
            }
        } else {
            log.debug("Another instance is processing timeout for saga: {}", saga.getSagaId());
            return false;
        }
    }


    /**
     * PHASE 2 ENHANCEMENT: Check for timed-out sagas with specific duration and DISTRIBUTED COORDINATION
     * Preserves existing timeout detection logic
     */
    public int checkForTimeoutsWithDuration(Duration timeout) {
        log.debug("Checking for timed-out saga steps with timeout: {} and distributed coordination", timeout);

        // PRESERVE EXISTING - Use your proven timeout detection logic
        List<SagaStatus> activeStatuses = Arrays.asList(SagaStatus.STARTED, SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING);
        Instant cutoffTime = Instant.now().minus(timeout);

        List<OrderPurchaseSagaState> timedOutSagas = sagaRepository.findSagasWithStepTimeout(activeStatuses, cutoffTime);

        log.debug("Found {} timed-out sagas with timeout {} and distributed coordination", timedOutSagas.size(), timeout);

        // PHASE 2: Process each timeout with distributed coordination
        int processedCount = 0;
        for (OrderPurchaseSagaState saga : timedOutSagas) {
            if (processTimeoutWithDistributedLocking(saga)) {
                processedCount++;
            }
        }

        log.debug("Processed {} timeouts with distributed coordination", processedCount);
        return processedCount;
    }

    /**
     * Handle a timed-out saga
     */
    private void handleSagaTimeout(OrderPurchaseSagaState saga) {
        log.warn("Saga step timed out: {}", saga.getSagaId());

        if (saga.getRetryCount() < saga.getMaxRetries()) {
            // Retry the step with exponential backoff delay
            saga.incrementRetryCount();
            saga.addEvent(SagaEvent.of("RETRY", "Retrying step " + saga.getCurrentStep() + " after timeout (attempt " + saga.getRetryCount() + ")"));
            sagaRepository.save(saga);

            // Apply exponential backoff delay
            scheduleRetryWithDelay(saga);
        } else {
            // Exceeded retries, start compensation
            handleStepFailure(saga, "Step timed out after " + saga.getMaxRetries() + " retries");
        }
    }

    /**
     * PHASE 2 ENHANCEMENT: Handle a timed-out saga manually with DISTRIBUTED LOCKING
     * Preserves all existing timeout and retry logic
     * Adds distributed coordination to prevent multiple instances from handling same timeout
     */
    @Transactional
    public void handleSagaTimeoutManually(String sagaId, String reason) {
        log.warn("Handling manual timeout for saga: {} - {}", sagaId, reason);

        // PHASE 2: Use distributed saga lock instead of ReentrantLock
        String sagaLockKey = RedisLockService.buildSagaLockKey(sagaId);

        if (acquireSagaLockWithRetry(sagaLockKey, sagaId)) {
            try {
                // PRESERVE ALL EXISTING LOGIC
                Optional<OrderPurchaseSagaState> optionalSaga = sagaRepository.findById(sagaId);
                if (optionalSaga.isPresent()) {
                    OrderPurchaseSagaState saga = optionalSaga.get();

                    // Only handle timeout if saga is still active
                    if (saga.getStatus().isActive()) {
                        // PRESERVE EXISTING - Use your proven handleSagaTimeout logic
                        handleSagaTimeout(saga);
                    } else {
                        log.info("Saga {} is no longer active, skipping timeout handling", sagaId);
                    }
                } else {
                    log.warn("Saga {} not found for manual timeout handling", sagaId);
                }
            } finally {
                // PHASE 2: Release distributed saga lock
                boolean released = redisLockService.releaseLock(sagaLockKey);
                log.debug("Distributed saga lock released after timeout handling: sagaId={}, released={}",
                        sagaId, released);
            }
        } else {
            // Another instance is likely handling this timeout
            log.info("Could not acquire saga lock for timeout handling, another instance may be processing: sagaId={}",
                    sagaId);
        }
    }


    /**
     * PHASE 2 ENHANCEMENT: Schedule a retry with exponential backoff delay using DISTRIBUTED LOCKING
     * Preserves all your excellent retry logic while making it distributed-safe
     */
    private void scheduleRetryWithDelay(OrderPurchaseSagaState saga) {
        // PRESERVE EXISTING - Use your proven retry delay calculation
        long delayMs = calculateRetryDelay(saga.getRetryCount());

        log.info("Scheduling retry for saga {} with delay of {}ms (attempt {}) - distributed safe",
                saga.getSagaId(), delayMs, saga.getRetryCount());

        // PRESERVE EXISTING - Use CompletableFuture for async delay
        java.util.concurrent.CompletableFuture.delayedExecutor(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> {
                    // PHASE 2: Add distributed locking to retry execution
                    executeDelayedRetryWithDistributedLocking(saga.getSagaId());
                });
    }

    /**
     * PHASE 2: Execute delayed retry with distributed saga locking
     * Ensures only one instance executes the retry even in distributed environment
     */
    private void executeDelayedRetryWithDistributedLocking(String sagaId) {
        String sagaLockKey = RedisLockService.buildSagaLockKey(sagaId);

        if (redisLockService.tryLock(sagaLockKey, 2, TimeUnit.MINUTES)) {
            try {
                // PRESERVE EXISTING - Reload saga to ensure we have the latest state
                Optional<OrderPurchaseSagaState> optionalSaga = sagaRepository.findById(sagaId);
                if (optionalSaga.isPresent()) {
                    OrderPurchaseSagaState currentSaga = optionalSaga.get();

                    // PRESERVE EXISTING - Only retry if still in a retryable state
                    if (currentSaga.getStatus().isActive() && currentSaga.getRetryCount() <= currentSaga.getMaxRetries()) {
                        log.info("Executing delayed retry for saga {} (attempt {}) with distributed coordination",
                                currentSaga.getSagaId(), currentSaga.getRetryCount());

                        // PRESERVE EXISTING
                        processNextStepWithFencing(currentSaga);
                    } else {
                        log.warn("Saga {} state changed during retry delay, skipping retry", sagaId);
                    }
                } else {
                    log.warn("Saga {} not found during delayed retry", sagaId);
                }
            } catch (Exception e) {
                log.error("Error during delayed retry for saga {}", sagaId, e);
            } finally {
                redisLockService.releaseLock(sagaLockKey);
            }
        } else {
            log.debug("Another instance is processing retry for saga: {}", sagaId);
        }
    }

    /**
     * Calculate retry delay using exponential backoff
     * Base delay * (2^(retryCount-1)) with jitter
     */
    private long calculateRetryDelay(int retryCount) {
        // Base delay in milliseconds
        long baseDelayMs = baseRetryDelaySeconds * 1000L;

        // Exponential backoff: base * 2^(retryCount-1)
        long exponentialDelay = baseDelayMs * (1L << (retryCount - 1));

        // Add jitter (±20% random variation)
        double jitter = 0.8 + (Math.random() * 0.4); // 0.8 to 1.2
        long delayWithJitter = Math.round(exponentialDelay * jitter);

        // Cap at maximum delay (60 seconds)
        long maxDelayMs = 60 * 1000L;
        return Math.min(delayWithJitter, maxDelayMs);
    }

    /**
     * Get or create a saga-specific lock for synchronization
     */
    private ReentrantLock getSagaLock(String sagaId) {
        return sagaLocks.computeIfAbsent(sagaId, k -> new ReentrantLock());
    }

    /**
     * PHASE 2: Clean up completed saga locks to prevent memory leaks
     * Enhanced version of existing cleanupCompletedSagaLocks for distributed environment
     */
    private void cleanupCompletedSagaLocks(String sagaId) {
        // PRESERVE EXISTING - Remove from local saga lock map
        sagaLocks.remove(sagaId);

        // PHASE 2 NEW - Also clean up distributed saga lock if it exists
        String sagaLockKey = RedisLockService.buildSagaLockKey(sagaId);
        try {
            // Only release if we own it (this prevents cleaning up locks held by other instances)
            redisLockService.releaseLock(sagaLockKey);
            log.debug("Cleaned up distributed saga lock for completed saga: {}", sagaId);
        } catch (Exception e) {
            log.debug("Distributed saga lock cleanup attempted but not owned by this instance: {}", sagaId);
        }
    }

    // Repository access methods
    public Optional<OrderPurchaseSagaState> findById(String sagaId) {
        return sagaRepository.findById(sagaId);
    }

    public Optional<OrderPurchaseSagaState> findByOrderId(Long orderId) {
        return sagaRepository.findByOrderId(orderId);
    }

    public List<OrderPurchaseSagaState> findByUserId(String userId) {
        return sagaRepository.findByUserId(userId);
    }

    public List<OrderPurchaseSagaState> findActiveSagas() {
        return sagaRepository.findActiveSagas();
    }

    /**
     * PHASE 3 ENHANCEMENT: Cancel saga by user request with FENCING TOKENS
     * Provides bulletproof protection against split-brain scenarios
     * Preserves all existing compensation logic while eliminating race conditions
     */
    @Transactional
    public boolean cancelSagaByUserWithFencing(String sagaId, String orderId, String reason) {
        log.info("Processing user cancellation request with fencing tokens: sagaId={}, orderId={}, reason={}",
                sagaId, orderId, reason);

        // PHASE 3: Try to acquire payment lock with fencing token
        String paymentLockKey = RedisLockService.buildPaymentLockKey(orderId);

        log.debug("Attempting to acquire payment lock with fencing token for cancellation: lockKey={}", paymentLockKey);

        FencingLockResult paymentLockResult = redisLockService.tryLockWithFencing(paymentLockKey, 30, TimeUnit.SECONDS);

        if (paymentLockResult.isAcquired() && paymentLockResult.isValid()) {
            try {
                log.info("Payment lock with fencing token acquired for cancellation: sagaId={}, orderId={}, token={}",
                        sagaId, orderId, paymentLockResult.getFencingToken());

                // PHASE 3: Acquire distributed saga lock with fencing token
                String sagaLockKey = RedisLockService.buildSagaLockKey(sagaId);
                FencingLockResult sagaLockResult = redisLockService.tryLockWithFencing(sagaLockKey, 2, TimeUnit.MINUTES);

                if (sagaLockResult.isAcquired() && sagaLockResult.isValid()) {
                    try {
                        // PRESERVE EXISTING - Validate saga exists and is in active state
                        Optional<OrderPurchaseSagaState> optionalSaga = sagaRepository.findById(sagaId);
                        if (optionalSaga.isEmpty()) {
                            log.warn("Saga not found for cancellation: sagaId={}", sagaId);
                            return false;
                        }

                        OrderPurchaseSagaState saga = optionalSaga.get();

                        // Validate saga is in active state
                        if (saga.getStatus() == SagaStatus.COMPLETED || saga.getStatus() == SagaStatus.FAILED) {
                            log.warn("Cannot cancel saga in final state: sagaId={}, status={}", sagaId, saga.getStatus());
                            return false;
                        }

                        log.info("Proceeding with saga cancellation - all locks secured with fencing tokens: sagaId={}", sagaId);

                        // PHASE 3: Set failure reason and add fencing token info
                        saga.setFailureReason("User cancellation with fencing tokens: " + reason);
                        saga.addEvent(SagaEvent.of("USER_CANCELLATION_REQUESTED_WITH_FENCING",
                                "User requested cancellation with fencing protection: " + reason +
                                        ", paymentToken=" + paymentLockResult.getFencingToken() +
                                        ", sagaToken=" + sagaLockResult.getFencingToken()));

                        // PHASE 3: Update saga with fencing token
                        saga.setFencingToken(Long.valueOf(sagaLockResult.getFencingToken()));

                        // PRESERVE EXISTING - Use existing compensation strategy
                        startCompensationWithFencing(saga, paymentLockResult.getFencingToken());

                        log.info("User cancellation with fencing tokens initiated successfully: sagaId={}, orderId={}",
                                sagaId, orderId);
                        return true;

                    } finally {
                        redisLockService.releaseLock(sagaLockKey);
                    }
                } else {
                    log.warn("Failed to acquire saga lock with fencing token for cancellation: sagaId={}", sagaId);
                    return false;
                }

            } finally {
                // Always release payment lock
                boolean released = redisLockService.releaseLock(paymentLockKey);
                log.info("Payment lock released after cancellation attempt: sagaId={}, orderId={}, released={}",
                        sagaId, orderId, released);
            }
        } else {
            // Failed to acquire payment lock - payment is currently in progress
            String lockHolder = redisLockService.getLockHolder(paymentLockKey);
            log.warn("Cannot cancel saga - payment in progress (lock held by: {}): sagaId={}, orderId={}",
                    lockHolder, sagaId, orderId);

            return false;
        }
    }

    @Scheduled(fixedRateString = "${saga.lock.health-check.interval-seconds:60}000")
    public void performSagaLockHealthCheck() {
        if (!lockMonitoringEnabled) {
            return;
        }

        try {
            // Get locks held by this instance
            Set<String> heldLocks = redisLockService.getLocksHeldByThisInstance();

            log.debug("Saga lock health check: {} distributed locks held by this instance", heldLocks.size());

            // Check for potential orphaned locks (locks held longer than expected)
            long staleThresholdMs = Duration.ofMinutes(defaultTimeoutMinutes * 2).toMillis();
            int staleCount = 0;

            for (String lockKey : heldLocks) {
                if (lockKey.contains("saga:lock:saga:")) {
                    String sagaId = extractSagaIdFromLockKey(lockKey);
                    if (isSagaLockPotentiallyStale(sagaId, staleThresholdMs)) {
                        staleCount++;
                        log.warn("Potentially stale saga lock detected: sagaId={}, lockKey={}", sagaId, lockKey);
                    }
                }
            }

            if (staleCount > 0) {
                log.warn("Found {} potentially stale saga locks - consider investigating", staleCount);
            }

        } catch (Exception e) {
            log.error("Error during saga lock health check", e);
        }
    }

    /**
     * PHASE 2: Graceful shutdown - release all locks held by this instance
     * Implements proper cleanup for distributed environment
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down saga service - releasing all distributed locks");

        try {
            // Release all distributed locks held by this instance
            redisLockService.releaseAllLocksForInstance();

            log.info("Saga service shutdown completed - all locks released");

        } catch (Exception e) {
            log.error("Error during saga service shutdown", e);
        }
    }

    /**
     * PHASE 2: Extract saga ID from distributed lock key
     */
    private String extractSagaIdFromLockKey(String lockKey) {
        // Lock key format: "saga:lock:saga:{sagaId}"
        String[] parts = lockKey.split(":");
        return parts.length >= 4 ? parts[3] : null;
    }

    /**
     * PHASE 2: Check if a saga lock might be stale
     */
    private boolean isSagaLockPotentiallyStale(String sagaId, long staleThresholdMs) {
        try {
            Optional<OrderPurchaseSagaState> saga = sagaRepository.findById(sagaId);
            if (saga.isEmpty()) {
                return true; // Saga doesn't exist, lock is definitely stale
            }

            OrderPurchaseSagaState sagaState = saga.get();

            // If saga is completed or failed, lock shouldn't exist
            if (!sagaState.getStatus().isActive()) {
                return true;
            }

            // Check if saga has been in current step too long
            if (sagaState.getLastUpdatedTime() != null) {
                long ageMs = System.currentTimeMillis() - sagaState.getLastUpdatedTime().toEpochMilli();
                return ageMs > staleThresholdMs;
            }

            return false;

        } catch (Exception e) {
            log.debug("Error checking saga staleness for {}: {}", sagaId, e.getMessage());
            return false;
        }
    }


}