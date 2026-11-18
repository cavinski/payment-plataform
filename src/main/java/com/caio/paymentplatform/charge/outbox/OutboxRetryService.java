
package com.caio.paymentplatform.charge.outbox;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxRetryService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long INITIAL_DELAY_SECONDS = 5;
    private static final long MAX_DELAY_SECONDS = 300;

    private final OutboxRepository outboxRepository;

    public OutboxRetryService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID eventId, Throwable cause) {
        
        OutboxEvent event = outboxRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Outbox event not found: " + eventId
            ));

        int nextAttempt = event.getAttempts() + 1;

        long delaySeconds = Math.min(
            INITIAL_DELAY_SECONDS * (1L << (nextAttempt - 1)),
            MAX_DELAY_SECONDS
        );

        Instant nextAttemptAt = Instant.now().plusSeconds(delaySeconds);

        String error = cause.getMessage();

        if (error == null || error.isBlank()) {
            error = cause.getClass().getSimpleName();
        }

        if (error.length() > 2000) {
            error = error.substring(0, 2000);
        }

        event.registerFailure(error, nextAttemptAt, MAX_ATTEMPTS);

        outboxRepository.save(event);
    }
}
