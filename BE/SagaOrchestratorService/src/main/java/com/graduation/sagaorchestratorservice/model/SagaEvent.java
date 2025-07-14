package com.graduation.sagaorchestratorservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents an event in the saga execution history
 * Used for tracking and auditing saga progress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaEvent {

    private String type;
    private String description;
    private Instant timestamp;

    /**
     * Factory method to create a saga event
     */
    public static SagaEvent of(String type, String description) {
        return SagaEvent.builder()
                .type(type)
                .description(description)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Factory method to create a step start event
     */
    public static SagaEvent stepStarted(String stepName) {
        return of("STEP_STARTED", "Started executing step: " + stepName);
    }

    /**
     * Factory method to create a step completed event
     */
    public static SagaEvent stepCompleted(String stepName) {
        return of("STEP_COMPLETED", "Successfully completed step: " + stepName);
    }

    /**
     * Factory method to create a step failed event
     */
    public static SagaEvent stepFailed(String stepName, String reason) {
        return of("STEP_FAILED", "Step " + stepName + " failed: " + reason);
    }

    /**
     * Factory method to create a compensation started event
     */
    public static SagaEvent compensationStarted(String reason) {
        return of("COMPENSATION_STARTED", "Started compensation: " + reason);
    }

    /**
     * Factory method to create a compensation completed event
     */
    public static SagaEvent compensationCompleted() {
        return of("COMPENSATION_COMPLETED", "Compensation process completed successfully");
    }

    /**
     * Factory method to create a saga completed event
     */
    public static SagaEvent sagaCompleted() {
        return of("SAGA_COMPLETED", "Order purchase saga completed successfully");
    }

    /**
     * Factory method to create a saga failed event
     */
    public static SagaEvent sagaFailed(String reason) {
        return of("SAGA_FAILED", "Saga execution failed: " + reason);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", timestamp, type, description);
    }
}