package com.graduation.paymentservice.service;

import com.graduation.paymentservice.constant.Constant;
import com.graduation.paymentservice.model.FencingLockResult;
import com.graduation.paymentservice.model.PaymentStatus;
import com.graduation.paymentservice.model.PaymentTransaction;
import com.graduation.paymentservice.model.ProcessedMessage;
import com.graduation.paymentservice.repository.PaymentTransactionRepository;
import com.graduation.paymentservice.service.RedisLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling payment commands from the Saga Orchestrator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCommandHandlerService {

    private final PaymentTransactionRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IdempotencyService idempotencyService;
    private final RedisLockService redisLockService;

    /**
     * Handle process payment command WITH DISTRIBUTED LOCKING
     * Mock implementation that randomly succeeds or fails
     * Step: idempotency check -> acquire lock -> logic solving -> record processing -> publish event -> release lock
     */
    @Transactional
    public void handleProcessPayment(Map<String, Object> command) {
        String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);
        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);

        // PRESERVE EXISTING - Check if the command has already been processed
        if (idempotencyService.isProcessed(messageId, sagaId)) {
            log.info(Constant.LOG_COMMAND_ALREADY_PROCESSED, sagaId, messageId);
            return;
        }

        // PRESERVE EXISTING - Extract and validate payload
        Map<String, Object> payload = (Map<String, Object>) command.get(Constant.FIELD_PAYLOAD);
        if (payload == null) {
            log.error(Constant.LOG_COMMAND_PAYLOAD_NULL, sagaId);
            publishPaymentEvent(sagaId, null, null,
                    Constant.EVENT_PAYMENT_FAILED, false,
                    null, Constant.ERROR_INVALID_COMMAND_FORMAT);
            return;
        }

        String orderId = (String) payload.get(Constant.FIELD_ORDER_ID);
        String userId = (String) payload.get(Constant.FIELD_USER_ID);
        BigDecimal amount = new BigDecimal(payload.get(Constant.FIELD_TOTAL_AMOUNT).toString());
        String paymentMethod = (String) payload.getOrDefault(Constant.FIELD_PAYMENT_METHOD, Constant.DEFAULT_PAYMENT_METHOD);

        // PRESERVE EXISTING - Validate payload fields
        if (orderId == null || orderId.isEmpty() || userId == null || userId.isEmpty() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(Constant.LOG_INVALID_PAYMENT_COMMAND_DATA, sagaId, orderId, userId, amount);
            publishPaymentEvent(sagaId, orderId, null,
                    Constant.EVENT_PAYMENT_FAILED, false,
                    null, Constant.ERROR_INVALID_PAYMENT_DATA);
            return;
        }

        // PHASE 3: Acquire distributed lock WITH fencing token
        String lockKey = RedisLockService.buildPaymentLockKey(orderId);

        log.info("Attempting to acquire payment lock with fencing token: orderId={}, sagaId={}, lockKey={}",
                orderId, sagaId, lockKey);

        FencingLockResult lockResult = redisLockService.acquireLockWithFencing(lockKey, 1, TimeUnit.MINUTES);

        if (lockResult.isAcquired() && lockResult.isValid()) {
            try {
                log.info("Payment lock acquired with fencing token: orderId={}, sagaId={}, token={}",
                        orderId, sagaId, lockResult.getFencingToken());

                // PHASE 3: Process payment with fencing token validation
                processPaymentLogic(sagaId, messageId, orderId, userId, amount, paymentMethod,
                        lockResult.getFencingToken());

            } finally {
                // Always release lock
                boolean released = redisLockService.releaseLock(lockKey);
                log.info("Payment lock released: orderId={}, sagaId={}, released={}", orderId, sagaId, released);
            }
        } else {
            // Lock acquisition failed - payment already in progress or fencing token issue
            String lockHolder = redisLockService.getLockHolder(lockKey);
            String errorMessage = String.format(
                    "Payment processing already in progress for order %s. Lock held by: %s",
                    orderId, lockHolder);

            log.warn("Payment lock acquisition failed: orderId={}, sagaId={}, lockHolder={}", orderId, sagaId, lockHolder);

            // Record processing failure and publish event
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            publishPaymentEvent(sagaId, orderId, null, Constant.EVENT_PAYMENT_FAILED, false,
                    null, errorMessage);
        }
    }

    /**
     * PHASE 3: Enhanced payment processing logic with fencing token validation
     * Preserves all existing business logic while adding split-brain protection
     */
    private void processPaymentLogic(String sagaId, String messageId, String orderId,
                                                String userId, BigDecimal amount, String paymentMethod,
                                                String fencingToken) {

        log.info("Processing payment with fencing token: orderId={}, amount={}, sagaId={}, token={}",
                orderId, amount, sagaId, fencingToken);

        try {
            // PRESERVE EXISTING - Create payment transaction
            PaymentTransaction transaction = PaymentTransaction.createForSaga(
                    orderId, userId, amount, sagaId, paymentMethod);

            // PHASE 3: Process payment with fencing token validation
            boolean processedSuccessfully = transaction.processPaymentWithFencing(Long.valueOf(fencingToken));

            if (!processedSuccessfully) {
                // Fencing token validation failed
                log.error("Payment processing failed due to stale fencing token: orderId={}, token={}",
                        orderId, fencingToken);

                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
                publishPaymentEvent(sagaId, orderId, null,
                        Constant.EVENT_PAYMENT_FAILED, false,
                        null, "Payment rejected due to stale operation");
                return;
            }

            // PHASE 3: Validate fencing token with Redis before saving
            String resourceTokenKey = RedisLockService.buildPaymentResourceTokenKey(orderId);
            if (!redisLockService.validateFencingToken(resourceTokenKey, fencingToken)) {
                log.error("Redis fencing token validation failed: orderId={}, token={}", orderId, fencingToken);

                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
                publishPaymentEvent(sagaId, orderId, null,
                        Constant.EVENT_PAYMENT_FAILED, false,
                        null, "Payment rejected due to stale fencing token");
                return;
            }

            // PRESERVE EXISTING - Save transaction (now with fencing token)
            PaymentTransaction savedTransaction = paymentRepository.save(transaction);

            // PRESERVE EXISTING - Publish event based on payment result
            if (savedTransaction.getStatus().isSuccessful()) {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
                publishPaymentEvent(sagaId, orderId, savedTransaction.getId(),
                        Constant.EVENT_PAYMENT_PROCESSED, true,
                        "Payment processed successfully with fencing token: " + fencingToken, null);

            } else {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
                String reason = savedTransaction.getMockDecisionReason() != null
                        ? savedTransaction.getMockDecisionReason()
                        : "Payment processing failed";

                publishPaymentEvent(sagaId, orderId, savedTransaction.getId(),
                        Constant.EVENT_PAYMENT_FAILED, false,
                        null, reason);
            }

        } catch (Exception e) {
            log.error("Error during payment processing with fencing token: orderId={}, sagaId={}, token={}",
                    orderId, sagaId, fencingToken, e);

            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            publishPaymentEvent(sagaId, orderId, null,
                    Constant.EVENT_PAYMENT_FAILED, false,
                    null, "Payment processing error: " + e.getMessage());
        }
    }


    /**
     * PHASE 3: Handle reverse payment command with fencing token validation
     * Preserves existing compensation logic while adding split-brain protection
     */
    @Transactional
    public void handleReversePayment(Map<String, Object> command) {
        String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);
        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);

        // PRESERVE EXISTING - Check idempotency
        if (idempotencyService.isProcessed(messageId, sagaId)) {
            log.info("Reverse payment command already processed: sagaId={}, messageId={}", sagaId, messageId);
            return;
        }

        // PRESERVE EXISTING - Extract payload
        Map<String, Object> payload = (Map<String, Object>) command.get(Constant.FIELD_PAYLOAD);
        String orderId = (String) payload.get(Constant.FIELD_ORDER_ID);
        String reason = (String) payload.getOrDefault(Constant.FIELD_REASON, "Saga compensation");

        log.info("Processing reverse payment with fencing token: orderId={}, sagaId={}", orderId, sagaId);

        // PHASE 3: Acquire lock with fencing token for compensation
        String lockKey = RedisLockService.buildPaymentLockKey(orderId);
        FencingLockResult lockResult = redisLockService.acquireLockWithFencing(lockKey, 1, TimeUnit.MINUTES);

        if (lockResult.isAcquired() && lockResult.isValid()) {
            try {
                // Find the payment transaction to reverse
                Optional<PaymentTransaction> optionalTransaction = paymentRepository.findByOrderId(orderId);

                if (optionalTransaction.isPresent()) {
                    PaymentTransaction transaction = optionalTransaction.get();

                    // PHASE 3: Validate fencing token before reversal
                    String resourceTokenKey = RedisLockService.buildPaymentResourceTokenKey(orderId);
                    if (!redisLockService.validateFencingToken(resourceTokenKey, lockResult.getFencingToken())) {
                        log.error("Payment reversal rejected due to stale fencing token: orderId={}, token={}",
                                orderId, lockResult.getFencingToken());

                        publishPaymentEvent(sagaId, orderId, transaction.getId(),
                                Constant.EVENT_PAYMENT_REVERSED, false,
                                null, "Payment reversal rejected due to stale operation");
                        return;
                    }

                    // PHASE 3: Reverse payment with fencing token
                    boolean reversalSuccessful = transaction.updateStatusWithFencing(
                            PaymentStatus.REVERSED, reason, Long.valueOf(lockResult.getFencingToken()));

                    if (reversalSuccessful) {
                        paymentRepository.save(transaction);

                        idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
                        publishPaymentEvent(sagaId, orderId, transaction.getId(),
                                Constant.EVENT_PAYMENT_REVERSED, true,
                                "Payment reversed successfully with fencing token: " + lockResult.getFencingToken(), null);
                    } else {
                        log.error("Payment reversal failed due to fencing token validation: orderId={}", orderId);
                        publishPaymentEvent(sagaId, orderId, transaction.getId(),
                                Constant.EVENT_PAYMENT_REVERSED, false,
                                null, "Payment reversal failed due to stale operation");
                    }
                } else {
                    log.warn("Payment transaction not found for reversal: orderId={}", orderId);
                    publishPaymentEvent(sagaId, orderId, null,
                            Constant.EVENT_PAYMENT_REVERSED, false,
                            null, "Payment transaction not found");
                }

            } finally {
                redisLockService.releaseLock(lockKey);
            }
        } else {
            log.warn("Failed to acquire lock for payment reversal: orderId={}", orderId);
            publishPaymentEvent(sagaId, orderId, null,
                    Constant.EVENT_PAYMENT_REVERSED, false,
                    null, "Failed to acquire lock for payment reversal");
        }
    }

    /**
     * PRESERVE EXISTING: Extracted payment reversal logic (all existing logic preserved)
     */
    private void processPaymentReversalLogic(String sagaId, String messageId, String orderId, String reason) {

        log.info(Constant.LOG_REVERSING_PAYMENT_ORDER, orderId, sagaId);

        try {
            // PRESERVE EXISTING: Simple mock - randomly succeed or fail
            boolean success = Math.random() > 0.2; // 80% success rate

            if (success) {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
                publishPaymentEvent(sagaId, orderId, null,
                        Constant.EVENT_PAYMENT_REVERSED, true,
                        Constant.PAYMENT_REVERSED_SUCCESS, null);

            } else {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
                publishPaymentEvent(sagaId, orderId, null,
                        Constant.EVENT_PAYMENT_REVERSE_FAILED, false,
                        null, Constant.REASON_PAYMENT_FAILED);
            }

        } catch (Exception e) {
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            log.error(Constant.LOG_ERROR_PROCESSING_PAYMENT_COMMAND, e.getMessage(), e);
            publishPaymentEvent(sagaId, orderId, null,
                    Constant.EVENT_PAYMENT_REVERSE_FAILED, false,
                    null, Constant.ERROR_TECHNICAL_PREFIX + e.getMessage());
        }
    }

    /**
     * PRESERVE EXISTING: Publish payment event back to saga orchestrator
     * (Keep all existing logic exactly as is)
     */
    private void publishPaymentEvent(String sagaId, String orderId, Long paymentTransactionId,
                                     String eventType, boolean success, String successMessage, String errorMessage) {
        try {
            // TODO: Inject KafkaTemplate when available
            // For now, just log the event that would be published

            log.info(Constant.LOG_PUBLISHING_PAYMENT_EVENT,
                    sagaId, orderId, eventType, success);

            if (success) {
                log.info(Constant.EVENT_PAYMENT_PROCESSED, successMessage);
            } else {
                log.error(Constant.EVENT_PAYMENT_FAILED, errorMessage);
            }

            Map<String, Object> event = new HashMap<>();
            event.put(Constant.FIELD_MESSAGE_ID, Constant.PREFIX_PAYMENT_MESSAGE + System.currentTimeMillis() + "_" +
                    UUID.randomUUID().toString().substring(0, Constant.UUID_SUBSTRING_LENGTH));
            event.put(Constant.FIELD_SAGA_ID, sagaId);
            event.put(Constant.FIELD_TYPE, eventType);
            event.put(Constant.FIELD_SUCCESS, success);
            event.put(Constant.FIELD_ORDER_ID, orderId);
            event.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());

            if (paymentTransactionId != null) {
                event.put(Constant.FIELD_PAYMENT_TRANSACTION_ID, paymentTransactionId);
            }

            if (success) {
                event.put(Constant.RESPONSE_MESSAGE, successMessage);
            } else {
                event.put(Constant.FIELD_ERROR_MESSAGE, errorMessage);
            }

            kafkaTemplate.send(Constant.TOPIC_PAYMENT_EVENTS, sagaId, event);

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_PUBLISHING_EVENT, e.getMessage(), e);
        }
    }

    /**
     * PRESERVE EXISTING: Generate unique message ID
     */
    private String generateMessageId() {
        return Constant.PREFIX_PAYMENT_MESSAGE + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);
    }
}