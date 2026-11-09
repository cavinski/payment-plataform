
package com.caio.paymentplatform.charge.outbox;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.caio.paymentplatform.charge.events.ChargePaidEvent;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutboxServiceIntegrationTests {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void shouldPersistChargePaidEvent() {
        
        UUID chargeId = UUID.randomUUID();

        ChargePaidEvent event = new ChargePaidEvent(
            UUID.randomUUID(),
            chargeId,
            new BigDecimal("149.90"),
            "BRL",
            java.time.Instant.now()
        );

        outboxService.save(event);

        var savedEvent = outboxRepository.findAll()
            .stream()
            .filter(outbox -> outbox.getAggregateId().equals(chargeId))
            .findFirst()
            .orElseThrow();

        assertEquals(chargeId, savedEvent.getAggregateId());
        assertEquals("ChargePaidEvent", savedEvent.getEventType());
        assertTrue(savedEvent.getPayload().contains(chargeId.toString()));
        assertFalse(savedEvent.isProcessed());
        assertEquals(0, savedEvent.getAttempts());
    }
}
