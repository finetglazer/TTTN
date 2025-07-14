package com.graduation.sagaorchestratorservice.controller;

import com.graduation.sagaorchestratorservice.model.OrderPurchaseSagaState;
import com.graduation.sagaorchestratorservice.service.OrderPurchaseSagaService;
import com.graduation.sagaorchestratorservice.service.SagaMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Saga Orchestrator Service
 */
@Slf4j
@RestController
@RequestMapping("/api/sagas")
@RequiredArgsConstructor
public class SagaController {

    private final OrderPurchaseSagaService orderPurchaseSagaService;
    private final SagaMonitoringService monitoringService;

    /**
     * Get saga by ID
     */
    @GetMapping("/{sagaId}")
    public ResponseEntity<Map<String, Object>> getSaga(@PathVariable String sagaId) {
        try {
            Optional<OrderPurchaseSagaState> saga = orderPurchaseSagaService.findById(sagaId);

            if (saga.isPresent()) {
                Map<String, Object> response = Map.of(
                        "success", true,
                        "saga", createSagaResponse(saga.get())
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "Saga not found"
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            log.error("Error getting saga: {}", sagaId, e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Error retrieving saga: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get saga by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getSagaByOrderId(@PathVariable Long orderId) {
        try {
            Optional<OrderPurchaseSagaState> saga = orderPurchaseSagaService.findByOrderId(orderId);

            if (saga.isPresent()) {
                Map<String, Object> response = Map.of(
                        "success", true,
                        "saga", createSagaResponse(saga.get())
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "Saga not found for order: " + orderId
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            log.error("Error getting saga for order: {}", orderId, e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Error retrieving saga: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get sagas by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getSagasByUserId(@PathVariable String userId) {
        try {
            List<OrderPurchaseSagaState> sagas = orderPurchaseSagaService.findByUserId(userId);

            List<Map<String, Object>> sagaResponses = sagas.stream()
                    .map(this::createSagaResponse)
                    .toList();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "sagas", sagaResponses,
                    "totalCount", sagas.size()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting sagas for user: {}", userId, e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Error retrieving user sagas: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get all active sagas
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveSagas() {
        try {
            List<OrderPurchaseSagaState> activeSagas = orderPurchaseSagaService.findActiveSagas();

            List<Map<String, Object>> sagaResponses = activeSagas.stream()
                    .map(this::createSagaResponse)
                    .toList();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "activeSagas", sagaResponses,
                    "totalCount", activeSagas.size()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting active sagas", e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Error retrieving active sagas: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cancel saga by user request
     */
    @PostMapping("/{sagaId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelSaga(@PathVariable String sagaId) {
        try {
            OrderPurchaseSagaState cancelledSaga = orderPurchaseSagaService.cancelSagaByUser(sagaId);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Saga cancellation initiated",
                    "saga", createSagaResponse(cancelledSaga)
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error cancelling saga: {}", sagaId, e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Error cancelling saga: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Get saga monitoring health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            SagaMonitoringService.HealthStatus health = monitoringService.getHealthStatus();

            Map<String, Object> response = Map.of(
                    "service", "SagaOrchestratorService",
                    "status", health.healthy ? "UP" : "DOWN",
                    "activeCount", health.activeCount,
                    "totalProcessed", health.totalProcessed,
                    "totalFailures", health.totalFailures,
                    "failureRate", String.format("%.2f%%", health.failureRate * 100),
                    "timestamp", java.time.LocalDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting health status", e);
            Map<String, Object> errorResponse = Map.of(
                    "service", "SagaOrchestratorService",
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = Map.of(
                "service", "SagaOrchestratorService",
                "status", "Running",
                "message", "Saga Orchestrator Service Test Endpoint",
                "timestamp", java.time.LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Create response object for saga
     */
    private Map<String, Object> createSagaResponse(OrderPurchaseSagaState saga) {
        Map<String, Object> response = new HashMap<>();
        response.put("sagaId", saga.getSagaId());
        response.put("orderId", saga.getOrderId());
        response.put("userId", saga.getUserId());
        response.put("status", saga.getStatus().name());
        response.put("currentStep", saga.getCurrentStep() != null ? saga.getCurrentStep().name() : "null");
        response.put("totalAmount", saga.getTotalAmount());
        response.put("startTime", saga.getStartTime().toString());
        response.put("lastUpdatedTime", saga.getLastUpdatedTime().toString());
        response.put("retryCount", saga.getRetryCount());
        response.put("failureReason", saga.getFailureReason());
        response.put("completedSteps", saga.getCompletedSteps());
        response.put("sagaEvents", saga.getSagaEvents());
        return response;
    }
}