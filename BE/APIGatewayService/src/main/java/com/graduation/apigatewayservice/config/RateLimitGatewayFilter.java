package com.graduation.apigatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitGatewayFilter extends AbstractGatewayFilterFactory<RateLimitGatewayFilter.Config> {

    private final Map<String, ClientRateLimit> clientLimits = new ConcurrentHashMap<>();

    public RateLimitGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientId = getClientId(exchange);

            if (isRateLimited(clientId, config)) {
                log.warn("Rate limit exceeded for client: {}", clientId);

                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

                String body = "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}";
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

                return exchange.getResponse().writeWith(Mono.just(buffer));
            }

            return chain.filter(exchange);
        };
    }

    private String getClientId(org.springframework.web.server.ServerWebExchange exchange) {
        // Use IP address as client identifier
        return exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
                "unknown";
    }

    private boolean isRateLimited(String clientId, Config config) {
        ClientRateLimit limit = clientLimits.computeIfAbsent(clientId, k -> new ClientRateLimit());

        long now = Instant.now().getEpochSecond();
        long windowStart = now - config.getWindowSizeInSeconds();

        // Clean old entries
        limit.getRequestTimes().entrySet().removeIf(entry -> entry.getKey() < windowStart);

        // Count current requests in window
        int currentRequests = limit.getRequestTimes().values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();

        if (currentRequests >= config.getMaxRequests()) {
            return true;
        }

        // Add current request
        limit.getRequestTimes().computeIfAbsent(now, k -> new AtomicInteger(0)).incrementAndGet();

        return false;
    }

    public static class Config {
        private int maxRequests = 100; // requests per window
        private int windowSizeInSeconds = 60; // 1 minute window

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public int getWindowSizeInSeconds() {
            return windowSizeInSeconds;
        }

        public void setWindowSizeInSeconds(int windowSizeInSeconds) {
            this.windowSizeInSeconds = windowSizeInSeconds;
        }
    }

    private static class ClientRateLimit {
        private final Map<Long, AtomicInteger> requestTimes = new ConcurrentHashMap<>();

        public Map<Long, AtomicInteger> getRequestTimes() {
            return requestTimes;
        }
    }
}