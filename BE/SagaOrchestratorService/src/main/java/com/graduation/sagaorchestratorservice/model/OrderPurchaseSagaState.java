package com.graduation.sagaorchestratorservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.graduation.sagaorchestratorservice.model.enums.OrderPurchaseSagaStep;
import com.graduation.sagaorchestratorservice.model.enums.SagaStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity representing the state of an Order Purchase Saga
 * Tracks the complete lifecycle from order creation to completion
 */
@Slf4j
@Entity
@Table(name = "order_purchase_sagas", indexes = {
        @Index(name = "idx_saga_order_id", columnList = "orderId"),
        @Index(name = "idx_saga_user_id", columnList = "userId"),
        @Index(name = "idx_saga_status", columnList = "status"),
        @Index(name = "idx_saga_start_time", columnList = "startTime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderPurchaseSagaState {

    @Id
    @Column(name = "saga_id", nullable = false)
    private String sagaId;

    // Business data from order
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "order_description", length = 1000)
    private String orderDescription;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Payment processing data
    @Column(name = "payment_transaction_id")
    private Long paymentTransactionId;

    // Saga execution state
    @Enumerated(EnumType.STRING)
    @Column(name = "current_step")
    private OrderPurchaseSagaStep currentStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    @Column(name = "completed_steps", columnDefinition = "TEXT")
    private String completedStepsJson;

    @Column(name = "saga_events", columnDefinition = "TEXT")
    private String sagaEventsJson;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    // Timing information
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "last_updated_time", nullable = false)
    private Instant lastUpdatedTime;

    @Column(name = "current_step_start_time")
    private Instant currentStepStartTime;

    // Retry information
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Transient
    private List<String> completedSteps;

    @Transient
    private List<SagaEvent> sagaEvents;

    /**
     * Factory method to initiate a new order purchase saga
     */
    public static OrderPurchaseSagaState initiate(String sagaId, String userId, Long orderId,
                                             String userEmail, String userName,
                                             String orderDescription, BigDecimal totalAmount) {
        OrderPurchaseSagaState saga = OrderPurchaseSagaState.builder()
                .sagaId(sagaId)
                .userId(userId)
                .orderId(orderId)
                .userEmail(userEmail)
                .userName(userName)
                .orderDescription(orderDescription)
                .totalAmount(totalAmount)
                .currentStep(OrderPurchaseSagaStep.PROCESS_PAYMENT)
                .status(SagaStatus.STARTED)
                .startTime(Instant.now())
                .lastUpdatedTime(Instant.now())
                .currentStepStartTime(Instant.now())
                .retryCount(0)
                .maxRetries(3)
                .build();

        saga.initializeCollections();
        saga.addEvent(SagaEvent.of("SAGA_INITIATED",
                "Order purchase saga initiated for order: " + orderId));

        return saga;
    }

    /**
     * Move to the next step in the saga
     */
    public void moveToNextStep() {
        // Add current step to completed steps
        if (currentStep != null && !currentStep.isCompensationStep()) {
            getCompletedSteps().add(currentStep.name());
        }

        // Get next step
        OrderPurchaseSagaStep nextStep = currentStep.getNextStep();
        currentStep = nextStep;
        currentStepStartTime = Instant.now();

        if (nextStep == OrderPurchaseSagaStep.COMPLETE_SAGA) {
            status = SagaStatus.COMPLETED;
            endTime = Instant.now();
            addEvent(SagaEvent.sagaCompleted());
        } else {
            status = SagaStatus.IN_PROGRESS;
            addEvent(SagaEvent.stepStarted(nextStep.getDescription()));
        }

        lastUpdatedTime = Instant.now();
    }

    /**
     * Handle a step failure
     */
    public void handleFailure(String reason, String stepName) {
        failureReason = reason;
        status = SagaStatus.FAILED;
        addEvent(SagaEvent.stepFailed(stepName, reason));
        lastUpdatedTime = Instant.now();
    }

    /**
     * Start compensation process
     */
    public void startCompensation() {
        status = SagaStatus.COMPENSATING;
        addEvent(SagaEvent.compensationStarted(failureReason));

        // Determine first compensation step based on completed steps
        boolean paymentProcessed = getCompletedSteps().contains(OrderPurchaseSagaStep.PROCESS_PAYMENT.name());
        boolean orderStatusUpdated = getCompletedSteps().contains(OrderPurchaseSagaStep.UPDATE_ORDER_STATUS.name());

        currentStep = OrderPurchaseSagaStep.determineFirstCompensationStep(
                paymentProcessed, orderStatusUpdated);

        currentStepStartTime = Instant.now();
        lastUpdatedTime = Instant.now();

        addEvent(SagaEvent.of("COMPENSATION_STEP",
                "Starting compensation with step: " + currentStep.getDescription()));
    }

    /**
     * Complete compensation process
     */
    public void completeCompensation() {
        status = SagaStatus.COMPENSATION_COMPLETED;
        currentStep = OrderPurchaseSagaStep.COMPLETE_SAGA;
        endTime = Instant.now();
        addEvent(SagaEvent.compensationCompleted());
        lastUpdatedTime = Instant.now();
    }

    /**
     * Add an event to the saga history
     */
    public void addEvent(SagaEvent event) {
        if (sagaEvents == null) {
            sagaEvents = new ArrayList<>();
        }
        sagaEvents.add(event);
    }

    /**
     * Check if the current step has timed out
     */
    public boolean isCurrentStepTimedOut(Duration timeout) {
        if (currentStepStartTime == null) {
            return false;
        }
        return Duration.between(currentStepStartTime, Instant.now()).compareTo(timeout) > 0;
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        retryCount++;
        lastUpdatedTime = Instant.now();
    }

    /**
     * Check if max retries exceeded
     */
    public boolean isMaxRetriesExceeded() {
        return retryCount >= maxRetries;
    }

    /**
     * Get completed steps list
     */
    public List<String> getCompletedSteps() {
        if (completedSteps == null) {
            if (completedStepsJson != null && !completedStepsJson.trim().isEmpty()) {
                try {
                    completedSteps = objectMapper.readValue(completedStepsJson, new TypeReference<List<String>>() {});
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse completed steps JSON for saga {}: {}", sagaId, completedStepsJson, e);
                    completedSteps = new ArrayList<>();
                }
            } else {
                completedSteps = new ArrayList<>();
            }
        }
        return completedSteps;
    }

    /**
     * Get saga events list
     */
    public List<SagaEvent> getSagaEvents() {
        if (sagaEvents == null) {
            if (sagaEventsJson != null && !sagaEventsJson.trim().isEmpty()) {
                try {
                    sagaEvents = objectMapper.readValue(sagaEventsJson, new TypeReference<List<SagaEvent>>() {});
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse saga events JSON for saga {}: {}", sagaId, sagaEventsJson, e);
                    sagaEvents = new ArrayList<>();
                }
            } else {
                sagaEvents = new ArrayList<>();
            }
        }
        return sagaEvents;
    }

    /**
     * Initialize collections if null
     */
    private void initializeCollections() {
        if (completedSteps == null) {
            completedSteps = new ArrayList<>();
        }
        if (sagaEvents == null) {
            sagaEvents = new ArrayList<>();
        }
    }

    /**
     * Pre-persist callback to serialize collections
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        // Configure ObjectMapper once, properly
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        if (completedSteps != null) {
            try {
                completedStepsJson = objectMapper.writeValueAsString(completedSteps);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize completed steps for saga {}", sagaId, e);
                completedStepsJson = "[]";
            }
        }

        if (sagaEvents != null) {
            try {
                sagaEventsJson = objectMapper.writeValueAsString(sagaEvents);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize saga events for saga {}", sagaId, e);
                sagaEventsJson = "[]";
            }
        }

        if (lastUpdatedTime == null) {
            lastUpdatedTime = Instant.now();
        }
    }

    /**
     * Post-load callback to deserialize collections
     */
    @PostLoad
    public void postLoad() {
        // Initialize transient collections from JSON
        getCompletedSteps();
        getSagaEvents();
    }

    @Override
    public String toString() {
        return String.format("OrderPurchaseSaga{sagaId='%s', orderId=%d, userId='%s', status=%s, currentStep=%s}",
                sagaId, orderId, userId, status, currentStep);
    }
}