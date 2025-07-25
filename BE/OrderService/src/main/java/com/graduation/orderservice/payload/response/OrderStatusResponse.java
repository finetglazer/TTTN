package com.graduation.orderservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class OrderStatusResponse {
    Long orderId;
    String status;
}
