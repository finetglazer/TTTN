package com.graduation.paymentservice.service;

import com.graduation.paymentservice.constant.Constant;
import com.graduation.paymentservice.model.PaymentTransaction;
import com.graduation.paymentservice.model.ProcessedMessage;
import com.graduation.paymentservice.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    /**
     * Handle process payment command
     * Mock implementation that randomly succeeds or fails
     * Step: idempotency check -> logic solving -> record processing -> publish event
     */
    @Transactional
    public void handleProcessPayment(Map<String, Object> command) {
        String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);
        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);

        // Check if the command has already been processed
        if (idempotencyService.isProcessed(messageId, sagaId)) {
            log.info(Constant.LOG_COMMAND_ALREADY_PROCESSED, sagaId, messageId);
            return; // Skip processing if already handled
        }

        // Extract the payload which contains the actual command data
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

        // Validate above fields from the payload
        if (orderId == null || orderId.isEmpty() || userId == null || userId.isEmpty() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(Constant.LOG_INVALID_PAYMENT_COMMAND_DATA, sagaId, orderId, userId, amount);
            publishPaymentEvent(sagaId, orderId, null,
                    Constant.EVENT_PAYMENT_FAILED, false,
                    null, Constant.ERROR_INVALID_PAYMENT_DATA);
            return;
        }

        log.info(Constant.LOG_PROCESSING_PAYMENT_ORDER, orderId, amount, sagaId);

        try {
            // Create payment transaction
            PaymentTransaction transaction = PaymentTransaction.createForSaga(
                    orderId, userId, amount, sagaId, paymentMethod);

            // Process payment (mock implementation)
            transaction.processPayment();

            // Save transaction
            PaymentTransaction savedTransaction = paymentRepository.save(transaction);

            // Publish event based on payment result
            if (savedTransaction.getStatus().isSuccessful()) {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
                publishPaymentEvent(sagaId, orderId, savedTransaction.getId(),
                        Constant.EVENT_PAYMENT_PROCESSED, true,
                        Constant.PAYMENT_PROCESSED_SUCCESS, null);

            } else {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
                String reason = savedTransaction.getMockDecisionReason() != null
                        ? savedTransaction.getMockDecisionReason()
                        : Constant.REASON_PAYMENT_DECLINED;

                publishPaymentEvent(sagaId, orderId, savedTransaction.getId(),
                        Constant.EVENT_PAYMENT_FAILED, false,
                        null, reason);
            }

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_PROCESSING_PAYMENT_COMMAND, e.getMessage(), e);
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);

            // Publish failure event
            publishPaymentEvent(sagaId, orderId, null,
                    Constant.EVENT_PAYMENT_FAILED, false,
                    null, Constant.ERROR_TECHNICAL_PREFIX + e.getMessage());
        }
    }

    /**
     * Handle reverse payment command
     * Simple mock implementation that randomly succeeds or fails
     */
    @Transactional
    public void handleReversePayment(Map<String, Object> command) {
        // Processed message check
        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);
        String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);

        if (idempotencyService.isProcessed(messageId, sagaId)) {
            log.info(Constant.LOG_COMMAND_ALREADY_PROCESSED, sagaId, messageId);
            return; // Skip processing if already handled
        }

        Map<String, Object> payload = (Map<String, Object>) command.get(Constant.FIELD_PAYLOAD);
        String orderId = (String) payload.get(Constant.FIELD_ORDER_ID);
        String reason = (String) payload.get(Constant.FIELD_REASON);
        String paymentTransactionId = (String) payload.get(Constant.FIELD_PAYMENT_TRANSACTION_ID);

        // Check if payload is valid
        if (orderId == null || orderId.isEmpty() || paymentTransactionId == null || paymentTransactionId.isEmpty() || reason == null || reason.isEmpty()) {
            log.error(Constant.LOG_INVALID_REVERSE_PAYMENT_DATA, sagaId, orderId, paymentTransactionId);
            publishPaymentEvent(sagaId, orderId, null,
                    Constant.EVENT_PAYMENT_REVERSE_FAILED, false,
                    null, Constant.ERROR_INVALID_REVERSE_DATA);
            return;
        }

        log.info(Constant.LOG_REVERSING_PAYMENT_ORDER, orderId, sagaId);

        try {
            // Simple mock - randomly succeed or fail
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
     * Publish payment event back to saga orchestrator
     */
    private void publishPaymentEvent(String sagaId, String orderId, Long paymentTransactionId,
                                     String eventType, boolean success,
                                     String successMessage, String errorMessage) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put(Constant.FIELD_MESSAGE_ID, Constant.PREFIX_PAYMENT_MESSAGE + System.currentTimeMillis() + "_" +
                    UUID.randomUUID().toString().substring(0, Constant.UUID_SUBSTRING_LENGTH));
            event.put(Constant.FIELD_SAGA_ID, sagaId);
            event.put(Constant.FIELD_TYPE, eventType);
            event.put(Constant.FIELD_SUCCESS, success);
            event.put(Constant.FIELD_ORDER_ID, orderId);

            if (paymentTransactionId != null) {
                event.put(Constant.FIELD_PAYMENT_TRANSACTION_ID, paymentTransactionId);
            }

            event.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());

            if (success) {
                event.put(Constant.RESPONSE_MESSAGE, successMessage);
            } else {
                event.put(Constant.FIELD_ERROR_MESSAGE, errorMessage);
            }

            log.info(Constant.LOG_PUBLISHING_PAYMENT_EVENT, eventType, success, sagaId);

            kafkaTemplate.send(Constant.TOPIC_PAYMENT_EVENTS, sagaId, event);

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_PUBLISHING_EVENT, e.getMessage(), e);
        }
    }
}