package com.graduation.sagaorchestratorservice.listener;

import com.graduation.sagaorchestratorservice.handler.OrderEventHandler;
import com.graduation.sagaorchestratorservice.handler.PaymentEventHandler;
import com.graduation.sagaorchestratorservice.handler.SagaEventHandler;
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

    private final OrderEventHandler orderEventHandler;
    private final PaymentEventHandler paymentEventHandler;
    private final SagaEventHandler sagaEventHandler;

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

            // Delegate to handler
            orderEventHandler.handleOrderEvent(event);

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

            // Delegate to handler
            paymentEventHandler.handlePaymentEvent(event);

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

            // Delegate to handler
            sagaEventHandler.handleSagaEvent(event);

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
            // Delegate to handler
            sagaEventHandler.handleDlqMessage(messagePayload);

            // Acknowledge to prevent infinite loop in DLQ processing
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing DLQ message: {}", e.getMessage(), e);
            // Still acknowledge to prevent infinite loop in DLQ processing
            ack.acknowledge();
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
            // Delegate to handler
            sagaEventHandler.handleHealthCheckMessage(message);

            // Acknowledge to confirm listener is working
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error processing health check message: {}", e.getMessage(), e);
            ack.acknowledge(); // Still acknowledge to avoid blocking
        }
    }
}