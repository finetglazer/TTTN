package com.graduation.apigatewayservice.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    private final RateLimitGatewayFilter rateLimitGatewayFilter;

    public RouteConfig(RateLimitGatewayFilter rateLimitGatewayFilter) {
        this.rateLimitGatewayFilter = rateLimitGatewayFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Order Service Route
                .route("order-service-route", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f
                                .stripPrefix(1) // Remove /api prefix before forwarding
                                .filter(rateLimitGatewayFilter.apply(createRateLimitConfig(50, 60))) // 50 requests per minute
                                .circuitBreaker(config -> config
                                        .setName("order-service-cb")
                                        .setFallbackUri("forward:/fallback/order-service")
                                )
                        )
                        .uri("http://localhost:8081")
                )

                // Payment Service Route
                .route("payment-service-route", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f
                                .stripPrefix(1) // Remove /api prefix before forwarding
                                .filter(rateLimitGatewayFilter.apply(createRateLimitConfig(30, 60))) // 30 requests per minute (more restrictive for payments)
                                .circuitBreaker(config -> config
                                        .setName("payment-service-cb")
                                        .setFallbackUri("forward:/fallback/payment-service")
                                )
                        )
                        .uri("http://localhost:8082")
                )

                // Health check route for gateway itself
                .route("gateway-health", r -> r
                        .path("/health")
                        .uri("http://localhost:8080/actuator/health")
                )

                .build();
    }

    private RateLimitGatewayFilter.Config createRateLimitConfig(int maxRequests, int windowSeconds) {
        RateLimitGatewayFilter.Config config = new RateLimitGatewayFilter.Config();
        config.setMaxRequests(maxRequests);
        config.setWindowSizeInSeconds(windowSeconds);
        return config;
    }
}