package com.graduation.orderservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BaseResponse<T> {
    private Integer status;
    private String msg;
    private T data;
}
