package com.graduation.paymentservice.service;

import com.graduation.paymentservice.model.PaymentTransaction;
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

    /**
     * Handle process payment command
     * Mock implementation that randomly succeeds or fails
     */
    @Transactional
    public void handleProcessPayment(Map<String, Object> command) {
        String sagaId = (String) command.get("sagaId");

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
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        String paymentMethod = (String) payload.getOrDefault("paymentMethod", "CREDIT_CARD");

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
                publishPaymentEvent(sagaId, orderId, savedTransaction.getId(),
                        "PAYMENT_PROCESSED", true,
                        "Payment processed successfully", null);
            } else {
                String reason = savedTransaction.getMockDecisionReason() != null
                        ? savedTransaction.getMockDecisionReason()
                        : "Payment declined";

                publishPaymentEvent(sagaId, orderId, savedTransaction.getId(),
                        "PAYMENT_FAILED", false,
                        null, reason);
            }

        } catch (Exception e) {
            log.error("Error processing payment for order: {}", orderId, e);

            // Publish failure event
            publishPaymentEvent(sagaId, orderId, null,
                    "PAYMENT_FAILED", false,
                    null, "Technical error: "  + e.getMessage());
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