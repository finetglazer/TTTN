package com.graduation.paymentservice.service;

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
        String sagaId = (String) command.get("sagaId");
        String messageId = (String) command.get("messageId");

        // Check if the command has already been processed
        if (idempotencyService.isProcessed(messageId, sagaId)) {
            log.info("Command already processed: sagaId={}, messageId={}", sagaId, messageId);
            return; // Skip processing if already handled
        }

        // Extract the payload which contains the actual command data
        Map<String, Object> payload = (Map<String, Object>) command.get("payload");
        if (payload == null) {
            log.error("Command payload is null for sagaId: {}", sagaId);
            publishPaymentEvent(sagaId, null, null,
                    "PAYMENT_FAILED", false,
                    null, "Invalid command format: missing payload");
            return;
        }

        String orderId = (String) payload.get("orderId");
        String userId = (String) payload.get("userId");
        BigDecimal amount = new BigDecimal(payload.get("totalAmount").toString());
        String paymentMethod = (String) payload.getOrDefault("paymentMethod", "CREDIT_CARD");

        // validate above fields from the payload

        if (orderId == null || orderId.isEmpty() || userId == null || userId.isEmpty() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid payment command data for sagaId: {}, orderId: {}, userId: {}, amount: {}",
                    sagaId, orderId, userId, amount);
            publishPaymentEvent(sagaId, orderId, null,
                    "PAYMENT_FAILED", false,
                    null, "Invalid payment command data");
            return;
        }

        log.info("Processing payment for order: {}, amount: {}, sagaId: {}",
                orderId, amount, sagaId);

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
                        "PAYMENT_PROCESSED", true,
                        "Payment processed successfully", null);

            } else {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
                String reason = savedTransaction.getMockDecisionReason() != null
                        ? savedTransaction.getMockDecisionReason()
                        : "Payment declined";

                publishPaymentEvent(sagaId, orderId, savedTransaction.getId(),
                        "PAYMENT_FAILED", false,
                        null, reason);

            }


        } catch (Exception e) {
            log.error("Error processing payment for order: {}", orderId, e);
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);

            // Publish failure event
            publishPaymentEvent(sagaId, orderId, null,
                    "PAYMENT_FAILED", false,
                    null, "Technical error: "  + e.getMessage());

        }
    }

    /**
     * Handle reverse payment command
     * Simple mock implementation that randomly succeeds or fails
     */
    @Transactional
    public void handleReversePayment(Map<String, Object> command) {
        // Processed message check
        String messageId = (String) command.get("messageId");
        String sagaId = (String) command.get("sagaId");
        if(idempotencyService.isProcessed(messageId, sagaId)) {
            log.info("Reverse payment command already processed: sagaId={}, messageId={}", sagaId, messageId);
            return; // Skip processing if already handled
        }



        Map<String, Object> payload = (Map<String, Object>) command.get("payload");
        String orderId = (String) payload.get("orderId");
        String reason = (String) payload.get("reason");
        String paymentTransactionId = (String) payload.get("paymentTransactionId");

        // Check if payload is valid
        if (orderId == null || orderId.isEmpty() || paymentTransactionId == null || paymentTransactionId.isEmpty() || reason == null || reason.isEmpty()) {
            log.error("Invalid reverse payment command data for sagaId: {}, orderId: {}, paymentTransactionId: {}",
                    sagaId, orderId, paymentTransactionId);
            publishPaymentEvent(sagaId, orderId, null,
                    "PAYMENT_REVERSE_FAILED", false,
                    null, "Invalid reverse payment command data");
            return;
        }

        log.info("Reversing payment for order: {}, sagaId: {}", orderId, sagaId);

        try {
            // Simple mock - randomly succeed or fail
            boolean success = Math.random() > 0.2; // 80% success rate

            if (success) {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
                publishPaymentEvent(sagaId, orderId, null,
                        "PAYMENT_REVERSED", true,
                        "Payment reversed successfully", null);

            } else {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
                publishPaymentEvent(sagaId, orderId, null,
                        "PAYMENT_REVERSE_FAILED", false,
                        null, "Payment reversal failed");
            }

        } catch (Exception e) {
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            log.error("Error reversing payment for order: {}", orderId, e);
            publishPaymentEvent(sagaId, orderId, null,
                    "PAYMENT_REVERSE_FAILED", false,
                    null, "Technical error: " + e.getMessage());
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
            event.put("messageId", "PAY_" + System.currentTimeMillis() + "_" +
                    UUID.randomUUID().toString().substring(0, 8));
            event.put("sagaId", sagaId);
            event.put("type", eventType);
            event.put("success", success);
            event.put("orderId", orderId);

            if (paymentTransactionId != null) {
                event.put("paymentTransactionId", paymentTransactionId);
            }

            event.put("timestamp", System.currentTimeMillis());

            if (success) {
                event.put("message", successMessage);
            } else {
                event.put("errorMessage", errorMessage);
            }

            log.info("Publishing payment event: type={}, success={}, sagaId={}",
                    eventType, success, sagaId);

            kafkaTemplate.send("payment.events", sagaId, event);

        } catch (Exception e) {
            log.error("Error publishing payment event: {}", e.getMessage(), e);
        }
    }
}