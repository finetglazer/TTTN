package com.graduation.orderservice.service;

import com.graduation.orderservice.model.Order;
import com.graduation.orderservice.model.OrderStatus;
import com.graduation.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for Order business logic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create a new order and trigger saga
     */
    @Transactional
    public Order createOrder(String userId, String userEmail, String userName,
                             String orderDescription, BigDecimal totalAmount) {

        log.info("Creating order for user: {} with amount: {}", userId, totalAmount);

        try {
            // Create the order
            Order order = Order.createOrder(userId, userEmail, userName, orderDescription, totalAmount);

            // Save to database
            Order savedOrder = orderRepository.save(order);
            log.info("Order created successfully with ID: {}", savedOrder.getId());

            // Publish event to trigger saga
            publishOrderCreatedEvent(savedOrder);

            return savedOrder;

        } catch (Exception e) {
            log.error("Error creating order for user: {}", userId, e);
            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }
    }

    /**
     * Publish OrderCreated event to trigger saga
     */
    private void publishOrderCreatedEvent(Order order) {
        try {
            // Create event payload
            Map<String, Object> event = new HashMap<>();
            event.put("messageId", generateMessageId());
            event.put("type", "ORDER_CREATED");
            event.put("timestamp", System.currentTimeMillis());

            // Order data for saga
            event.put("orderId", order.getId());
            event.put("userId", order.getUserId());
            event.put("userEmail", order.getUserEmail());
            event.put("userName", order.getUserName());
            event.put("orderDescription", order.getOrderDescription());
            event.put("totalAmount", order.getTotalAmount());
            event.put("orderStatus", order.getStatus().name());
            event.put("createdAt", order.getCreatedAt().toString());

            // Publish to saga events topic
            kafkaTemplate.send("saga.events", order.getId().toString(), event);

            log.info("Published ORDER_CREATED event for order: {} to trigger saga", order.getId());

        } catch (Exception e) {
            log.error("Failed to publish ORDER_CREATED event for order: {}", order.getId(), e);
            // Don't fail the order creation if event publishing fails
            // In production, you might want to retry or use a more robust mechanism
        }
    }

    /**
     * Update order status (called by saga)
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, String reason, String sagaId) {
        log.info("Updating order {} status to {} for saga: {}", orderId, newStatus, sagaId);

        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isEmpty()) {
            throw new RuntimeException("Order not found: " + orderId);
        }

        Order order = optionalOrder.get();

        // Update saga ID if provided
        if (sagaId != null && order.getSagaId() == null) {
            order.setSagaId(sagaId);
        }

        // Update status with history
        order.updateStatus(newStatus, reason, "SAGA_ORCHESTRATOR");

        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated to {} successfully", orderId, newStatus);

        return updatedOrder;
    }

    /**
     * Cancel order (compensation step)
     */
    @Transactional
    public Order cancelOrder(Long orderId, String reason, String sagaId) {
        log.info("Cancelling order {} for saga: {}", orderId, sagaId);

        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isEmpty()) {
            throw new RuntimeException("Order not found: " + orderId);
        }

        Order order = optionalOrder.get();

        // Cancel the order
        order.cancel(reason, "SAGA_COMPENSATION");

        Order cancelledOrder = orderRepository.save(order);
        log.info("Order {} cancelled successfully", orderId);

        return cancelledOrder;
    }

    /**
     * Handle order update confirmed command
     */
    public void handleUpdateOrderConfirmed(Map<String, Object> command) {
        try {
            String sagaId = (String) command.get("sagaId");
            Long orderId = Long.valueOf(command.get("orderId").toString());
            String reason = (String) command.getOrDefault("reason", "Payment processed successfully");

            log.info("Updating order {} to CONFIRMED for saga: {}", orderId, sagaId);

            // TODO: Inject OrderService and call updateOrderStatus
            // orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED, reason, sagaId);

            // Publish success event
            publishOrderEvent(sagaId, orderId, "ORDER_STATUS_UPDATED_CONFIRMED", true,
                    "Order status updated to CONFIRMED", null);

        } catch (Exception e) {
            log.error("Error updating order to confirmed: {}", e.getMessage(), e);

            // Publish failure event
            String sagaId = (String) command.get("sagaId");
            Long orderId = Long.valueOf(command.get("orderId").toString());
            publishOrderEvent(sagaId, orderId, "ORDER_STATUS_UPDATE_FAILED", false,
                    null, e.getMessage());
        }
    }

    /**
     * Handle order update delivered command
     */
    public void handleUpdateOrderDelivered(Map<String, Object> command) {
        try {
            String sagaId = (String) command.get("sagaId");
            Long orderId = Long.valueOf(command.get("orderId").toString());
            String reason = (String) command.getOrDefault("reason", "Order delivered successfully");

            log.info("Updating order {} to DELIVERED for saga: {}", orderId, sagaId);

            // TODO: Inject OrderService and call updateOrderStatus
            // orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED, reason, sagaId);

            // Publish success event
            publishOrderEvent(sagaId, orderId, "ORDER_STATUS_UPDATED_DELIVERED", true,
                    "Order status updated to DELIVERED", null);

        } catch (Exception e) {
            log.error("Error updating order to delivered: {}", e.getMessage(), e);

            // Publish failure event
            String sagaId = (String) command.get("sagaId");
            Long orderId = Long.valueOf(command.get("orderId").toString());
            publishOrderEvent(sagaId, orderId, "ORDER_STATUS_UPDATE_FAILED", false,
                    null, e.getMessage());
        }
    }

    /**
     * Handle order cancellation command
     */
    public void handleCancelOrder(Map<String, Object> command) {
        try {
            String sagaId = (String) command.get("sagaId");
            Long orderId = Long.valueOf(command.get("orderId").toString());
            String reason = (String) command.getOrDefault("reason", "Order cancelled by saga");

            log.info("Cancelling order {} for saga: {}", orderId, sagaId);

            // TODO: Inject OrderService and call cancelOrder
            // orderService.cancelOrder(orderId, reason, sagaId);

            // Publish success event
            publishOrderEvent(sagaId, orderId, "ORDER_CANCELLED", true,
                    "Order cancelled successfully", null);

        } catch (Exception e) {
            log.error("Error cancelling order: {}", e.getMessage(), e);

            // Publish failure event
            String sagaId = (String) command.get("sagaId");
            Long orderId = Long.valueOf(command.get("orderId").toString());
            publishOrderEvent(sagaId, orderId, "ORDER_CANCELLATION_FAILED", false,
                    null, e.getMessage());
        }
    }

    /**
     * Publish order event back to saga orchestrator
     */
    private void publishOrderEvent(String sagaId, Long orderId, String eventType,
                                   boolean success, String successMessage, String errorMessage) {
        try {
            // TODO: Inject KafkaTemplate when available
            // For now, just log the event that would be published

            log.info("Publishing order event: sagaId={}, orderId={}, eventType={}, success={}",
                    sagaId, orderId, eventType, success);

            if (success) {
                log.info("Order event success: {}", successMessage);
            } else {
                log.error("Order event failure: {}", errorMessage);
            }

            // When KafkaTemplate is injected, this would be:
            /*
            Map<String, Object> event = new HashMap<>();
            event.put("messageId", "ORDER_EVT_" + System.currentTimeMillis());
            event.put("sagaId", sagaId);
            event.put("type", eventType);
            event.put("success", success);
            event.put("orderId", orderId);
            event.put("timestamp", System.currentTimeMillis());

            if (success) {
                event.put("message", successMessage);
            } else {
                event.put("errorMessage", errorMessage);
            }

            kafkaTemplate.send("order.events", sagaId, event);
            */

        } catch (Exception e) {
            log.error("Error publishing order event: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate unique message ID
     */
    private String generateMessageId() {
        return "ORDER_MSG_" + System.currentTimeMillis() + "_" +
                java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}