package com.graduation.orderservice.payload;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating orders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @NotBlank(message = "User email cannot be blank")
    @Email(message = "Invalid email format")
    private String userEmail;

    @NotBlank(message = "User name cannot be blank")
    private String userName;

    @NotBlank(message = "Order description cannot be blank")
    private String orderDescription;

    @NotNull(message = "Total amount cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
    private BigDecimal totalAmount;

    @Override
    public String toString() {
        return String.format("CreateOrderRequest{userId='%s', userEmail='%s', userName='%s', totalAmount=%s}",
                userId, userEmail, userName, totalAmount);
    }
}
