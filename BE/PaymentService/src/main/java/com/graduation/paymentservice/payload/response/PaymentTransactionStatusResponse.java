package com.graduation.paymentservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentTransactionStatusResponse {
    Long orderId;
    String status;
}
