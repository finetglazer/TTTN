package com.graduation.sagaorchestratorservice.handler;

import com.graduation.sagaorchestratorservice.service.OrderPurchaseSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handler for Payment Service events
 * Processes payment-related events and coordinates with saga service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final OrderPurchaseSagaService orderPurchaseSagaService;

    /**
     * Handle payment events from Payment Service
     */
    public void handlePaymentEvent(Map<String, Object> event) {
        String eventType = (String) event.get("type");
        String sagaId = (String) event.get("sagaId");

        log.debug("Processing payment event type: {} for saga: {}", eventType, sagaId);

        // Validate required fields
        if (sagaId == null || sagaId.trim().isEmpty()) {
            log.warn("Received payment event without sagaId, ignoring: {}", event);
            return;
        }

        // Route event to saga service
        orderPurchaseSagaService.handleEventMessage(event);
    }
}