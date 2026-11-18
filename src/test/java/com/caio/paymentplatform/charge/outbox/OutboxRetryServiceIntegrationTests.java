
package com.caio.paymentplatform.charge.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OutboxRetryServiceIntegrationTests {

    @Autowired
    private OutboxRetryService retryService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void shouldRegisterFailureAndScheduleRetry() {

        OutboxEvent event = createEvent();

        outboxRepository.saveAndFlush(event);

        retryService.recordFailure(
            event.getId(),
            new RuntimeException("Notification service unavailable")
        );

        OutboxEvent updated = outboxRepository.findById(event.getId()).orElseThrow();

        assertThat(updated.getAttempts()).isEqualTo(1);
        assertThat(updated.getLastError())
            .isEqualTo("Notification service unavailable");
        assertThat(updated.getNextAttemptAt())
            .isAfter(Instant.now());
        assertThat(updated.getFailedAt()).isNull();
    }

    @Test
    void shouldMarkEventAsFailedAfterMaximumAttempts() {

        OutboxEvent event = createEvent();

        outboxRepository.saveAndFlush(event);

        for (int i = 0; i < 5; i++) {
            retryService.recordFailure(
                event.getId(),
                new RuntimeException("Processing failed")
            );
        }

        OutboxEvent updated = outboxRepository.findById(event.getId()).orElseThrow();

        assertThat(updated.getAttempts()).isEqualTo(5);
        assertThat(updated.getFailedAt()).isNotNull();
        assertThat(updated.isFailed()).isTrue();
    }

    private OutboxEvent createEvent() {
        return OutboxEvent.create(
            UUID.randomUUID(),
            "ChargePaidEvent",
            "{}"
        );
    }
}
