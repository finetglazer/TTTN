package com.graduation.apigatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping(value = "/order-service", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        log.warn("Order Service fallback triggered");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createFallbackResponse(
                        "Order Service is currently unavailable",
                        "Please try again in a few moments. If the problem persists, contact support."
                ));
    }

    @RequestMapping(value = "/product-service", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        log.warn("Payment Service fallback triggered");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createFallbackResponse(
                        "Payment Service is currently unavailable",
                        "Payment processing is temporarily unavailable. Please try again later."
                ));
    }

    private Map<String, Object> createFallbackResponse(String service, String message) {
        return Map.of(
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", message,
                "service", service
        );
    }
}