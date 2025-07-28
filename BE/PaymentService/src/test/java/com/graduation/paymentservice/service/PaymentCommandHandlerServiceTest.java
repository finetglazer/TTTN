package com.graduation.paymentservice.service;

import com.graduation.paymentservice.constant.Constant;
import com.graduation.paymentservice.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit; // Import TimeUnit

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong; // Import anyLong
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PaymentCommandHandlerService.
 */
@ExtendWith(MockitoExtension.class)
class PaymentCommandHandlerServiceTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentCommandHandlerService paymentCommandHandlerService;

    private Map<String, Object> command;
    private String sagaId;
    private String messageId;

    @BeforeEach
    void setUp() {
        sagaId = UUID.randomUUID().toString();
        messageId = "msg-" + UUID.randomUUID().toString();

        command = new HashMap<>();
        command.put(Constant.FIELD_SAGA_ID, sagaId);
        command.put(Constant.FIELD_MESSAGE_ID, messageId);
    }

    @Test
    @DisplayName("handleProcessPayment should skip processing for an already processed message")
    void handleProcessPayment_whenMessageAlreadyProcessed_shouldSkipProcessing() {
        // --- Arrange ---
        when(idempotencyService.isProcessed(messageId, sagaId)).thenReturn(true);

        // --- Act ---
        paymentCommandHandlerService.handleProcessPayment(command);

        // --- Assert ---
        verify(idempotencyService, times(1)).isProcessed(messageId, sagaId);

        // **FIXED LINE**: Use anyLong() for the primitive long argument.
        verify(redisLockService, never()).acquireLock(anyString(), anyLong(), any(TimeUnit.class));

        verify(paymentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        verify(idempotencyService, never()).recordProcessing(anyString(), anyString(), any());
    }
}