package com.graduation.sagaorchestratorservice.handler;

import com.graduation.sagaorchestratorservice.service.OrderPurchaseSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handler for Saga-specific events
 * Processes saga coordination, monitoring, and control events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventHandler {

    private final OrderPurchaseSagaService orderPurchaseSagaService;

    /**
     * Handle saga-specific events
     */
    public void handleSagaEvent(Map<String, Object> event) {
        String eventType = (String) event.get("type");
        String sagaId = (String) event.get("sagaId");

        log.debug("Processing saga event type: {} for saga: {}", eventType, sagaId);

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
     * Handle DLQ messages for debugging and recovery
     */
    public void handleDlqMessage(Object messagePayload) {
        try {
            log.error("Received message in DLQ: {}", messagePayload);

            // TODO: Implement DLQ handling logic
            // 1. Parse message to understand what failed
            // 2. Log for debugging purposes
            // 3. Optionally attempt manual recovery
            // 4. Alert monitoring systems

        } catch (Exception e) {
            log.error("Error processing DLQ message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle health check messages
     */
    public void handleHealthCheckMessage(Map<String, Object> message) {
        try {
            log.debug("Received health check message: {}", message.get("timestamp"));

            // Simply acknowledge to confirm listener is working

        } catch (Exception e) {
            log.error("Error processing health check message: {}", e.getMessage(), e);
        }
    }
}