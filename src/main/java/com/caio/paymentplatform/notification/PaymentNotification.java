
package com.caio.paymentplatform.notification;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_notifications")
public class PaymentNotification {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "charge_id", nullable = false)
    private UUID chargeId;

    @Column(name = "notification_type", nullable = false, length = 100)
    private String notificationType;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentNotification() {
    }

    private PaymentNotification(
        UUID eventId,
        UUID chargeId,
        String notificationType,
        String message
    ) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.chargeId = chargeId;
        this.notificationType = notificationType;
        this.message = message;
        this.createdAt = Instant.now();
    }

    public static PaymentNotification create(
        UUID eventId,
        UUID chargeId,
        String notificationType,
        String message
    ) {
        return new PaymentNotification(
            eventId,
            chargeId,
            notificationType,
            message
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getChargeId() {
        return chargeId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
