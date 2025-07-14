package com.graduation.sagaorchestratorservice.scheduler;

import com.graduation.sagaorchestratorservice.service.SagaMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scheduler for saga timeout checking and cleanup operations
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SagaTimeoutScheduler {

    private final SagaMonitoringService sagaMonitoringService;

    // Configuration values from properties
    @Value("${saga.timeout.default-minutes:10}")
    private int defaultTimeoutMinutes;

    @Value("${saga.timeout.payment-minutes:5}")
    private int paymentTimeoutMinutes;

    @Value("${saga.timeout.order-minutes:15}")
    private int orderTimeoutMinutes;

    // Metrics for monitoring scheduler health
    private final AtomicLong timeoutCheckCount = new AtomicLong(0);
    private final AtomicLong timedOutSagasCount = new AtomicLong(0);
    private Instant lastSuccessfulCheck = Instant.now();

    /**
     * Check for timed-out sagas every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void checkForTimeouts() {
        long checkNumber = timeoutCheckCount.incrementAndGet();
        log.debug("Running scheduled timeout check #{}", checkNumber);

        try {
            Instant startTime = Instant.now();

            // For now, we'll implement basic timeout logic
            // In a full implementation, this would check against actual saga state repository
            int timedOutCount = performTimeoutCheck();

            // Update metrics
            lastSuccessfulCheck = Instant.now();
            long duration = Duration.between(startTime, lastSuccessfulCheck).toMillis();

            if (timedOutCount > 0) {
                log.warn("Timeout check #{} completed in {}ms - found {} timed-out sagas",
                        checkNumber, duration, timedOutCount);
                timedOutSagasCount.addAndGet(timedOutCount);
            } else {
                log.debug("Timeout check #{} completed in {}ms - no timed-out sagas found",
                        checkNumber, duration);
            }

        } catch (Exception e) {
            log.error("Error during scheduled timeout check #{}", checkNumber, e);
        }
    }

    /**
     * Perform the actual timeout check logic
     * This is a placeholder implementation that will be expanded when we have saga state management
     */
    private int performTimeoutCheck() {
        int timedOutCount = 0;

        try {
            // Get current health status to check for long-running sagas
            SagaMonitoringService.HealthStatus health = sagaMonitoringService.getHealthStatus();

            if (health.activeCount > 0) {
                log.debug("Checking {} active sagas for timeouts", health.activeCount);

                // This is where we would implement actual timeout logic:
                // 1. Query saga state repository for active sagas
                // 2. Check each saga's start time against timeout thresholds
                // 3. Trigger compensation for timed-out sagas

                // For now, we'll simulate timeout detection
                timedOutCount = checkSimulatedTimeouts();
            }

        } catch (Exception e) {
            log.error("Error performing timeout check", e);
            throw e;
        }

        return timedOutCount;
    }

    /**
     * Simulated timeout check for demonstration
     * This will be replaced with real saga state checking
     */
    private int checkSimulatedTimeouts() {
        // Placeholder implementation
        // In real implementation, this would:
        // 1. Query database for active sagas
        // 2. Calculate elapsed time for each saga
        // 3. Compare against configured timeouts
        // 4. Trigger compensation for timed-out sagas

        return 0; // No timeouts found in simulation
    }

    /**
     * Get timeout duration for a specific saga type
     */
    public Duration getTimeoutForSagaType(String sagaType) {
        if (sagaType == null) {
            return Duration.ofMinutes(defaultTimeoutMinutes);
        }

        return switch (sagaType.toLowerCase()) {
            case "payment", "payment-processing" -> Duration.ofMinutes(paymentTimeoutMinutes);
            case "order", "order-processing" -> Duration.ofMinutes(orderTimeoutMinutes);
            default -> Duration.ofMinutes(defaultTimeoutMinutes);
        };
    }

    /**
     * Check if a saga has timed out based on its start time and type
     */
    public boolean hasSagaTimedOut(String sagaType, Instant sagaStartTime) {
        if (sagaStartTime == null) {
            return false;
        }

        Duration timeout = getTimeoutForSagaType(sagaType);
        Instant timeoutThreshold = Instant.now().minus(timeout);

        return sagaStartTime.isBefore(timeoutThreshold);
    }

    /**
     * Manual timeout check for specific saga
     */
    public boolean checkSagaTimeout(String sagaId, String sagaType, Instant startTime) {
        if (hasSagaTimedOut(sagaType, startTime)) {
            log.warn("Saga {} of type {} has timed out (started at {})",
                    sagaId, sagaType, startTime);

            // Here we would trigger compensation logic
            triggerSagaCompensation(sagaId, "TIMEOUT");

            return true;
        }

        return false;
    }

    /**
     * Trigger compensation for a timed-out saga
     * This is a placeholder that will be implemented with actual saga orchestration
     */
    private void triggerSagaCompensation(String sagaId, String reason) {
        log.warn("Triggering compensation for saga {} due to: {}", sagaId, reason);

        // Record the failure in monitoring
        sagaMonitoringService.recordSagaFailed(sagaId, reason);

        // Here we would:
        // 1. Update saga state to COMPENSATING
        // 2. Send compensation commands to participating services
        // 3. Track compensation progress

        // For now, just log the action
        log.info("Compensation triggered for saga: {}", sagaId);
    }

    /**
     * Cleanup task that runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void performDailyCleanup() {
        log.info("Starting daily saga cleanup task");

        try {
            // Reset metrics counters (keep daily snapshots)
            long totalTimeoutChecks = timeoutCheckCount.get();
            long totalTimedOutSagas = timedOutSagasCount.get();

            log.info("Daily summary: {} timeout checks performed, {} sagas timed out",
                    totalTimeoutChecks, totalTimedOutSagas);

            // Additional cleanup tasks can be added here:
            // - Archive old saga records
            // - Clean up temporary data
            // - Generate reports

        } catch (Exception e) {
            log.error("Error during daily cleanup", e);
        }
    }

    /**
     * Get scheduler health information
     */
    public SchedulerHealth getHealthInfo() {
        Duration timeSinceLastCheck = Duration.between(lastSuccessfulCheck, Instant.now());
        boolean healthy = timeSinceLastCheck.toMinutes() < 2; // Consider unhealthy if no check in 2 minutes

        return new SchedulerHealth(
                healthy,
                timeoutCheckCount.get(),
                timedOutSagasCount.get(),
                lastSuccessfulCheck,
                timeSinceLastCheck
        );
    }

    /**
     * Health information for the scheduler
     */
    public static class SchedulerHealth {
        public final boolean healthy;
        public final long totalChecks;
        public final long totalTimeouts;
        public final Instant lastCheckTime;
        public final Duration timeSinceLastCheck;

        public SchedulerHealth(boolean healthy, long totalChecks, long totalTimeouts,
                               Instant lastCheckTime, Duration timeSinceLastCheck) {
            this.healthy = healthy;
            this.totalChecks = totalChecks;
            this.totalTimeouts = totalTimeouts;
            this.lastCheckTime = lastCheckTime;
            this.timeSinceLastCheck = timeSinceLastCheck;
        }
    }
}