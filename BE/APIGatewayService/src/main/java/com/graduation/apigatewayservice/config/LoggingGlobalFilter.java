package com.graduation.apigatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        // Pre-filter: Log incoming request
        logRequest(exchange);

        // Record start time
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // Post-filter: Log response
                    long duration = System.currentTimeMillis() - startTime;
                    logResponse(exchange, duration);
                });
    }

    private void logRequest(ServerWebExchange exchange) {
        var request = exchange.getRequest();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("=== INCOMING REQUEST === [{}]", timestamp);
        log.info("Method: {} | Path: {} | Remote Address: {}",
                request.getMethod(),
                request.getPath().value(),
                request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown"
        );

        // Log headers (excluding sensitive ones)
        log.info("Headers: {}",
                request.getHeaders().entrySet().stream()
                        .filter(entry -> !isSensitiveHeader(entry.getKey()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ))
        );

        // Log query parameters
        if (!request.getQueryParams().isEmpty()) {
            log.info("Query Params: {}", request.getQueryParams());
        }
    }

    private void logResponse(ServerWebExchange exchange, long duration) {
        var response = exchange.getResponse();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("=== OUTGOING RESPONSE === [{}]", timestamp);
        log.info("Status: {} | Duration: {}ms | Path: {}",
                response.getStatusCode(),
                duration,
                exchange.getRequest().getPath().value()
        );

        // Log response headers (excluding sensitive ones)
        log.info("Response Headers: {}",
                response.getHeaders().entrySet().stream()
                        .filter(entry -> !isSensitiveHeader(entry.getKey()))
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                java.util.Map.Entry::getValue
                        ))
        );

        log.info("========================");
    }

    private boolean isSensitiveHeader(String headerName) {
        String lowerCaseName = headerName.toLowerCase();
        return lowerCaseName.contains("authorization") ||
                lowerCaseName.contains("cookie") ||
                lowerCaseName.contains("token") ||
                lowerCaseName.contains("password");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}