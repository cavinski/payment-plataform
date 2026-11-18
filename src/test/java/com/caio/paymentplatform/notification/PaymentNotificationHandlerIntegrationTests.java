
package com.caio.paymentplatform.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.caio.paymentplatform.charge.events.ChargePaidEvent;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentNotificationHandlerIntegrationTests {

    @Autowired
    private PaymentNotificationHandler notificationHandler;

    @Autowired
    private PaymentNotificationRepository notificationRepository;

    @Test
    void shouldPersistNotificationWhenChargePaidEventIsHandled() {

        ChargePaidEvent event = createEvent();

        notificationHandler.handle(event);

        var notification = notificationRepository
            .findByEventId(event.eventId())
            .orElseThrow();

        assertEquals(event.eventId(), notification.getEventId());
        assertEquals(event.chargeId(), notification.getChargeId());
        assertEquals("CHARGE_PAID", notification.getNotificationType());
        assertEquals(
            "Payment of 149.90 BRL was successfully confirmed.",
            notification.getMessage()
        );
    }

    @Test
    void shouldRejectDuplicateNotificationForSameEvent() {

        ChargePaidEvent event = createEvent();

        notificationHandler.handle(event);
        notificationRepository.flush();

        assertThrows(DataIntegrityViolationException.class,
            () -> {
                notificationHandler.handle(event);
                notificationRepository.flush();
            }
        );
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
}
