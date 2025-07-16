package com.graduation.paymentservice.listener;

import com.graduation.paymentservice.service.PaymentCommandHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka listener for Payment Service commands
 * Handles payment-related commands from the Saga Orchestrator
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentServiceKafkaListener {

    private final PaymentCommandHandlerService commandHandlerService;

    /**
     * Listen to payment commands from Saga Orchestrator
     * Handles commands like: PAYMENT_PROCESS, PAYMENT_REVERSE
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
                    commandHandlerService.handleProcessPayment(command);
                    break;
                case "PAYMENT_REVERSE":
                    commandHandlerService.handleReversePayment(command);
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