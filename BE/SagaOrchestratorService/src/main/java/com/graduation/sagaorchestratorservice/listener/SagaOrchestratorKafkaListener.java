package com.graduation.sagaorchestratorservice.listener;

import com.graduation.sagaorchestratorservice.service.OrderPurchaseSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka listener for Saga Orchestrator Service
 * Handles events from Order and Payment services to coordinate saga flow
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaOrchestratorKafkaListener {

    private final OrderPurchaseSagaService orderPurchaseSagaService;

    /**
     * Listen to order events from Order Service
     * Handles events like: ORDER_STATUS_UPDATED_CONFIRMED, ORDER_STATUS_UPDATED_DELIVERED,
     * ORDER_CANCELLED, ORDER_STATUS_UPDATE_FAILED, ORDER_CANCELLATION_FAILED
     */
    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            containerFactory = "orderEventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-order-events"
    )
    public void consumeOrderEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
        try {
            String eventType = (String) event.get("type");
            String sagaId = (String) event.get("sagaId");
            String messageId = (String) event.get("messageId");

            log.info("Received order event type: {} for saga: {} messageId: {}",
                    eventType, sagaId, messageId);

            // Validate required fields
            if (sagaId == null || sagaId.trim().isEmpty()) {
                log.warn("Received order event without sagaId, ignoring: {}", event);
                ack.acknowledge();
                return;
            }

            // Route event to saga service
            orderPurchaseSagaService.handleEventMessage(event);

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Order event acknowledged: {} for saga: {}", eventType, sagaId);

        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ
            throw new RuntimeException("Order event processing failed", e);
        }
    }

    /**
     * Listen to payment events from Payment Service
     * Handles events like: PAYMENT_PROCESSED, PAYMENT_FAILED, PAYMENT_CANCELLED, PAYMENT_CANCELLATION_FAILED
     */
    @KafkaListener(
            topics = "${kafka.topics.payment-events}",
            containerFactory = "paymentEventKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-payment-events"
    )
    public void consumePaymentEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
        try {
            String eventType = (String) event.get("type");
            String sagaId = (String) event.get("sagaId");
            String messageId = (String) event.get("messageId");

            log.info("Received payment event type: {} for saga: {} messageId: {}",
                    eventType, sagaId, messageId);

            // Validate required fields
            if (sagaId == null || sagaId.trim().isEmpty()) {
                log.warn("Received payment event without sagaId, ignoring: {}", event);
                ack.acknowledge();
                return;
            }

            // Route event to saga service
            orderPurchaseSagaService.handleEventMessage(event);

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Payment event acknowledged: {} for saga: {}", eventType, sagaId);

        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ
            throw new RuntimeException("Payment event processing failed", e);
        }
    }

    /**
     * Listen to saga-specific events (for monitoring and coordination)
     * This can be used for saga-to-saga communication or external monitoring
     */
    @KafkaListener(
            topics = "${kafka.topics.saga-events}",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-saga-events"
    )
    public void consumeSagaEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
        try {
            String eventType = (String) event.get("type");
            String sagaId = (String) event.get("sagaId");
            String messageId = (String) event.get("messageId");

            log.info("Received saga event type: {} for saga: {} messageId: {}",
                    eventType, sagaId, messageId);

            // Handle saga-specific events
            switch (eventType) {
                case "SAGA_TIMEOUT_CHECK":
                    handleSagaTimeoutCheck(event);
                    break;
                case "SAGA_MONITORING_UPDATE":
                    handleSagaMonitoringUpdate(event);
                    break;
                case "SAGA_EXTERNAL_CANCEL_REQUEST":
                    handleExternalCancelRequest(event);
                    break;
                default:
                    log.debug("Unhandled saga event type: {} for saga: {}", eventType, sagaId);
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Saga event acknowledged: {} for saga: {}", eventType, sagaId);

        } catch (Exception e) {
            log.error("Error processing saga event: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ
            throw new RuntimeException("Saga event processing failed", e);
        }
    }

    /**
     * Listen to Dead Letter Queue messages for debugging and recovery
     */
    @KafkaListener(
            topics = "${kafka.topics.dlq}",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-dlq"
    )
    public void consumeDlqMessages(@Payload Object messagePayload, Acknowledgment ack) {
        try {
            log.error("Received message in DLQ: {}", messagePayload);

            // TODO: Implement DLQ handling logic
            // 1. Parse message to understand what failed
            // 2. Log for debugging purposes
            // 3. Optionally attempt manual recovery
            // 4. Alert monitoring systems

            // For now, just log and acknowledge
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing DLQ message: {}", e.getMessage(), e);
            // Still acknowledge to prevent infinite loop in DLQ processing
            ack.acknowledge();
        }
    }

    /**
     * Handle saga timeout check events
     * Triggered by scheduler to check for timed-out saga steps
     */
    private void handleSagaTimeoutCheck(Map<String, Object> event) {
        String sagaId = (String) event.get("sagaId");
        log.debug("Handling timeout check for saga: {}", sagaId);

        try {
            // Delegate to saga service for timeout handling
            orderPurchaseSagaService.checkForTimeouts();

        } catch (Exception e) {
            log.error("Error handling timeout check for saga: {}", sagaId, e);
        }
    }

    /**
     * Handle saga monitoring update events
     * Used for metrics and monitoring updates
     */
    private void handleSagaMonitoringUpdate(Map<String, Object> event) {
        String sagaId = (String) event.get("sagaId");
        String updateType = (String) event.get("updateType");

        log.debug("Handling monitoring update for saga: {} type: {}", sagaId, updateType);

        // TODO: Implement monitoring update logic
        // This could update dashboard metrics, send alerts, etc.
    }

    /**
     * Handle external cancel request
     * Allows external systems to request saga cancellation
     */
    private void handleExternalCancelRequest(Map<String, Object> event) {
        String sagaId = (String) event.get("sagaId");
        String requestedBy = (String) event.get("requestedBy");
        String reason = (String) event.get("reason");

        log.info("Handling external cancel request for saga: {} by: {} reason: {}",
                sagaId, requestedBy, reason);

        try {
            // Delegate to saga service for cancellation
            orderPurchaseSagaService.cancelSagaByUser(sagaId);

        } catch (Exception e) {
            log.error("Error handling external cancel request for saga: {}", sagaId, e);
        }
    }

    /**
     * Health check listener (optional)
     * Can be used to verify kafka connectivity and listener health
     */
    @KafkaListener(
            topics = "saga.health.check",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}-health"
    )
    public void consumeHealthCheckMessages(@Payload Map<String, Object> message, Acknowledgment ack) {
        try {
            log.debug("Received health check message: {}", message.get("timestamp"));

            // Simply acknowledge to confirm listener is working
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing health check message: {}", e.getMessage(), e);
            ack.acknowledge(); // Still acknowledge to avoid blocking
        }
    }
}