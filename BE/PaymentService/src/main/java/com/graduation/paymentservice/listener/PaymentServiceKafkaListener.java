package com.graduation.paymentservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Kafka listener for Payment Service commands
 * Handles payment-related commands from the Saga Orchestrator
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentServiceKafkaListener {

    // TODO: Inject PaymentCommandHandlerService when implemented
    // private final PaymentCommandHandlerService commandHandlerService;

    /**
     * Listen to payment commands from Saga Orchestrator
     * Handles commands like: PAYMENT_PROCESS, PAYMENT_CANCEL
     */
    @KafkaListener(
            topics = "${kafka.topics.payment-commands}",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentCommands(@Payload Map<String, Object> command, Acknowledgment ack) {
        try {
            String commandType = (String) command.get("type");
            String sagaId = (String) command.get("sagaId");
            String messageId = (String) command.get("messageId");

            log.info("Processing payment command type: {} for saga: {} messageId: {}",
                    commandType, sagaId, messageId);

            // Route to appropriate handler based on command type
            switch (commandType) {
                case "PAYMENT_PROCESS":
//                    handleProcessPayment(command);
                    break;
                case "PAYMENT_CANCEL":
//                    handleCancelPayment(command);
                    break;
                default:
                    log.warn("Unknown payment command type: {} for saga: {}", commandType, sagaId);
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Payment command acknowledged: {} for saga: {}", commandType, sagaId);

        } catch (Exception e) {
            log.error("Error processing payment command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ
            throw new RuntimeException("Payment command processing failed", e);
        }
    }
}