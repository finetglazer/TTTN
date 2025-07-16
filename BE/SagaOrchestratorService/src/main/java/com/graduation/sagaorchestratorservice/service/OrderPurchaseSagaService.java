package com.graduation.sagaorchestratorservice.service;

import com.graduation.sagaorchestratorservice.exception.SagaExecutionException;
import com.graduation.sagaorchestratorservice.exception.SagaNotFoundException;
import com.graduation.sagaorchestratorservice.model.OrderPurchaseSagaState;
import com.graduation.sagaorchestratorservice.model.SagaEvent;
import com.graduation.sagaorchestratorservice.model.enums.*;
import com.graduation.sagaorchestratorservice.repository.OrderPurchaseSagaStateRepository;
import com.graduation.sagaorchestratorservice.utils.SagaIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service for managing Order Purchase Saga workflow
 * Handles the complete lifecycle from order creation to completion/compensation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPurchaseSagaService {

    private final OrderPurchaseSagaStateRepository sagaRepository;
    private final KafkaMessagePublisher messagePublisher;
    private final IdempotencyService idempotencyService;
    private final SagaMonitoringService monitoringService;

    @Value("${saga.retry.max-attempts:3}")
    private int maxRetries;

    @Value("${saga.timeout.default-minutes:10}")
    private long defaultTimeoutMinutes;

    // Kafka topic configuration
    @Value("${kafka.topics.order-commands}")
    private String orderCommandsTopic;

    @Value("${kafka.topics.payment-commands}")
    private String paymentCommandsTopic;

    @Value("${saga.compensation.max-retries:3}")
    private int defaultMaxCompensationRetries;

    /**
     * Start a new order purchase saga
     * Called when an order is created and needs payment processing
     */
    @Transactional
    public OrderPurchaseSagaState startSaga(String userId, Long orderId, String userEmail,
                                       String userName, String orderDescription, BigDecimal totalAmount) {

        log.info("Starting order purchase saga for order: {} user: {}", orderId, userId);

        // Check if saga already exists for this order
        Optional<OrderPurchaseSagaState> existingSaga = sagaRepository.findByOrderId(orderId);
        if (existingSaga.isPresent()) {
            log.warn("Saga already exists for order: {}", orderId);
            return existingSaga.get();
        }

        // Generate unique saga ID
        String sagaId = SagaIdGenerator.generateForType("ORDER_PURCHASE");

        // Create and save the saga
        OrderPurchaseSagaState saga = OrderPurchaseSagaState.initiate(
                sagaId, userId, orderId, userEmail, userName, orderDescription, totalAmount);

        sagaRepository.save(saga);

        // Record in monitoring
        monitoringService.recordSagaStarted(sagaId, "ORDER_PURCHASE");

        // Process the first step (PROCESS_PAYMENT)
        processNextStep(saga);

        log.info("Order purchase saga started successfully: {}", sagaId);
        return saga;
    }

    /**
     * Process the next step in the saga
     */
    @Transactional
    public void processNextStep(OrderPurchaseSagaState saga) {
        log.debug("Processing step [{}] for saga: {}", saga.getCurrentStep(), saga.getSagaId());

        // Handle completion
        if (saga.getCurrentStep() == OrderPurchaseSagaStep.COMPLETE_SAGA) {
            completeSaga(saga);
            return;
        }

        try {
            // Create command message for current step
            Map<String, Object> command = createCommandForCurrentStep(saga);
            if (command == null) {
                log.warn("No command created for step: {} in saga: {}",
                        saga.getCurrentStep(), saga.getSagaId());
                return;
            }

            // Save saga state before sending command
            sagaRepository.save(saga);

            // Determine target topic and publish command
            String targetTopic = getTopicForCommand(saga.getCurrentStep().getCommandType());

            messagePublisher.publishSagaStepCommand(
                    saga.getSagaId(),
                    saga.getCurrentStep().getStepNumber(),
                    saga.getCurrentStep().getCommandType().name(),
                    command,
                    targetTopic
            );

            log.info("Published command [{}] for saga [{}] to topic: {}",
                    saga.getCurrentStep().getCommandType(), saga.getSagaId(), targetTopic);

        } catch (Exception e) {
            log.error("Error processing step {} for saga {}", saga.getCurrentStep(), saga.getSagaId(), e);
            handleStepFailure(saga, "Failed to process step: " + e.getMessage());
        }
    }

    /**
     * Handle incoming event messages from services
     */
    @Transactional
    public void handleEventMessage(Map<String, Object> eventData) {
        String sagaId = (String) eventData.get("sagaId");
        String eventType = (String) eventData.get("type");
        Boolean success = (Boolean) eventData.get("success");

        log.debug("Handling event [{}] for saga: {}", eventType, sagaId);

        // Find the saga
        Optional<OrderPurchaseSagaState> optionalSaga = sagaRepository.findById(sagaId);
        if (optionalSaga.isEmpty()) {
            log.warn("Received event for unknown saga: {}", sagaId);
            return;
        }

        OrderPurchaseSagaState saga = optionalSaga.get();

        // Check idempotency
        String messageId = (String) eventData.get("messageId");
        ActionType actionType = isCompensationEvent(eventType) ? ActionType.COMPENSATION : ActionType.FORWARD;

        if (idempotencyService.isProcessed(messageId, sagaId,
                saga.getCurrentStep() != null ? saga.getCurrentStep().getStepNumber() : null,
                eventType, actionType)) {
            log.info("Event [{}] for saga [{}] has already been processed", eventType, sagaId);
            return;
        }

        try {
            // Validate event matches current step
            if (!isEventForCurrentStep(saga, eventType)) {
                log.warn("Event [{}] doesn't match current step [{}] for saga [{}]",
                        eventType, saga.getCurrentStep(), sagaId);
                recordEventProcessing(eventData, saga, "Event ignored - doesn't match current step");
                return;
            }

            // Process based on success/failure
            if (Boolean.TRUE.equals(success)) {
                processSuccessEvent(saga, eventData);
            } else {
                processFailureEvent(saga, eventData);
            }

            // Record successful processing
            recordEventProcessing(eventData, saga, "Event processed successfully");

        } catch (Exception e) {
            log.error("Error handling event {} for saga {}", eventType, sagaId, e);
            recordEventProcessing(eventData, saga, "Error processing event: " + e.getMessage());
        }
    }
    // Helper method to determine if event is compensation
    private boolean isCompensationEvent(String eventType) {
        return eventType.contains("FAILED") || eventType.contains("COMPENSATION") || eventType.contains("ROLLBACK");
    }

    /**
     * Process a successful event
     */
    private void processSuccessEvent(OrderPurchaseSagaState saga, Map<String, Object> eventData) {
        String eventType = (String) eventData.get("type");
        log.info("Processing success event [{}] for saga [{}]", eventType, saga.getSagaId());

        // Update saga with event data
        updateSagaWithEventData(saga, eventData);

        // Record monitoring
        long processingTime = System.currentTimeMillis(); // Simple timing
        monitoringService.recordMessageProcessed(saga.getSagaId(), eventType, processingTime);

        if (saga.getStatus() == SagaStatus.COMPENSATING) {
            // Handle compensation step success
            handleCompensationStepSuccess(saga);
        } else {
            // Normal flow - move to next step
            saga.addEvent(SagaEvent.stepCompleted(saga.getCurrentStep().getDescription()));
            saga.moveToNextStep();
        }

        // Save and continue
        sagaRepository.save(saga);

        // Process next step if still active
        if (saga.getStatus() == SagaStatus.IN_PROGRESS || saga.getStatus() == SagaStatus.COMPENSATING) {
            processNextStep(saga);
        }
    }

    /**
     * Process a failure event
     */
    private void processFailureEvent(OrderPurchaseSagaState saga, Map<String, Object> eventData) {
        String eventType = (String) eventData.get("type");
        String errorMessage = (String) eventData.get("errorMessage");

        log.warn("Processing failure event [{}] for saga [{}]: {}", eventType, saga.getSagaId(), errorMessage);

        // Record monitoring
        monitoringService.recordMessageFailed(saga.getSagaId(), eventType, errorMessage);

        // Check if this is a compensation step failure
        if (saga.getStatus() == SagaStatus.COMPENSATING) {
            // Handle compensation step failure
            handleCompensationStepFailure(saga, errorMessage != null ? errorMessage : "Compensation step failed");
        } else {
            // Handle normal step failure
            handleStepFailure(saga, errorMessage != null ? errorMessage : "Step failed without specific reason");
        }
    }

    /**
     * Handle step failure and start compensation if needed
     */
    private void handleStepFailure(OrderPurchaseSagaState saga, String reason) {
        log.warn("Handling step failure for saga [{}]: {}", saga.getSagaId(), reason);

        saga.handleFailure(reason, saga.getCurrentStep().name());

        // Start compensation if this step requires it
        if (saga.getCurrentStep().requiresCompensation()) {
            startCompensation(saga);
        } else {
            // For non-compensatable steps, just mark as failed
            saga.setStatus(SagaStatus.COMPENSATION_COMPLETED);
            saga.setEndTime(Instant.now());
            monitoringService.recordSagaFailed(saga.getSagaId(), reason);
        }

        sagaRepository.save(saga);
    }

    /**
     * Start compensation process
     */
    private void startCompensation(OrderPurchaseSagaState saga) {
        log.info("Starting compensation for saga: {}", saga.getSagaId());

        saga.startCompensation();
        sagaRepository.save(saga);

        // Process compensation steps
        processNextStep(saga);
    }

    /**
     * Handle successful completion of a compensation step
     */
    private void handleCompensationStepSuccess(OrderPurchaseSagaState saga) {
        OrderPurchaseSagaStep currentStep = saga.getCurrentStep();
        log.debug("Compensation step [{}] completed for saga: {}", currentStep, saga.getSagaId());

        // Add compensation step to completed steps
        saga.getCompletedSteps().add(currentStep.name());

        // Get next compensation step
        OrderPurchaseSagaStep nextStep = currentStep.getNextCompensationStep();

        if (nextStep == OrderPurchaseSagaStep.COMPLETE_SAGA) {
            // Compensation completed
            saga.completeCompensation();
            monitoringService.recordSagaFailed(saga.getSagaId(), "Compensated: " + saga.getFailureReason());
        } else {
            // Move to next compensation step
            saga.setCurrentStep(nextStep);
            saga.setCurrentStepStartTime(Instant.now());
            saga.setLastUpdatedTime(Instant.now());
            saga.addEvent(SagaEvent.of("COMPENSATION_STEP",
                    "Moving to compensation step: " + nextStep.getDescription()));
        }
    }

    /**
     * Handle failed compensation step
     */
    private void handleCompensationStepFailure(OrderPurchaseSagaState saga, String reason) {
        log.warn("🚨 Compensation step FAILED for saga [{}]: {}", saga.getSagaId(), reason);

        // Retry compensation or mark as compensation failed
        if (saga.getCompensationRetryCount() < saga.getMaxCompensationRetries()) {
            // Retry compensation step
            saga.incrementCompensationRetryCount();
            saga.addEvent(SagaEvent.of("COMPENSATION_RETRY",
                    "Retrying compensation step " + saga.getCurrentStep() + " (attempt " + saga.getCompensationRetryCount() + ")"));

            log.info("🔄 Retrying compensation step for saga [{}], attempt {}/{}",
                    saga.getSagaId(), saga.getCompensationRetryCount(), saga.getMaxCompensationRetries());

            sagaRepository.save(saga);

            // Retry the current compensation step
            processNextStep(saga);

        } else {
            // Mark as compensation failed - manual intervention needed
            log.error("❌ COMPENSATION FAILED for saga [{}] after {} retries. Manual intervention required!",
                    saga.getSagaId(), saga.getMaxCompensationRetries());

            saga.setStatus(SagaStatus.COMPENSATION_FAILED);
            saga.setEndTime(Instant.now());
            saga.addEvent(SagaEvent.of("COMPENSATION_FAILED",
                    "Compensation failed after " + saga.getMaxCompensationRetries() + " retries: " + reason));

            sagaRepository.save(saga);

            // Record the compensation failure
            monitoringService.recordSagaFailed(saga.getSagaId(),
                    "Compensation failed after retries: " + reason);
        }
    }

    /**
     * Complete saga successfully
     */
    private void completeSaga(OrderPurchaseSagaState saga) {
        log.info("Completing saga successfully: {}", saga.getSagaId());

        saga.setStatus(SagaStatus.COMPLETED);
        saga.setEndTime(Instant.now());
        saga.addEvent(SagaEvent.sagaCompleted());

        sagaRepository.save(saga);
        monitoringService.recordSagaCompleted(saga.getSagaId());
    }

    /**
     * Create command for the current step
     */
    private Map<String, Object> createCommandForCurrentStep(OrderPurchaseSagaState saga) {
        Map<String, Object> command = new HashMap<>();

        switch (saga.getCurrentStep()) {
            case PROCESS_PAYMENT:
                command.put("orderId", saga.getOrderId().toString());
                command.put("userId", saga.getUserId());
                command.put("amount", saga.getTotalAmount());
                command.put("paymentMethod", "CREDIT_CARD"); // Default for now
                break;

            case UPDATE_ORDER_STATUS:
                command.put("orderId", saga.getOrderId().toString());
                command.put("newStatus", "CONFIRMED");
                command.put("reason", "Payment processed successfully");
                break;

            case CANCEL_PAYMENT:
                command.put("orderId", saga.getOrderId().toString());
                command.put("paymentTransactionId", saga.getPaymentTransactionId());
                command.put("reason", saga.getFailureReason());
                break;

            case CANCEL_ORDER:
                command.put("orderId", saga.getOrderId().toString());
                command.put("reason", saga.getFailureReason());
                command.put("cancelledBy", "SAGA_COMPENSATION");
                break;

            default:
                log.warn("No command mapping for step: {}", saga.getCurrentStep());
                return null;
        }

        return command;
    }

    /**
     * Get Kafka topic for command type
     */
    private String getTopicForCommand(CommandType commandType) {
        return switch (commandType) {
            case PAYMENT_PROCESS, PAYMENT_REVERSE -> paymentCommandsTopic;
            case ORDER_UPDATE_CONFIRMED, ORDER_UPDATE_DELIVERED, ORDER_CANCEL -> orderCommandsTopic;
            default -> "saga.dlq"; // Dead letter queue fallback
        };
    }

    /**
     * Check if event matches current saga step
     */
    private boolean isEventForCurrentStep(OrderPurchaseSagaState saga, String eventType) {
        if (saga.getCurrentStep() == null) {
            return false;
        }

        try {
            EventType event = EventType.valueOf(eventType);
            CommandType expectedCommandType = saga.getCurrentStep().getCommandType();
            CommandType eventCommandType = event.getAssociatedCommandType();

            return eventCommandType == expectedCommandType;
        } catch (Exception e) {
            log.error("Error checking event match for saga {}: {}", saga.getSagaId(), e.getMessage());
            return false;
        }
    }

    /**
     * Update saga with data from event
     */
    private void updateSagaWithEventData(OrderPurchaseSagaState saga, Map<String, Object> eventData) {
        String eventType = (String) eventData.get("type");

        // Extract specific data based on event type
        switch (eventType) {
            case "PAYMENT_PROCESSED":
                Object paymentTransactionId = eventData.get("paymentTransactionId");
                if (paymentTransactionId != null) {
                    saga.setPaymentTransactionId(Long.valueOf(paymentTransactionId.toString()));
                }
                break;
            // Add other event data extraction as needed
        }
    }

    /**
     * Record event processing for idempotency
     */
    private void recordEventProcessing(Map<String, Object> eventData, OrderPurchaseSagaState saga, String result) {
        String messageId = (String) eventData.get("messageId");
        String eventType = (String) eventData.get("type");
        Integer stepId = saga.getCurrentStep() != null ? saga.getCurrentStep().getStepNumber() : null;

        Map<String, Object> processingResult = new HashMap<>();
        processingResult.put("result", result);
        processingResult.put("sagaStatus", saga.getStatus().name());
        processingResult.put("currentStep", saga.getCurrentStep() != null ? saga.getCurrentStep().name() : "null");

        idempotencyService.recordProcessing(messageId, saga.getSagaId(), stepId, eventType, processingResult);
    }

    /**
     * Check for timed-out sagas and handle them
     */
    public void checkForTimeouts() {
        log.debug("Checking for timed-out saga steps");

        List<SagaStatus> activeStatuses = Arrays.asList(SagaStatus.STARTED, SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING);
        Instant cutoffTime = Instant.now().minus(Duration.ofMinutes(defaultTimeoutMinutes));

        List<OrderPurchaseSagaState> timedOutSagas = sagaRepository.findSagasWithStepTimeout(activeStatuses, cutoffTime);

        for (OrderPurchaseSagaState saga : timedOutSagas) {
            handleSagaTimeout(saga);
        }
    }

    /**
     * Handle a timed-out saga
     */
    private void handleSagaTimeout(OrderPurchaseSagaState saga) {
        log.warn("Saga step timed out: {}", saga.getSagaId());

        if (saga.getRetryCount() < saga.getMaxRetries()) {
            // Retry the step
            saga.incrementRetryCount();
            saga.addEvent(SagaEvent.of("RETRY", "Retrying step " + saga.getCurrentStep() + " after timeout"));
            sagaRepository.save(saga);
            processNextStep(saga);
        } else {
            // Exceeded retries, start compensation
            handleStepFailure(saga, "Step timed out after " + saga.getMaxRetries() + " retries");
        }
    }

    // Repository access methods
    public Optional<OrderPurchaseSagaState> findById(String sagaId) {
        return sagaRepository.findById(sagaId);
    }

    public Optional<OrderPurchaseSagaState> findByOrderId(Long orderId) {
        return sagaRepository.findByOrderId(orderId);
    }

    public List<OrderPurchaseSagaState> findByUserId(String userId) {
        return sagaRepository.findByUserId(userId);
    }

    public List<OrderPurchaseSagaState> findActiveSagas() {
        return sagaRepository.findActiveSagas();
    }

    /**
     * Cancel a saga by user request (if possible)
     */
    @Transactional
    public OrderPurchaseSagaState cancelSagaByUser(String sagaId) throws SagaNotFoundException, SagaExecutionException {
        log.info("Processing user cancellation request for saga: {}", sagaId);

        Optional<OrderPurchaseSagaState> optionalSaga = sagaRepository.findById(sagaId);
        if (optionalSaga.isEmpty()) {
            throw new SagaNotFoundException(sagaId);
        }

        OrderPurchaseSagaState saga = optionalSaga.get();

        // Check if saga can be cancelled
        if (!saga.getStatus().isActive()) {
            throw new SagaExecutionException(sagaId, "Cannot cancel saga in state: " + saga.getStatus());
        }

        // Mark as failed and start compensation
        saga.handleFailure("Cancelled by user request", saga.getCurrentStep().name());
        startCompensation(saga);

        return saga;
    }
}