package com.graduation.orderservice.payload;

import com.graduation.orderservice.constant.Constant;
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

    @NotBlank(message = Constant.VALIDATION_USER_ID_BLANK)
    private String userId;

    @NotBlank(message = Constant.VALIDATION_USER_EMAIL_BLANK)
    @Email(message = Constant.VALIDATION_INVALID_EMAIL)
    private String userEmail;

    @NotBlank(message = Constant.VALIDATION_USER_NAME_BLANK)
    private String userName;

    @NotBlank(message = Constant.VALIDATION_ORDER_DESCRIPTION_BLANK)
    private String orderDescription;

    @NotNull(message = Constant.VALIDATION_TOTAL_AMOUNT_NULL)
    @DecimalMin(value = "0.0", inclusive = false, message = Constant.VALIDATION_TOTAL_AMOUNT_MIN)
    @Digits(integer = 10, fraction = 2, message = Constant.VALIDATION_INVALID_AMOUNT_FORMAT)
    private BigDecimal totalAmount;

    @Override
    public String toString() {
        return String.format(Constant.FORMAT_CREATE_ORDER_REQUEST_TOSTRING,
                userId, userEmail, userName, totalAmount);
    }
}