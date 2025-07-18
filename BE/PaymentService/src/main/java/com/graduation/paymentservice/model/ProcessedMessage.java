// ProcessedMessage.java - Model/Entity
package com.graduation.paymentservice.model;

import java.time.Instant;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "processed_messages")
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedMessage {

    @Id
    @Column(name = "message_id")
    private String messageId;

    @Column(name = "saga_id")
    private String sagaId;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProcessStatus status;

    // Status Enum
    public enum ProcessStatus {
        SUCCESS, FAILED, COMPENSATED
    }
}