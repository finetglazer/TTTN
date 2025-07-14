package com.graduation.orderservice.listener;

import com.graduation.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka listener for Order Service commands
 * Handles order-related commands from the Saga Orchestrator
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderServiceKafkaListener {

    // TODO: Inject OrderCommandHandlerService when implemented
    // private final OrderCommandHandlerService commandHandlerService;

    private final OrderService orderService;

    /**
     * Listen to order commands from Saga Orchestrator
     * Handles commands like: ORDER_UPDATE_CONFIRMED, ORDER_UPDATE_DELIVERED, ORDER_CANCEL
     */
    @KafkaListener(
            topics = "${kafka.topics.order-commands}",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeOrderCommands(@Payload Map<String, Object> command, Acknowledgment ack) {
        try {
            String commandType = (String) command.get("type");
            String sagaId = (String) command.get("sagaId");
            String messageId = (String) command.get("messageId");

            log.info("Processing order command type: {} for saga: {} messageId: {}",
                    commandType, sagaId, messageId);

            // Route to appropriate handler based on command type
            switch (commandType) {
                case "ORDER_UPDATE_CONFIRMED":
                    orderService.handleUpdateOrderConfirmed(command);
                    break;
                case "ORDER_UPDATE_DELIVERED":
                    orderService.handleUpdateOrderDelivered(command);
                    break;
                case "ORDER_CANCEL":
                    orderService.handleCancelOrder(command);
                    break;
                default:
                    log.warn("Unknown order command type: {} for saga: {}", commandType, sagaId);
                    break;
            }

            // Acknowledge the message
            ack.acknowledge();
            log.debug("Order command acknowledged: {} for saga: {}", commandType, sagaId);

        } catch (Exception e) {
            log.error("Error processing order command: {}", e.getMessage(), e);
            // Don't acknowledge - will be retried or sent to DLQ
            throw new RuntimeException("Order command processing failed", e);
        }
    }


}