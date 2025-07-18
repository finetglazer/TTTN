package com.graduation.paymentservice.service;



import com.graduation.paymentservice.model.ProcessedMessage;
import com.graduation.paymentservice.repository.ProcessedMessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final ProcessedMessageRepository processedMessageRepository;

    /**
     * Check if a message has been processed before
     * This handles different scenarios:
     * 1. Message with messageId
     * 2. Message identified by sagaId combination
     */
    public boolean isProcessed(String messageId, String sagaId) {
        // Validate messageId is provided (sagaId is only for context/logging)
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId is required for idempotency check");
        }

        // Check by messageId only (each command should have unique messageId)
        if (processedMessageRepository.findByMessageId(messageId).isPresent()) {
            log.debug("Message already processed: messageId={}", messageId);
            return true;
        }

        log.info("Message not processed: messageId={}, sagaId={}", messageId, sagaId);
        return false;
    }

    public void recordProcessing(String messageId, String sagaId, ProcessedMessage.ProcessStatus status) {
        if (messageId == null || messageId.isEmpty()) {
            throw new IllegalArgumentException("messageId is required");
        }
        if (sagaId == null || sagaId.isEmpty()) {
            throw new IllegalArgumentException("sagaId is required");
        }
        try {
            ProcessedMessage processedMessage = new ProcessedMessage(messageId, sagaId, Instant.now(), status);
            processedMessageRepository.save(processedMessage);
            log.info("Recorded processing for messageId: {}, sagaId: {}, status: {}", messageId, sagaId, status);

        } catch (Exception ex) {
            log.error("Failed to record processing for messageId: {}, sagaId: {}", messageId, sagaId, ex);
            throw new RuntimeException("Failed to record processing", ex);
        }


    }

    /**
     * Clean up old processed messages
     * This can be scheduled to run periodically to remove old entries
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldProcessedMessages() {
        log.info("Cleaning up old processed messages");

        try {
            Instant cutoffTime = Instant.now().minus(30, ChronoUnit.HOURS);

            int deletedCount = processedMessageRepository.deleteByProcessedAtBefore(cutoffTime);

            log.info("Deleted {} old processed messages", deletedCount);
            if (deletedCount > 0) {
                log.info("Old processed messages cleanup completed successfully");
            } else {
                log.info("No old processed messages to clean up");
            }

        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
