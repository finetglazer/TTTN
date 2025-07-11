package com.graduation.orderservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")///api/orders/test
public class test {
    @GetMapping("/test")
    public String test() {
        return "Order Service Test Endpoint";
    }
}
