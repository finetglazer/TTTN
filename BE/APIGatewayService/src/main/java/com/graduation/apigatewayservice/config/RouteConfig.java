package com.graduation.apigatewayservice.config;

import com.graduation.apigatewayservice.constants.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Objects;

@Slf4j
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

            RouteRateLimitConfig routeConfig = determineRateLimitConfig(path);

            log.debug("Applying rate limit for path: {} | Config hashcode: {}",
                    path, routeConfig.getSafeHashCode());

            RateLimitGatewayFilter.Config config = createRateLimitConfig(
                    routeConfig.getMaxRequests(),
                    routeConfig.getWindowSeconds()
            );

            return rateLimitGatewayFilter.apply(config)
                    .filter(exchange, chain);
        };
    }

    private RouteRateLimitConfig determineRateLimitConfig(String path) {
        if (path.startsWith("/api/payments")) {
            // More restrictive for payments - sensitive operations
            return new RouteRateLimitConfig("payments", 30, 60);
        } else if (path.startsWith("/api/orders")) {
            // Moderate restrictions for orders
            return new RouteRateLimitConfig("orders", 40, 60);
        } else if (path.startsWith("/api/users")) {
            // Standard restrictions for user operations
            return new RouteRateLimitConfig("users", 50, 60);
        } else if (path.startsWith("/api/products")) {
            // Higher limits for product browsing
            return new RouteRateLimitConfig("products", 100, 60);
        } else {
            // Default rate limit
            return new RouteRateLimitConfig("default", 50, 60);
        }
    }

    private RateLimitGatewayFilter.Config createRateLimitConfig(int maxRequests, int windowSeconds) {
        RateLimitGatewayFilter.Config config = new RateLimitGatewayFilter.Config();
        config.setMaxRequests(maxRequests);
        config.setWindowSizeInSeconds(windowSeconds);
        return config;
    }

    /**
     * Route-specific rate limit configuration with hashcode exclusion
     */
    private static class RouteRateLimitConfig {
        private final String routeType;
        private final int maxRequests;
        private final int windowSeconds;

        public RouteRateLimitConfig(String routeType, int maxRequests, int windowSeconds) {
            this.routeType = routeType;
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }

        public String getRouteType() { return routeType; }
        public int getMaxRequests() { return maxRequests; }
        public int getWindowSeconds() { return windowSeconds; }

        @Override
        public int hashCode() {
            // Include all fields as they define the route configuration behavior
            return Objects.hash(routeType, maxRequests, windowSeconds);
        }

        public int getSafeHashCode() {
            // For logging, include all configuration values as they are not sensitive
            return Objects.hash(routeType, maxRequests, windowSeconds);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RouteRateLimitConfig that = (RouteRateLimitConfig) obj;
            return maxRequests == that.maxRequests &&
                    windowSeconds == that.windowSeconds &&
                    Objects.equals(routeType, that.routeType);
        }

        @Override
        public String toString() {
            return String.format("RouteRateLimitConfig{routeType='%s', maxRequests=%d, windowSeconds=%d}",
                    routeType, maxRequests, windowSeconds);
        }
    }

    /**
     * Circuit breaker configuration with hashcode exclusion
     */
    public static class CircuitBreakerConfig {
        private final String serviceName;
        private final int failureThreshold;
        private final long timeout;
        private final long waitDurationInOpenState;

        public CircuitBreakerConfig(String serviceName, int failureThreshold, long timeout, long waitDurationInOpenState) {
            this.serviceName = serviceName;
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
            this.waitDurationInOpenState = waitDurationInOpenState;
        }

        public String getServiceName() { return serviceName; }
        public int getFailureThreshold() { return failureThreshold; }
        public long getTimeout() { return timeout; }
        public long getWaitDurationInOpenState() { return waitDurationInOpenState; }

        @Override
        public int hashCode() {
            // Include configuration values that define circuit breaker behavior
            return Objects.hash(serviceName, failureThreshold, timeout, waitDurationInOpenState);
        }

        public int getSafeHashCode() {
            // For logging, include all configuration values as they are not sensitive
            return Objects.hash(serviceName, failureThreshold, timeout, waitDurationInOpenState);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CircuitBreakerConfig that = (CircuitBreakerConfig) obj;
            return failureThreshold == that.failureThreshold &&
                    timeout == that.timeout &&
                    waitDurationInOpenState == that.waitDurationInOpenState &&
                    Objects.equals(serviceName, that.serviceName);
        }

        @Override
        public String toString() {
            return String.format("CircuitBreakerConfig{serviceName='%s', failureThreshold=%d, timeout=%d, waitDuration=%d}",
                    serviceName, failureThreshold, timeout, waitDurationInOpenState);
        }
    }
}