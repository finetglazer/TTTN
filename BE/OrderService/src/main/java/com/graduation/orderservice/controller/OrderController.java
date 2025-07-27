package com.graduation.orderservice.controller;

import com.graduation.orderservice.constant.Constant;
import com.graduation.orderservice.model.Order;
import com.graduation.orderservice.model.OrderStatus;
import com.graduation.orderservice.payload.request.CreateOrderRequest;
import com.graduation.orderservice.payload.response.BaseResponse;
import com.graduation.orderservice.payload.response.OrderStatusResponse;
import com.graduation.orderservice.repository.OrderRepository;
import com.graduation.orderservice.service.OrderCommandHandlerService;
import com.graduation.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

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

    private final OrderCommandHandlerService orderCommandHandlerService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /**
     * Create a new order
     * This will create the order and trigger the saga
     */
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        try {
            log.info(Constant.LOG_CREATING_ORDER, request.getUserId(), request.getTotalAmount());

            Order createdOrder = orderCommandHandlerService.createOrder(
                    request.getUserId(),
                    request.getUserEmail(),
                    request.getUserName(),
                    request.getOrderDescription(),
                    request.getTotalAmount(),
                    request.getShippingAddress()
            );

            return ResponseEntity.ok(new BaseResponse<>(1,Constant.ORDER_CREATED_SUCCESS,createdOrder.getOrderDetails()));

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_CREATING_ORDER, request.getUserId(), e);
            return ResponseEntity.ok(new BaseResponse<>(0, Constant.FAILED_TO_CREATE_ORDER, e.getClass().getSimpleName()));
        }
    }
    /**
     * Get all orders
     */

    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders() {
        try {
            return ResponseEntity.ok(orderService.getAllOrders());

        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                    Constant.RESPONSE_SUCCESS, false,
                    Constant.RESPONSE_MESSAGE, Constant.ERROR_RETRIEVING_ORDER + e.getMessage()
            );
            return ResponseEntity.ok(new BaseResponse<>(0, Constant.ERROR_RETRIEVING_ORDER, errorResponse));
        }
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) {
        try {
            Optional<Order> order = orderRepository.findById(orderId);

            if (order.isPresent()) {
                return ResponseEntity.ok(new BaseResponse<>(1, Constant.RESPONSE_SUCCESS, order.get().getOrderDetails()));
            } else {
                return ResponseEntity.ok(new BaseResponse<>(1, Constant.RESPONSE_SUCCESS, Constant.ORDER_NOT_FOUND));
            }

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_GETTING_ORDER, orderId, e);
            Map<String, Object> errorResponse = Map.of(
                    Constant.RESPONSE_SUCCESS, false,
                    Constant.RESPONSE_MESSAGE, Constant.ERROR_RETRIEVING_ORDER + e.getMessage()
            );
            return ResponseEntity.ok(new BaseResponse<>(0,Constant.ERROR_RETRIEVING_ORDER + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Get order status
     */
    @GetMapping("/{orderId}/status")
    public ResponseEntity<?> getOrderStatus(@PathVariable Long orderId) {
        try {
            Optional<Order> order = orderRepository.findById(orderId);

            if (order.isPresent()) {
                return ResponseEntity.ok(new BaseResponse<>(1, Constant.RESPONSE_SUCCESS, new OrderStatusResponse(orderId ,String.valueOf(order.get().getStatus()))));
            } else {
                return ResponseEntity.ok(new BaseResponse<>(1, Constant.RESPONSE_SUCCESS, Constant.ORDER_NOT_FOUND));
            }

        } catch (Exception e) {
            return ResponseEntity.ok(new BaseResponse<>(0,Constant.ERROR_RETRIEVING_ORDER_STATUS + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Cancel an order
     * This will validate authorization and trigger cancellation saga
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId,
                                         @RequestParam(required = false) String reason) {
        try {
            log.info(Constant.LOG_PROCESSING_CANCEL_ORDER_COMMAND, orderId);

            if (orderId == null || orderId <= 0) {
                return ResponseEntity.ok(new BaseResponse<>(0,
                        "Invalid order ID", "Order ID must be a positive number"));
            }

            // Find the order
            Optional<Order> optionalOrder = orderRepository.findById(orderId);
            if (optionalOrder.isEmpty()) {
                return ResponseEntity.ok(new BaseResponse<>(0,
                        Constant.ORDER_NOT_FOUND,
                        String.format(Constant.ERROR_ORDER_NOT_FOUND_ID, orderId)));
            }

            Order order = optionalOrder.get();

            // Check order status eligibility (CREATED or CONFIRMED only)
            if (!order.getStatus().canBeCancelled()) {
                String statusMessage = order.getStatus() == OrderStatus.DELIVERED
                        ? Constant.ERROR_INVALID_ORDER_STATUS_DELIVERED_FOR_CANCELLING
                        : Constant.ERROR_INVALID_ORDER_STATUS_BE_ALREADY_CANCELLED_FOR_CANCELLING;

                return ResponseEntity.ok(new BaseResponse<>(0,
                        Constant.ERROR_FAILED_TO_CANCEL_ORDER,
                        statusMessage));
            }

            // Initiate cancellation via saga
            String cancelReason = reason != null ? reason : "User cancellation request";
            orderCommandHandlerService.initiateCancellation(order, cancelReason);

            log.info("Cancellation initiated successfully: orderId={}", orderId);

            // Return success response to frontend with "cancellation initiated" message
            Map<String, Object> responseData = Map.of(
                    Constant.FIELD_ORDER_ID, orderId,
                    Constant.FIELD_ORDER_STATUS, "cancellation_initiated"
            );

            return ResponseEntity.ok(new BaseResponse<>(1,
                    Constant.ORDER_CANCELLATION_INITIATED,
                    responseData));

        } catch (Exception e) {
            log.error("Error processing cancellation request for order: {}", orderId, e);
            return ResponseEntity.ok(new BaseResponse<>(0,
                    "Failed to initiate cancellation",
                    "Technical error occurred"));
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
                    Constant.RESPONSE_SUCCESS, true,
                    Constant.RESPONSE_ORDERS, orderDetails,
                    Constant.RESPONSE_TOTAL_COUNT, orders.size()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_GETTING_USER_ORDERS, userId, e);
            Map<String, Object> errorResponse = Map.of(
                    Constant.RESPONSE_SUCCESS, false,
                    Constant.RESPONSE_MESSAGE, Constant.ERROR_RETRIEVING_USER_ORDERS + e.getMessage()
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
                Constant.RESPONSE_SERVICE, Constant.SERVICE_NAME,
                Constant.RESPONSE_STATUS, Constant.STATUS_RUNNING,
                Constant.RESPONSE_MESSAGE, Constant.TEST_ENDPOINT_MESSAGE,
                Constant.RESPONSE_TIMESTAMP, java.time.LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = Map.of(
                Constant.RESPONSE_SERVICE, Constant.SERVICE_NAME,
                Constant.RESPONSE_STATUS, Constant.STATUS_UP,
                Constant.RESPONSE_TIMESTAMP, java.time.LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
}