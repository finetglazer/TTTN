package com.graduation.sagaorchestratorservice.service;

import com.graduation.sagaorchestratorservice.repository.ProcessedMessageRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for monitoring saga metrics and health
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaMonitoringService {

    private final ProcessedMessageRepository processedMessageRepository;
    private final MeterRegistry meterRegistry;

    // Metrics counters and gauges
    private AtomicInteger activeSagasGauge;
    private AtomicLong totalSagasProcessed;
    private AtomicLong totalMessagesProcessed;
    private AtomicLong totalSagaFailures;

    private Counter sagaStartedCounter;
    private Counter sagaCompletedCounter;
    private Counter sagaFailedCounter;
    private Counter messageProcessedCounter;
    private Counter messageFailedCounter;

    private Timer sagaExecutionTimer;
    private Timer messageProcessingTimer;

    // In-memory tracking for active sagas
    private final Map<String, SagaMetrics> activeSagas = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeMetrics() {
        log.info("Initializing saga monitoring metrics");

        // Gauges for current state
        activeSagasGauge = new AtomicInteger(0);
        meterRegistry.gauge("saga.active.count", activeSagasGauge);

        totalSagasProcessed = new AtomicLong(0);
        meterRegistry.gauge("saga.total.processed", totalSagasProcessed);

        totalMessagesProcessed = new AtomicLong(0);
        meterRegistry.gauge("saga.messages.total.processed", totalMessagesProcessed);

        totalSagaFailures = new AtomicLong(0);
        meterRegistry.gauge("saga.total.failures", totalSagaFailures);

        // Counters for events
        sagaStartedCounter = Counter.builder("saga.started")
                .description("Number of sagas started")
                .register(meterRegistry);

        sagaCompletedCounter = Counter.builder("saga.completed")
                .description("Number of sagas completed successfully")
                .register(meterRegistry);

        sagaFailedCounter = Counter.builder("saga.failed")
                .description("Number of sagas that failed")
                .register(meterRegistry);

        messageProcessedCounter = Counter.builder("saga.message.processed")
                .description("Number of messages processed")
                .register(meterRegistry);

        messageFailedCounter = Counter.builder("saga.message.failed")
                .description("Number of message processing failures")
                .register(meterRegistry);

        // Timers for duration tracking
        sagaExecutionTimer = Timer.builder("saga.execution.time")
                .description("Time taken to complete sagas")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(meterRegistry);

        messageProcessingTimer = Timer.builder("saga.message.processing.time")
                .description("Time taken to process individual messages")
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(meterRegistry);

        log.info("Saga monitoring metrics initialized successfully");
    }

    /**
     * Record that a saga has started
     */
    public void recordSagaStarted(String sagaId, String sagaType) {
        SagaMetrics metrics = new SagaMetrics(sagaId, sagaType, Instant.now());
        activeSagas.put(sagaId, metrics);

        activeSagasGauge.incrementAndGet();
        sagaStartedCounter.increment();

        log.debug("Recorded saga started: {} of type {}", sagaId, sagaType);
    }

    /**
     * Record that a saga has completed successfully
     */
    public void recordSagaCompleted(String sagaId) {
        SagaMetrics metrics = activeSagas.remove(sagaId);
        if (metrics != null) {
            long executionTime = Duration.between(metrics.startTime, Instant.now()).toMillis();
            sagaExecutionTimer.record(executionTime, TimeUnit.MILLISECONDS);

            activeSagasGauge.decrementAndGet();
            sagaCompletedCounter.increment();
            totalSagasProcessed.incrementAndGet();

            log.debug("Recorded saga completed: {} in {}ms", sagaId, executionTime);
        } else {
            log.warn("Attempted to record completion for unknown saga: {}", sagaId);
        }
    }

    /**
     * Record that a saga has failed
     */
    public void recordSagaFailed(String sagaId, String reason) {
        SagaMetrics metrics = activeSagas.remove(sagaId);
        if (metrics != null) {
            long executionTime = Duration.between(metrics.startTime, Instant.now()).toMillis();

            activeSagasGauge.decrementAndGet();
            sagaFailedCounter.increment();
            totalSagaFailures.incrementAndGet();

            log.warn("Recorded saga failed: {} after {}ms, reason: {}", sagaId, executionTime, reason);
        } else {
            log.warn("Attempted to record failure for unknown saga: {}", sagaId);
        }
    }

    /**
     * Record successful message processing
     */
    public void recordMessageProcessed(String sagaId, String messageType, long processingTimeMs) {
        messageProcessedCounter.increment();
        messageProcessingTimer.record(processingTimeMs, TimeUnit.MILLISECONDS);
        totalMessagesProcessed.incrementAndGet();

        // Update saga metrics
        SagaMetrics metrics = activeSagas.get(sagaId);
        if (metrics != null) {
            metrics.incrementMessageCount();
        }

        log.debug("Recorded message processed: type={}, saga={}, time={}ms",
                messageType, sagaId, processingTimeMs);
    }

    /**
     * Record failed message processing
     */
    public void recordMessageFailed(String sagaId, String messageType, String reason) {
        messageFailedCounter.increment();

        // Update saga metrics
        SagaMetrics metrics = activeSagas.get(sagaId);
        if (metrics != null) {
            metrics.incrementFailureCount();
        }

        log.warn("Recorded message failed: type={}, saga={}, reason={}",
                messageType, sagaId, reason);
    }

    /**
     * Get current health status
     */
    public HealthStatus getHealthStatus() {
        long activeCount = activeSagasGauge.get();
        long totalProcessed = totalSagasProcessed.get();
        long totalFailures = totalSagaFailures.get();

        // Calculate failure rate
        double failureRate = totalProcessed > 0 ? (double) totalFailures / totalProcessed : 0.0;

        // Determine health status
        boolean healthy = activeCount < 1000 && failureRate < 0.1; // Less than 10% failure rate

        return new HealthStatus(
                healthy,
                activeCount,
                totalProcessed,
                totalFailures,
                failureRate,
                activeSagas.size()
        );
    }

    /**
     * Update metrics periodically
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void updateMetrics() {
        try {
            log.debug("Updating saga metrics");

            // Clean up old saga metrics (older than 1 hour)
            Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
            activeSagas.entrySet().removeIf(entry ->
                    entry.getValue().startTime.isBefore(oneHourAgo));

            // Update active count from actual map size
            activeSagasGauge.set(activeSagas.size());

            // Log current metrics
            HealthStatus health = getHealthStatus();
            log.debug("Current saga metrics: active={}, processed={}, failures={}, failure_rate={:.2f}%",
                    health.activeCount, health.totalProcessed, health.totalFailures,
                    health.failureRate * 100);

        } catch (Exception e) {
            log.error("Error updating saga metrics", e);
        }
    }

    /**
     * Get detailed metrics for a specific saga
     */
    public SagaMetrics getSagaMetrics(String sagaId) {
        return activeSagas.get(sagaId);
    }

    /**
     * Reset all metrics (for testing purposes)
     */
    public void resetMetrics() {
        activeSagas.clear();
        activeSagasGauge.set(0);
        totalSagasProcessed.set(0);
        totalMessagesProcessed.set(0);
        totalSagaFailures.set(0);

        log.info("Saga metrics reset");
    }

    /**
     * Inner class to track individual saga metrics
     */
    public static class SagaMetrics {
        public final String sagaId;
        public final String sagaType;
        public final Instant startTime;
        public final AtomicInteger messageCount = new AtomicInteger(0);
        public final AtomicInteger failureCount = new AtomicInteger(0);

        public SagaMetrics(String sagaId, String sagaType, Instant startTime) {
            this.sagaId = sagaId;
            this.sagaType = sagaType;
            this.startTime = startTime;
        }

        public void incrementMessageCount() {
            messageCount.incrementAndGet();
        }

        public void incrementFailureCount() {
            failureCount.incrementAndGet();
        }

        public long getExecutionTimeMs() {
            return Duration.between(startTime, Instant.now()).toMillis();
        }
    }

    /**
     * Health status information
     */
    public static class HealthStatus {
        public final boolean healthy;
        public final long activeCount;
        public final long totalProcessed;
        public final long totalFailures;
        public final double failureRate;
        public final int activeSagasTracked;

        public HealthStatus(boolean healthy, long activeCount, long totalProcessed,
                            long totalFailures, double failureRate, int activeSagasTracked) {
            this.healthy = healthy;
            this.activeCount = activeCount;
            this.totalProcessed = totalProcessed;
            this.totalFailures = totalFailures;
            this.failureRate = failureRate;
            this.activeSagasTracked = activeSagasTracked;
        }
    }
}