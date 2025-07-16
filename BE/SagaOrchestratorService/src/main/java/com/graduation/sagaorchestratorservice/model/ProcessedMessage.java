package com.graduation.sagaorchestratorservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.sagaorchestratorservice.model.enums.ActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity to track processed messages for idempotency
 */
@Slf4j
@Entity
@Table(name = "processed_messages", indexes = {
        @Index(name = "idx_processed_message_saga_step", columnList = "sagaId, stepId"),
        @Index(name = "idx_processed_message_processed_at", columnList = "processedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessedMessage {

    @Id
    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "saga_id")
    private String sagaId;

    @Column(name = "step_id")
    private Integer stepId;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private ActionType actionType;

    /**
     * Factory method to create a ProcessedMessage
     */
    public static ProcessedMessage create(String messageId, String sagaId, Integer stepId,
                                          String messageType, Map<String, Object> result) {
        return ProcessedMessage.builder()
                .messageId(messageId)
                .sagaId(sagaId)
                .stepId(stepId)
                .messageType(messageType)
                .resultJson(mapToJson(result))
                .processedAt(Instant.now())
                .build();
    }

    /**
     * Get the result as a Map
     */
    public Map<String, Object> getResult() {
        if (resultJson == null || resultJson.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(resultJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse result JSON for message {}: {}", messageId, resultJson, e);
            return new HashMap<>();
        }
    }

    /**
     * Set the result from a Map
     */
    public void setResult(Map<String, Object> result) {
        this.resultJson = mapToJson(result);
    }

    /**
     * Helper method to convert Map to JSON string
     */
    private static String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize result map to JSON: {}", map, e);
            return "{}";
        }
    }

    /**
     * Pre-persist callback to ensure processedAt is set
     */
    @PrePersist
    public void prePersist() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }

    @Override
    public String toString() {
        return String.format("ProcessedMessage{messageId='%s', sagaId='%s', stepId=%d, messageType='%s', processedAt=%s}",
                messageId, sagaId, stepId, messageType, processedAt);
    }
}