package com.ecommerce.modules.notification.domain;

import com.ecommerce.modules.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "notification_type", length = 50, nullable = false)
    private String notificationType;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "estado", length = 20)
    private String estado = "PENDING";

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public void markAsSent() {
        this.estado = "SENT";
        this.sentAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.estado = "FAILED";
        this.errorMessage = errorMessage;
    }
}
