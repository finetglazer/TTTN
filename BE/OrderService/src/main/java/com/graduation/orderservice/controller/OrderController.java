package com.graduation.orderservice.controller;

import com.graduation.orderservice.model.Order;
import com.graduation.orderservice.payload.CreateOrderRequest;
import com.graduation.orderservice.repository.OrderRepository;
import com.graduation.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Order Management
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    /**
     * Create a new order
     * This will create the order and trigger the saga
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        try {
            log.info("Creating order for user: {} with amount: {}", request.getUserId(), request.getTotalAmount());

            Order createdOrder = orderService.createOrder(
                    request.getUserId(),
                    request.getUserEmail(),
                    request.getUserName(),
                    request.getOrderDescription(),
                    request.getTotalAmount()
            );

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Order created successfully",
                    "order", createdOrder.getOrderDetails(),
                    "sagaId", createdOrder.getSagaId() != null ? createdOrder.getSagaId() : "pending"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating order for user: {}", request.getUserId(), e);

            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Failed to create order: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable Long orderId) {
        try {
            Optional<Order> order = orderRepository.findById(orderId);

            if (order.isPresent()) {
                Map<String, Object> response = Map.of(
                        "success", true,
                        "order", order.get().getOrderDetails()
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = Map.of(
                        "success", false,
                        "message", "Order not found"
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            log.error("Error getting order: {}", orderId, e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Error retrieving order: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get orders by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserOrders(@PathVariable String userId) {
        try {
            List<Order> orders = orderRepository.findByUserId(userId);

            List<Order.OrderDetails> orderDetails = orders.stream()
                    .map(Order::getOrderDetails)
                    .toList();

            Map<String, Object> response = Map.of(
                    "success", true,
                    "orders", orderDetails,
                    "totalCount", orders.size()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting orders for user: {}", userId, e);
            Map<String, Object> errorResponse = Map.of(
                    "success", false,
                    "message", "Error retrieving user orders: " + e.getMessage()
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
                "service", "OrderService",
                "status", "Running",
                "message", "Order Service Test Endpoint",
                "timestamp", java.time.LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = Map.of(
                "service", "OrderService",
                "status", "UP",
                "timestamp", java.time.LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }


}