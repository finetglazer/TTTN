package com.graduation.apigatewayservice.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class RouteConfig {

    private final RateLimitGatewayFilter rateLimitGatewayFilter;

    public RouteConfig(RateLimitGatewayFilter rateLimitGatewayFilter) {
        this.rateLimitGatewayFilter = rateLimitGatewayFilter;
    }

    /**
     * Apply rate limiting globally to all routes
     * Circuit breaker and other routing configs are handled in application.properties
     */
    @Bean
    @Order(1)
    public GlobalFilter customRateLimitFilter() {
        // Apply different rate limits based on path
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            RateLimitGatewayFilter.Config config;
            if (path.startsWith("/api/payments")) {
                // More restrictive for payments
                config = createRateLimitConfig(30, 60);
            } else {
                // Default rate limit
                config = createRateLimitConfig(50, 60);
            }

            return rateLimitGatewayFilter.apply(config)
                    .filter(exchange, chain);
        };
    }

    private RateLimitGatewayFilter.Config createRateLimitConfig(int maxRequests, int windowSeconds) {
        RateLimitGatewayFilter.Config config = new RateLimitGatewayFilter.Config();
        config.setMaxRequests(maxRequests);
        config.setWindowSizeInSeconds(windowSeconds);
        return config;
    }
}