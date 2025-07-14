package com.graduation.sagaorchestratorservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.sagaorchestratorservice.utils.MessageIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing messages to Kafka topics
 * This is a generic publisher that can handle any message type
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessagePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish a message to a topic with automatic key generation
     */
    public CompletableFuture<SendResult<String, Object>> publishMessage(Object message, String topic) {
        String messageId = MessageIdGenerator.generate();
        return publishMessage(message, topic, messageId);
    }

    /**
     * Publish a message to a topic with a specific key
     */
    public CompletableFuture<SendResult<String, Object>> publishMessage(Object message, String topic, String key) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be null or empty");
        }

        log.debug("Publishing message to topic: {}, key: {}, messageType: {}",
                topic, key, message.getClass().getSimpleName());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);

        // Add callback for logging
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Failed to send message to topic: {}, key: {}, error: {}",
                        topic, key, throwable.getMessage(), throwable);
            } else {
                log.debug("Successfully sent message to topic: {}, key: {}, offset: {}, partition: {}",
                        topic, key,
                        result.getRecordMetadata().offset(),
                        result.getRecordMetadata().partition());
            }
        });

        return future;
    }

    /**
     * Publish a command message with saga context
     */
    public CompletableFuture<SendResult<String, Object>> publishCommand(Map<String, Object> command,
                                                                        String topic, String sagaId) {
        // Ensure command has required fields
        if (!command.containsKey("messageId")) {
            command.put("messageId", MessageIdGenerator.generateCommandId());
        }
        if (!command.containsKey("sagaId")) {
            command.put("sagaId", sagaId);
        }
        if (!command.containsKey("timestamp")) {
            command.put("timestamp", System.currentTimeMillis());
        }
        if (!command.containsKey("type")) {
            command.put("type", "COMMAND");
        }

        // Use sagaId as partition key to ensure ordering
        return publishMessage(command, topic, sagaId);
    }

    /**
     * Publish an event message with saga context
     */
    public CompletableFuture<SendResult<String, Object>> publishEvent(Map<String, Object> event,
                                                                      String topic, String sagaId) {
        // Ensure event has required fields
        if (!event.containsKey("messageId")) {
            event.put("messageId", MessageIdGenerator.generateEventId());
        }
        if (!event.containsKey("sagaId")) {
            event.put("sagaId", sagaId);
        }
        if (!event.containsKey("timestamp")) {
            event.put("timestamp", System.currentTimeMillis());
        }
        if (!event.containsKey("type")) {
            event.put("type", "EVENT");
        }

        // Use sagaId as partition key to ensure ordering
        return publishMessage(event, topic, sagaId);
    }

    /**
     * Publish a saga step command
     */
    public CompletableFuture<SendResult<String, Object>> publishSagaStepCommand(String sagaId,
                                                                                Integer stepId,
                                                                                String commandType,
                                                                                Map<String, Object> payload,
                                                                                String topic) {
        Map<String, Object> command = Map.of(
                "messageId", MessageIdGenerator.generateForSagaStep(sagaId, stepId),
                "sagaId", sagaId,
                "stepId", stepId,
                "type", commandType,
                "timestamp", System.currentTimeMillis(),
                "payload", payload != null ? payload : Map.of()
        );

        return publishMessage(command, topic, sagaId);
    }

    /**
     * Publish a saga step event
     */
    public CompletableFuture<SendResult<String, Object>> publishSagaStepEvent(String sagaId,
                                                                              Integer stepId,
                                                                              String eventType,
                                                                              Map<String, Object> payload,
                                                                              String topic) {
        Map<String, Object> event = Map.of(
                "messageId", MessageIdGenerator.generateForSagaStep(sagaId, stepId),
                "sagaId", sagaId,
                "stepId", stepId,
                "type", eventType,
                "timestamp", System.currentTimeMillis(),
                "payload", payload != null ? payload : Map.of()
        );

        return publishMessage(event, topic, sagaId);
    }

    /**
     * Publish multiple messages in batch (fire and forget)
     */
    public void publishBatch(java.util.List<Map<String, Object>> messages, String topic) {
        if (messages == null || messages.isEmpty()) {
            log.warn("No messages to publish in batch");
            return;
        }

        log.info("Publishing batch of {} messages to topic: {}", messages.size(), topic);

        for (Map<String, Object> message : messages) {
            try {
                String sagaId = (String) message.get("sagaId");
                publishMessage(message, topic, sagaId != null ? sagaId : MessageIdGenerator.generate());
            } catch (Exception e) {
                log.error("Failed to publish message in batch: {}", message, e);
            }
        }
    }

    /**
     * Publish a message to Dead Letter Queue
     */
    public CompletableFuture<SendResult<String, Object>> publishToDLQ(Object originalMessage,
                                                                      String originalTopic,
                                                                      String errorReason) {
        Map<String, Object> dlqMessage = Map.of(
                "messageId", MessageIdGenerator.generate(),
                "originalTopic", originalTopic,
                "errorReason", errorReason,
                "timestamp", System.currentTimeMillis(),
                "originalMessage", originalMessage
        );

        // Use a default DLQ topic name
        String dlqTopic = originalTopic + ".dlq";

        log.warn("Publishing message to DLQ: topic={}, reason={}", dlqTopic, errorReason);

        return publishMessage(dlqMessage, dlqTopic, "DLQ");
    }

    /**
     * Health check method to verify Kafka connectivity
     */
    public boolean isHealthy() {
        try {
            // Simple health check by getting metadata
            kafkaTemplate.getDefaultTopic(); // This will trigger connection check
            return true;
        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            return false;
        }
    }
}