package com.graduation.orderservice.service;

import com.graduation.orderservice.payload.response.BaseResponse;
import com.graduation.orderservice.payload.response.GetAllOrdersResponse;
import com.graduation.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    // Method get all orders with the form of GetAllOrdersResponse
    public BaseResponse<?> getAllOrders() {
        // get all orders and take only the necessary fields
        return new BaseResponse<>(1, "Get all orders successfully", orderRepository.findAll().stream()
                .map(order -> {
                    GetAllOrdersResponse response = new GetAllOrdersResponse();
                    response.setOrderId(order.getId().toString());
                    response.setOrderDescription(order.getOrderDescription());
                    response.setUserName(order.getUserName());
                    response.setTotalAmount(order.getTotalAmount());
                    response.setCreatedAt(order.getCreatedAt());
                    response.setOrderStatus(order.getStatus());
                    return response;
                })
                .toList());
    }
}
