// ===================== OrderEventHandler.java =====================

package com.graduation.sagaorchestratorservice.handler;

import com.graduation.sagaorchestratorservice.constants.Constant;
import com.graduation.sagaorchestratorservice.service.OrderPurchaseSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final OrderPurchaseSagaService orderPurchaseSagaService;

    /**
     * Handle order events from Order Service
     */
    public void handleOrderEvent(Map<String, Object> event) {
        String eventType = (String) event.get(Constant.FIELD_TYPE);
        String sagaId = (String) event.get(Constant.FIELD_SAGA_ID);

        log.debug(Constant.LOG_PROCESSING_ORDER_EVENT, eventType, sagaId);

        if (eventType.equals(Constant.EVENT_ORDER_CREATED)) {
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
            Long orderId = Long.valueOf(event.get(Constant.FIELD_ORDER_ID).toString());
            String userId = (String) event.get(Constant.FIELD_USER_ID);
            String userEmail = (String) event.get(Constant.FIELD_USER_EMAIL);
            String userName = (String) event.get(Constant.FIELD_USER_NAME);
            String orderDescription = (String) event.get(Constant.FIELD_ORDER_DESCRIPTION);
            java.math.BigDecimal totalAmount = new java.math.BigDecimal(event.get(Constant.FIELD_TOTAL_AMOUNT).toString());

            log.info(Constant.LOG_STARTING_SAGA_FOR_ORDER,
                    orderId, userId, totalAmount);

            orderPurchaseSagaService.startSaga(userId, orderId, userEmail, userName, orderDescription, totalAmount);

            log.info(Constant.LOG_SAGA_STARTED_SUCCESS, orderId);

        } catch (Exception e) {
            log.error(Constant.ERROR_HANDLING_ORDER_CREATED, e);
            throw new RuntimeException(Constant.ERROR_FAILED_TO_START_SAGA, e);
        }
    }
}