package com.graduation.orderservice.payload.response;

import com.graduation.orderservice.model.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AllOrdersResponse {
    private String orderId;
    private String orderDescription;
    private String userName;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private OrderStatus orderStatus;
}
