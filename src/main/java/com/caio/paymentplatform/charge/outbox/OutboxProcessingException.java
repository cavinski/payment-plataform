
package com.caio.paymentplatform.charge.outbox;

import java.util.UUID;

public class OutboxProcessingException extends RuntimeException {

    private final UUID eventId;

    public OutboxProcessingException(UUID eventId, Throwable cause) {
        super("Failed to process outbox event: " + eventId, cause);
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }
}
