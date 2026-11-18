
package com.caio.paymentplatform.charge.outbox;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.caio.paymentplatform.charge.events.ChargePaidEvent;
import com.caio.paymentplatform.notification.PaymentNotificationRepository;

@SpringBootTest
@ActiveProfiles("test")
class OutboxProcessorIntegrationTests {

    @Autowired
    private OutboxProcessor outboxProcessor;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private PaymentNotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldProcessEventAndCreateNotification() throws JacksonException {

        ChargePaidEvent event = createEvent();

        OutboxEvent outboxEvent = createOutboxEvent(event);
        outboxRepository.saveAndFlush(outboxEvent);

        outboxProcessor.process(outboxEvent.getId());

        OutboxEvent processedEvent = outboxRepository
            .findById(outboxEvent.getId())
            .orElseThrow();

        assertTrue(processedEvent.isProcessed());

        var notification = notificationRepository.findByEventId(event.eventId());

        assertTrue(notification.isPresent());
        assertEquals(event.chargeId(), notification.get().getChargeId());
    }

    @Test
    void shouldNotProcessEventAgainWhenAlreadyProcessed() throws JacksonException {

        ChargePaidEvent event = createEvent();

        OutboxEvent outboxEvent = createOutboxEvent(event);
        outboxEvent.markAsProcessed();

        outboxRepository.saveAndFlush(outboxEvent);

        outboxProcessor.process(outboxEvent.getId());

        assertTrue(notificationRepository.findByEventId(event.eventId()).isEmpty());
    }

    @Test
    void shouldKeepEventPendingWhenDeserializationFails() {

       OutboxEvent outboxEvent = OutboxEvent.create(
            UUID.randomUUID(),
            "ChargePaidEvent",
            "{ \"eventId\": \"not-a-uuid\" }"
        );

        outboxRepository.saveAndFlush(outboxEvent);

        assertThrows(IllegalStateException.class,
            () -> outboxProcessor.process(outboxEvent.getId())
        );

        OutboxEvent unchangedEvent = outboxRepository
            .findById(outboxEvent.getId())
            .orElseThrow();

        assertFalse(unchangedEvent.isProcessed());
    }

    private ChargePaidEvent createEvent() {
        return new ChargePaidEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("149.90"),
            "BRL",
            Instant.now()
        );
    }

    private OutboxEvent createOutboxEvent(ChargePaidEvent event) throws JacksonException {
        return OutboxEvent.create(
            event.chargeId(),
            event.getClass().getSimpleName(),
            objectMapper.writeValueAsString(event)
        );
    }
}
