package com.graduation.sagaorchestratorservice.handler;

import com.graduation.sagaorchestratorservice.service.OrderPurchaseSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handler for Order Service events
 * Processes order-related events and coordinates with saga service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderPurchaseSagaService orderPurchaseSagaService;

    /**
     * Handle order events from Order Service
     */
    public void handleOrderEvent(Map<String, Object> event) {
        String eventType = (String) event.get("type");
        String sagaId = (String) event.get("sagaId");

        log.debug("Processing order event type: {} for saga: {}", eventType, sagaId);

//        switch (eventType) {
//            case "ORDER_CREATED":
//                handleOrderCreatedEvent(event);
//                break;
//            case "ORDER_STATUS_UPDATED_CONFIRMED":
//            case "ORDER_STATUS_UPDATE_FAILED":
//
//            case "ORDER_STATUS_UPDATED_DELIVERED":
//            case "ORDER_CANCELLED":
//            case "ORDER_CANCELLATION_FAILED":
//                orderPurchaseSagaService.handleEventMessage(event);
//                break;
//            default:
//                log.debug("Unhandled order event type: {}", eventType);
//                break;
//        }

        if (eventType.equals("ORDER_CREATED")) {
            handleOrderCreatedEvent(event);
        } else {
            orderPurchaseSagaService.handleEventMessage(event);
        }

    }

    /**
     * Handle ORDER_CREATED event to start new saga
     */
    private void handleOrderCreatedEvent(Map<String, Object> event) {
        try {
            Long orderId = Long.valueOf(event.get("orderId").toString());
            String userId = (String) event.get("userId");
            String userEmail = (String) event.get("userEmail");
            String userName = (String) event.get("userName");
            String orderDescription = (String) event.get("orderDescription");
            java.math.BigDecimal totalAmount = new java.math.BigDecimal(event.get("totalAmount").toString());

            log.info("Starting saga for order created event: orderId={}, userId={}, amount={}",
                    orderId, userId, totalAmount);

            orderPurchaseSagaService.startSaga(userId, orderId, userEmail, userName, orderDescription, totalAmount);

            log.info("Successfully started saga for order: {}", orderId);

        } catch (Exception e) {
            log.error("Error handling ORDER_CREATED event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start saga for ORDER_CREATED event", e);
        }
    }
}