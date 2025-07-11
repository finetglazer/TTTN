package com.graduation.apigatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@Order(-1) // Higher priority than default error handler
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        ErrorResponse errorResponse = createErrorResponse(ex, exchange);

        log.error("Gateway error occurred: {} for path: {}",
                ex.getMessage(),
                exchange.getRequest().getPath().value(),
                ex
        );

        exchange.getResponse().setStatusCode(errorResponse.getStatus());
        exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        String body = buildErrorResponseBody(errorResponse, exchange);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private ErrorResponse createErrorResponse(Throwable ex, ServerWebExchange exchange) {
        if (ex instanceof TimeoutException) {
            return new ErrorResponse(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Gateway Timeout",
                    "The service took too long to respond. Please try again later."
            );
        }

        if (ex instanceof ResponseStatusException rse) {
            return new ErrorResponse(
                    (HttpStatus) rse.getStatusCode(),
                    "Service Error",
                    rse.getReason() != null ? rse.getReason() : "An error occurred while processing your request."
            );
        }

        if (ex instanceof org.springframework.web.reactive.function.client.WebClientException) {
            return new ErrorResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Service Unavailable",
                    "The requested service is currently unavailable. Please try again later."
            );
        }

        // Default case
        return new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later."
        );
    }

    private String buildErrorResponseBody(ErrorResponse errorResponse, ServerWebExchange exchange) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String path = exchange.getRequest().getPath().value();

        return String.format("""
            {
              "timestamp": "%s",
              "status": %d,
              "error": "%s",
              "message": "%s",
              "path": "%s"
            }
            """,
                timestamp,
                errorResponse.getStatus().value(),
                errorResponse.getError(),
                errorResponse.getMessage(),
                path
        );
    }

    private static class ErrorResponse {
        private final HttpStatus status;
        private final String error;
        private final String message;

        public ErrorResponse(HttpStatus status, String error, String message) {
            this.status = status;
            this.error = error;
            this.message = message;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }
    }
}